package com.kingpaging.qwelcome.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.MessageTemplate
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.TechProfile
import com.kingpaging.qwelcome.data.Template
import com.kingpaging.qwelcome.navigation.Navigator
import com.kingpaging.qwelcome.ui.CustomerIntakeUiState
import com.kingpaging.qwelcome.util.PhoneUtils
import com.kingpaging.qwelcome.util.ResourceProvider
import com.kingpaging.qwelcome.util.SystemTimeProvider
import com.kingpaging.qwelcome.util.TimeProvider
import com.kingpaging.qwelcome.util.WifiValidationRules
import com.kingpaging.qwelcome.util.WifiQrGenerator
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** One-shot UI events emitted by the ViewModel */
sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    /** Emitted when message is successfully copied to clipboard - triggers visual feedback */
    data object CopySuccess : UiEvent()
    /** Emitted when user action is blocked by validation or other issue */
    data object ValidationFailed : UiEvent()
    /** Emitted when an action fails after button press */
    data object ActionFailed : UiEvent()
    data object RateLimitExceeded : UiEvent()
}

class CustomerIntakeViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val settingsStore: SettingsStore,
    private val resourceProvider: ResourceProvider,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
    private val enableForegroundInactivityTimer: Boolean = true
) : ViewModel() {

    companion object {
        private const val AUTO_CLEAR_TIMEOUT_MINUTES = 10
        private const val AUTO_CLEAR_TIMEOUT_MS = AUTO_CLEAR_TIMEOUT_MINUTES * 60 * 1000L
        private const val ACTION_COOLDOWN_MS = 2000L // 2 seconds between actions
        private const val KEY_LAST_ACTIVITY_TIMESTAMP = "last_activity_timestamp"

        // Regex for stripping non-digits from phone numbers - reused to avoid allocation
        private val NON_DIGIT_REGEX = Regex("\\D")

        /**
         * Validates a US phone number following NANP rules.
         * @param phone The phone number string (may contain formatting characters)
         * @param progressiveMode If true, returns progressive typing feedback (e.g., "7/10 digits").
         *                        If false, returns short generic error suitable for submit validation.
         * @param resourceProvider Provider to access string resources for error messages.
         * @return Error message string, or null if valid.
         */
        fun validatePhoneNumber(phone: String, progressiveMode: Boolean, resourceProvider: ResourceProvider): String? {
            val digits = phone.replace(NON_DIGIT_REGEX, "")
            val invalidPhoneError = resourceProvider.getString(R.string.error_phone_invalid)
            return when {
                phone.isEmpty() -> null // Don't show error for empty (handled at submit)
                digits.length < 10 -> {
                    if (progressiveMode) resourceProvider.getString(R.string.error_phone_partial, digits.length)
                    else invalidPhoneError
                }
                digits.length == 10 || digits.length == 11 -> {
                    validateNanpRules(digits, progressiveMode, invalidPhoneError, resourceProvider)
                }
                else -> {
                    // digits.length > 11
                    if (progressiveMode) resourceProvider.getString(R.string.error_phone_too_many_digits, digits.length)
                    else invalidPhoneError
                }
            }
        }

        /**
         * Validates NANP-specific rules for 10 or 11 digit phone numbers.
         * Extracted to reduce cognitive complexity of validatePhoneNumber.
         */
        private fun validateNanpRules(
            digits: String,
            progressiveMode: Boolean,
            invalidPhoneError: String,
            resourceProvider: ResourceProvider
        ): String? {
            // Check NANP rules: area code and exchange must start with 2-9
            val areaStart = if (digits.length == 11) 1 else 0
            val areaCode = digits.substring(areaStart, areaStart + 3)
            val exchange = digits.substring(areaStart + 3, areaStart + 6)
            return when {
                digits.length == 11 && digits[0] != '1' -> {
                    if (progressiveMode) resourceProvider.getString(R.string.error_phone_us_start)
                    else invalidPhoneError
                }
                areaCode[0] !in '2'..'9' -> {
                    if (progressiveMode) resourceProvider.getString(R.string.error_phone_area_code, areaCode[0])
                    else invalidPhoneError
                }
                exchange[0] !in '2'..'9' -> {
                    if (progressiveMode) resourceProvider.getString(R.string.error_phone_exchange, exchange[0])
                    else invalidPhoneError
                }
                else -> null // Valid!
            }
        }
    }

    private val _uiState = MutableStateFlow(CustomerIntakeUiState())
    val uiState: StateFlow<CustomerIntakeUiState> = _uiState.asStateFlow()

    // One-shot UI events (Toasts, navigation, etc.)
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    // Track last action time for rate limiting
    private var lastActionTime: Long = 0L
    private var inactivityJob: Job? = null
    private var clearedFormState: CustomerIntakeUiState? = null
    private var clearedFormToken: Long? = null
    private var nextClearedFormToken = 0L

    /**
     * Wraps [MutableStateFlow.update] with an automatic guard:
     * if the resulting state has [CustomerIntakeUiState.showQrSheet] true
     * but [CustomerIntakeUiState.qrEnabled] false, the sheet flag is cleared.
     */
    private inline fun updateState(transform: (CustomerIntakeUiState) -> CustomerIntakeUiState) {
        _uiState.update { current ->
            val next = transform(current)
            if (next.showQrSheet && !next.qrEnabled) next.copy(showQrSheet = false) else next
        }
    }

    fun setShowQrSheet(show: Boolean) {
        updateState { it.copy(showQrSheet = show) }
        if (show) recordUserActivity()
    }

    fun onPause() {
        inactivityJob?.cancel()
        inactivityJob = null
        clearedFormState = null
        clearedFormToken = null
    }

    fun onResume() {
        val lastActivityTimestamp = savedStateHandle.get<Long>(KEY_LAST_ACTIVITY_TIMESTAMP)
        if (lastActivityTimestamp != null) {
            val elapsed = timeProvider.elapsedRealtime() - lastActivityTimestamp
            if (elapsed < 0L || elapsed >= AUTO_CLEAR_TIMEOUT_MS) {
                clearForm()
            } else if (_uiState.value.hasCustomerData && enableForegroundInactivityTimer) {
                scheduleInactivityClear(AUTO_CLEAR_TIMEOUT_MS - elapsed)
            }
        }
    }

    fun clearForm() {
        clearedFormState = null
        clearedFormToken = null
        clearForm(emitToast = true)
    }

    fun clearFormWithUndo(): Long {
        val token = ++nextClearedFormToken
        clearedFormState = _uiState.value.takeIf { it.hasCustomerData }
        clearedFormToken = token.takeIf { clearedFormState != null }
        clearForm(emitToast = false)
        return token
    }

    fun discardClearFormUndo(token: Long) {
        if (clearedFormToken == token) {
            clearedFormState = null
            clearedFormToken = null
        }
    }

    fun undoClearForm(token: Long) {
        if (clearedFormToken != token) return
        val state = clearedFormState ?: return
        clearedFormState = null
        clearedFormToken = null
        _uiState.value = state.copy(showQrSheet = false)
        recordUserActivity()
    }

    private fun clearForm(emitToast: Boolean) {
        inactivityJob?.cancel()
        inactivityJob = null
        savedStateHandle.remove<Long>(KEY_LAST_ACTIVITY_TIMESTAMP)
        _uiState.update { CustomerIntakeUiState() }
        if (emitToast) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.ShowToast(resourceProvider.getString(R.string.toast_form_cleared)))
            }
        }
    }

    fun onCustomerNameChanged(name: String) {
        _uiState.update { it.copy(customerName = name, customerNameError = null) }
        recordUserActivity()
    }

    fun onCustomerPhoneChanged(phone: String) {
        // Real-time validation with progressive feedback
        val error = validatePhoneNumber(phone, progressiveMode = true, resourceProvider)
        _uiState.update { it.copy(customerPhone = phone, customerPhoneError = error) }
        recordUserActivity()
    }

    fun onSsidChanged(ssid: String) {
        // Real-time validation for SSID byte length (WiFi spec: max 32 bytes UTF-8)
        val error = if (ssid.isEmpty()) null else getWifiErrorMessage(WifiQrGenerator.validateSsid(ssid))
        updateState { it.copy(ssid = ssid, ssidError = error) }
        recordUserActivity()
    }

    fun onPasswordChanged(password: String) {
        // Real-time validation feedback for WiFi password (WPA/WPA2: 8-63 chars)
        val error = when {
            password.isEmpty() -> null // Don't show error for empty (show on submit)
            WifiValidationRules.isPasswordTooShort(password) -> resourceProvider.getString(
                R.string.error_password_partial,
                password.length
            )
            else -> getWifiErrorMessage(WifiQrGenerator.validatePassword(password))
        }
        updateState { it.copy(password = password, passwordError = error) }
        recordUserActivity()
    }

    fun onAccountNumberChanged(accountNumber: String) {
        _uiState.update { it.copy(accountNumber = accountNumber, accountNumberError = null) }
        recordUserActivity()
    }

    /**
     * Toggles open network mode. When enabled, password validation is skipped
     * and the password field is cleared/disabled (for guest networks without passwords).
     */
    fun onOpenNetworkChanged(isOpen: Boolean) {
        updateState { state ->
            state.copy(
                isOpenNetwork = isOpen,
                // Clear password and error when switching to open network
                password = if (isOpen) "" else state.password,
                passwordError = null
            )
        }
        recordUserActivity()
    }

    fun onSecurityTypeChanged(securityType: WifiQrGenerator.SecurityType) {
        updateState { it.copy(securityType = securityType) }
        recordUserActivity()
    }

    fun onHiddenNetworkChanged(isHidden: Boolean) {
        updateState { it.copy(isHiddenNetwork = isHidden) }
        recordUserActivity()
    }

    fun recordUserActivity() {
        if (!_uiState.value.hasCustomerData) return
        savedStateHandle[KEY_LAST_ACTIVITY_TIMESTAMP] = timeProvider.elapsedRealtime()
        if (enableForegroundInactivityTimer) {
            scheduleInactivityClear(AUTO_CLEAR_TIMEOUT_MS)
        }
    }

    private fun scheduleInactivityClear(delayMillis: Long) {
        inactivityJob?.cancel()
        inactivityJob = viewModelScope.launch {
            delay(delayMillis)
            checkInactivityTimeout()
        }
    }

    internal fun checkInactivityTimeout() {
        val lastActivityTimestamp = savedStateHandle.get<Long>(KEY_LAST_ACTIVITY_TIMESTAMP) ?: return
        val elapsed = timeProvider.elapsedRealtime() - lastActivityTimestamp
        if (elapsed < 0L || elapsed >= AUTO_CLEAR_TIMEOUT_MS) {
            clearForm()
        } else {
            scheduleInactivityClear(AUTO_CLEAR_TIMEOUT_MS - elapsed)
        }
    }

    /**
     * Check if enough time has passed since last action to prevent accidental spam.
     * Emits RateLimitExceeded event if rate limited.
     */
    private suspend fun checkRateLimit(): Boolean {
        val now = timeProvider.elapsedRealtime()
        if (now - lastActionTime < ACTION_COOLDOWN_MS) {
            _uiEvent.emit(UiEvent.RateLimitExceeded)
            return false
        }
        lastActionTime = now
        return true
    }

    /**
     * Handles SMS button click - validates inputs and sends SMS via Navigator.
     * @param navigator The Navigator instance for launching intents (injected for testability)
     */
    fun onSmsClicked(navigator: Navigator) = viewModelScope.launch {
        recordUserActivity()
        if (!checkRateLimit()) return@launch
        val activeTemplate = settingsStore.activeTemplateFlow.first()
        if (!validateInputs(requirePhone = true, activeTemplate = activeTemplate)) {
            _uiEvent.emit(UiEvent.ValidationFailed)
            return@launch
        }
        val message = generateMessage(activeTemplate)
        val normalizedPhone = PhoneUtils.normalize(_uiState.value.customerPhone)
        if (normalizedPhone != null) {
            navigator.openSms(normalizedPhone, message)
        }
    }

    /**
     * Handles Share button click - validates inputs and opens share sheet via Navigator.
     * Phone number is NOT required for sharing (only SMS needs it).
     * @param navigator The Navigator instance for launching intents (injected for testability)
     */
    fun onShareClicked(navigator: Navigator) = viewModelScope.launch {
        recordUserActivity()
        if (!checkRateLimit()) return@launch
        val activeTemplate = settingsStore.activeTemplateFlow.first()
        if (!validateInputs(requirePhone = false, activeTemplate = activeTemplate)) {
            _uiEvent.emit(UiEvent.ValidationFailed)
            return@launch
        }
        val message = generateMessage(activeTemplate)
        navigator.shareText(message)
    }

    /**
     * Handles Copy button click - validates inputs and copies to clipboard via Navigator.
     * Phone number is NOT required for copying (only SMS needs it).
     * @param navigator The Navigator instance for launching intents (injected for testability)
     */
    fun onCopyClicked(navigator: Navigator) = viewModelScope.launch {
        recordUserActivity()
        if (!checkRateLimit()) return@launch
        val activeTemplate = settingsStore.activeTemplateFlow.first()
        if (!validateInputs(requirePhone = false, activeTemplate = activeTemplate)) {
            _uiEvent.emit(UiEvent.ValidationFailed)
            return@launch
        }
        val message = generateMessage(activeTemplate)
        val success = navigator.copyToClipboard("Customer Message", message)
        if (success) {
            _uiEvent.emit(UiEvent.CopySuccess)
            _uiEvent.emit(UiEvent.ShowToast(resourceProvider.getString(R.string.toast_copied_to_clipboard)))
        } else {
            _uiEvent.emit(UiEvent.ActionFailed)
            _uiEvent.emit(UiEvent.ShowToast(resourceProvider.getString(R.string.toast_copy_failed)))
        }
    }

    /**
     * Validates form inputs before sending/sharing/copying.
     * @param requirePhone If true, validates phone number (for SMS). If false, skips phone validation (for Share/Copy).
     * @param activeTemplate The template snapshot to validate against, resolved once by the caller
     * so validation and message generation stay consistent even if the active template changes mid-action.
     */
    private fun validateInputs(requirePhone: Boolean, activeTemplate: Template): Boolean {
        val currentState = _uiState.value
        val requiresPassword = MessageTemplate.usesPlaceholder(
            activeTemplate.content,
            MessageTemplate.KEY_PASSWORD
        )
        val requiresAccountNumber = MessageTemplate.usesPlaceholder(
            activeTemplate.content,
            MessageTemplate.KEY_ACCOUNT_NUMBER
        )

        // Calculate all errors at once
        val customerNameError = if (currentState.customerName.isBlank()) resourceProvider.getString(R.string.error_name_empty) else null

        val customerPhoneError = if (requirePhone) {
            when {
                currentState.customerPhone.isBlank() -> resourceProvider.getString(R.string.error_phone_empty)
                else -> validatePhoneNumber(currentState.customerPhone, progressiveMode = false, resourceProvider)
            }
        } else null

        val ssidError = when {
            currentState.ssid.isBlank() -> resourceProvider.getString(R.string.error_ssid_empty)
            else -> getWifiErrorMessage(WifiQrGenerator.validateSsid(currentState.ssid))
        }

        // Skip password validation for open networks
        val passwordError = if (!requiresPassword || currentState.isOpenNetwork) {
            null // Open networks don't require passwords
        } else {
            getWifiErrorMessage(WifiQrGenerator.validatePassword(currentState.password))
        }

        val accountNumberError = if (requiresAccountNumber && currentState.accountNumber.isBlank()) {
            resourceProvider.getString(R.string.error_account_empty)
        } else {
            null
        }

        // Batch all error updates into a single state change to minimize recompositions
        updateState { state ->
            state.copy(
                customerNameError = customerNameError,
                customerPhoneError = customerPhoneError,
                ssidError = ssidError,
                passwordError = passwordError,
                accountNumberError = accountNumberError
            )
        }

        // Use the isValid property from UiState (handles open network logic)
        return _uiState.value.isValid
    }

    /**
     * Generates the welcome message from template and current UI state.
     * Uses the {{ tech_signature }} placeholder if present in template.
     * If placeholder is absent, signature is NOT added (user controls placement).
     * @param template The template snapshot resolved by the caller, kept consistent with [validateInputs].
     */
    private suspend fun generateMessage(template: Template): String {
        val uiState = _uiState.value
        val techProfile = settingsStore.techProfileFlow.first()
        val customerData = uiState.toCustomerData()

        // Only include signature if {{ tech_signature }} placeholder is present
        val templateContent = template.content
        return if (templateContent.contains(MessageTemplate.KEY_TECH_SIGNATURE)) {
            // Use placeholder system - signature is embedded where user placed it
            MessageTemplate.generate(templateContent, customerData, techProfile)
        } else {
            // No placeholder = no signature (user opted out)
            MessageTemplate.generate(templateContent, customerData)
        }
    }

    private fun getWifiErrorMessage(result: WifiQrGenerator.ValidationResult): String? {
        return when (result) {
            WifiQrGenerator.ValidationResult.Success -> null
            is WifiQrGenerator.ValidationResult.Error -> resourceProvider.getString(result.messageResId)
        }
    }
}
