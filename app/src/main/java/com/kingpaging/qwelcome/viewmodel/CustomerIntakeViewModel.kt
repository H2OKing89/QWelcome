package com.kingpaging.qwelcome.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kingpaging.qwelcome.data.MessageTemplate
import com.kingpaging.qwelcome.data.SettingsStore
import com.kingpaging.qwelcome.data.TechProfile
import com.kingpaging.qwelcome.navigation.Navigator
import com.kingpaging.qwelcome.ui.CustomerIntakeUiState
import com.kingpaging.qwelcome.util.PhoneUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One-shot UI events emitted by the ViewModel */
sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    /** Emitted when message is successfully copied to clipboard - triggers visual feedback */
    object CopySuccess : UiEvent()
    object RateLimitExceeded : UiEvent()
}

class CustomerIntakeViewModel(private val settingsStore: SettingsStore) : ViewModel() {

    companion object {
        private const val AUTO_CLEAR_TIMEOUT_MINUTES = 10
        private const val AUTO_CLEAR_TIMEOUT_MS = AUTO_CLEAR_TIMEOUT_MINUTES * 60 * 1000L
        private const val ACTION_COOLDOWN_MS = 2000L // 2 seconds between actions
        
        // Validation error messages
        // TODO: Move these to strings.xml for proper localization support
        // For now, keeping as constants for easier refactoring
        const val ERROR_NAME_EMPTY = "Customer name is required"
        const val ERROR_PHONE_EMPTY = "Phone number is required"
        const val ERROR_PHONE_INVALID = "Enter a valid US phone number"
        const val ERROR_SSID_EMPTY = "WiFi network name is required"
        const val ERROR_SSID_TOO_LONG = "SSID cannot exceed 32 characters"
        const val ERROR_PASSWORD_EMPTY = "WiFi password is required"
        const val ERROR_PASSWORD_TOO_SHORT = "Password must be at least 8 characters"
        const val ERROR_PASSWORD_TOO_LONG = "Password cannot exceed 63 characters"
        const val ERROR_ACCOUNT_EMPTY = "Account number is required"
        
        // Regex for stripping non-digits from phone numbers - reused to avoid allocation
        private val NON_DIGIT_REGEX = Regex("\\D")
        
        /**
         * Validates a US phone number following NANP rules.
         * @param phone The phone number string (may contain formatting characters)
         * @param progressiveMode If true, returns progressive typing feedback (e.g., "7/10 digits").
         *                        If false, returns short generic error suitable for submit validation.
         * @return Error message string, or null if valid.
         */
        fun validatePhoneNumber(phone: String, progressiveMode: Boolean): String? {
            val digits = phone.replace(NON_DIGIT_REGEX, "")
            return when {
                phone.isEmpty() -> null // Don't show error for empty (handled at submit)
                digits.length < 10 -> {
                    if (progressiveMode) "Enter 10-digit US number (${digits.length}/10)"
                    else ERROR_PHONE_INVALID
                }
                digits.length == 10 || digits.length == 11 -> {
                    validateNanpRules(digits, progressiveMode)
                }
                digits.length > 11 -> {
                    if (progressiveMode) "Too many digits (${digits.length})" else ERROR_PHONE_INVALID
                }
                else -> null
            }
        }
        
        /**
         * Validates NANP-specific rules for 10 or 11 digit phone numbers.
         * Extracted to reduce cognitive complexity of validatePhoneNumber.
         */
        private fun validateNanpRules(digits: String, progressiveMode: Boolean): String? {
            // Check NANP rules: area code and exchange must start with 2-9
            val areaStart = if (digits.length == 11) 1 else 0
            val areaCode = digits.substring(areaStart, areaStart + 3)
            val exchange = digits.substring(areaStart + 3, areaStart + 6)
            return when {
                digits.length == 11 && digits[0] != '1' -> {
                    if (progressiveMode) "US numbers start with 1" else ERROR_PHONE_INVALID
                }
                areaCode[0] !in '2'..'9' -> {
                    if (progressiveMode) "Area code can't start with ${areaCode[0]}" else ERROR_PHONE_INVALID
                }
                exchange[0] !in '2'..'9' -> {
                    if (progressiveMode) "Exchange can't start with ${exchange[0]}" else ERROR_PHONE_INVALID
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

    // Track when app went to background for auto-clear
    private var backgroundTimestamp: Long = 0L

    // Track last action time for rate limiting
    private var lastActionTime: Long = 0L

    fun onPause() {
        backgroundTimestamp = System.currentTimeMillis()
    }

    fun onResume() {
        if (backgroundTimestamp > 0) {
            val elapsed = System.currentTimeMillis() - backgroundTimestamp
            if (elapsed >= AUTO_CLEAR_TIMEOUT_MS) {
                clearForm()
            }
            backgroundTimestamp = 0L
        }
    }

    fun clearForm() {
        _uiState.update { CustomerIntakeUiState() }
    }

    fun onCustomerNameChanged(name: String) {
        _uiState.update { it.copy(customerName = name, customerNameError = null) }
    }

    fun onCustomerPhoneChanged(phone: String) {
        // Real-time validation with progressive feedback
        val error = validatePhoneNumber(phone, progressiveMode = true)
        _uiState.update { it.copy(customerPhone = phone, customerPhoneError = error) }
    }

    fun onSsidChanged(ssid: String) {
        _uiState.update { it.copy(ssid = ssid, ssidError = null) }
    }

    fun onPasswordChanged(password: String) {
        // Real-time validation feedback for WiFi password (WPA/WPA2: 8-63 chars)
        val error = when {
            password.isEmpty() -> null // Don't show error for empty (show on submit)
            password.length < 8 -> "Password must be at least 8 characters (${password.length}/8)"
            password.length > 63 -> ERROR_PASSWORD_TOO_LONG
            else -> null
        }
        _uiState.update { it.copy(password = password, passwordError = error) }
    }

    fun onAccountNumberChanged(accountNumber: String) {
        _uiState.update { it.copy(accountNumber = accountNumber, accountNumberError = null) }
    }

    /**
     * Check if enough time has passed since last action to prevent accidental spam.
     * Emits RateLimitExceeded event if rate limited.
     */
    private suspend fun checkRateLimit(): Boolean {
        val now = System.currentTimeMillis()
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
        if (!checkRateLimit()) return@launch
        if (validateInputs(requirePhone = true)) {
            val message = generateMessage()
            val normalizedPhone = PhoneUtils.normalize(_uiState.value.customerPhone)
            if (normalizedPhone != null) {
                navigator.openSms(normalizedPhone, message)
            }
        }
    }

    /**
     * Handles Share button click - validates inputs and opens share sheet via Navigator.
     * Phone number is NOT required for sharing (only SMS needs it).
     * @param navigator The Navigator instance for launching intents (injected for testability)
     */
    fun onShareClicked(navigator: Navigator) = viewModelScope.launch {
        if (!checkRateLimit()) return@launch
        if (validateInputs(requirePhone = false)) {
            val message = generateMessage()
            navigator.shareText(message)
        }
    }

    /**
     * Handles Copy button click - validates inputs and copies to clipboard via Navigator.
     * Phone number is NOT required for copying (only SMS needs it).
     * @param navigator The Navigator instance for launching intents (injected for testability)
     */
    fun onCopyClicked(navigator: Navigator) = viewModelScope.launch {
        if (!checkRateLimit()) return@launch
        if (validateInputs(requirePhone = false)) {
            val message = generateMessage()
            navigator.copyToClipboard("Customer Message", message)
            _uiEvent.emit(UiEvent.CopySuccess)
            _uiEvent.emit(UiEvent.ShowToast("Message copied to clipboard"))
        }
    }

    /**
     * Validates form inputs before sending/sharing/copying.
     * @param requirePhone If true, validates phone number (for SMS). If false, skips phone validation (for Share/Copy).
     */
    private fun validateInputs(requirePhone: Boolean): Boolean {
        val currentState = _uiState.value
        
        // Calculate all errors at once
        val customerNameError = if (currentState.customerName.isBlank()) ERROR_NAME_EMPTY else null
        
        val customerPhoneError = if (requirePhone) {
            when {
                currentState.customerPhone.isBlank() -> ERROR_PHONE_EMPTY
                else -> validatePhoneNumber(currentState.customerPhone, progressiveMode = false)
            }
        } else null
        
        val ssidError = when {
            currentState.ssid.isBlank() -> ERROR_SSID_EMPTY
            currentState.ssid.toByteArray(Charsets.UTF_8).size > 32 -> ERROR_SSID_TOO_LONG
            else -> null
        }
        
        val passwordError = when {
            currentState.password.isBlank() -> ERROR_PASSWORD_EMPTY
            currentState.password.length < 8 -> ERROR_PASSWORD_TOO_SHORT
            currentState.password.length > 63 -> ERROR_PASSWORD_TOO_LONG
            else -> null
        }
        
        val accountNumberError = if (currentState.accountNumber.isBlank()) ERROR_ACCOUNT_EMPTY else null
        
        // Batch all error updates into a single state change to minimize recompositions
        _uiState.update { state ->
            state.copy(
                customerNameError = customerNameError,
                customerPhoneError = customerPhoneError,
                ssidError = ssidError,
                passwordError = passwordError,
                accountNumberError = accountNumberError
            )
        }

        return customerNameError == null && 
               customerPhoneError == null && 
               ssidError == null && 
               passwordError == null && 
               accountNumberError == null
    }

    /**
     * Generates the welcome message from template and current UI state.
     * Uses the {{ tech_signature }} placeholder if present in template,
     * otherwise appends signature at the end for backward compatibility.
     */
    private suspend fun generateMessage(): String {
        val uiState = _uiState.value
        val techProfile = settingsStore.techProfileFlow.first()
        val template = settingsStore.activeTemplateFlow.first()
        val customerData = uiState.toCustomerData()

        // Check if template uses {{ tech_signature }} placeholder
        val templateContent = template.content
        return if (templateContent.contains(MessageTemplate.KEY_TECH_SIGNATURE)) {
            // Use placeholder system - signature is embedded in template
            MessageTemplate.generate(templateContent, customerData, techProfile)
        } else {
            // Legacy behavior - append signature at end for templates without placeholder
            val baseMessage = MessageTemplate.generate(templateContent, customerData)
            baseMessage + buildSignature(techProfile)
        }
    }
    
    private fun buildSignature(profile: TechProfile): String {
        val lines = listOf(profile.name, profile.title, profile.dept)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return if (lines.isEmpty()) "" else "\n\n" + lines.joinToString("\n")
    }
}
