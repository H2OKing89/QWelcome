package com.example.allowelcome.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allowelcome.data.CustomerData
import com.example.allowelcome.data.MessageTemplate
import com.example.allowelcome.data.SettingsStore
import com.example.allowelcome.data.TechProfile
import com.example.allowelcome.navigation.Navigator
import com.example.allowelcome.ui.CustomerIntakeUiState
import com.example.allowelcome.util.PhoneUtils
import com.example.allowelcome.util.StringUtils
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
        _uiState.update { it.copy(customerPhone = phone, customerPhoneError = null) }
    }

    fun onSsidChanged(ssid: String) {
        _uiState.update { it.copy(ssid = ssid, ssidError = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
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
        if (validateInputs()) {
            val message = generateMessage()
            val normalizedPhone = PhoneUtils.normalize(_uiState.value.customerPhone)
            if (normalizedPhone != null) {
                navigator.openSms(normalizedPhone, message)
            }
        }
    }

    /**
     * Handles Share button click - validates inputs and opens share sheet via Navigator.
     * @param navigator The Navigator instance for launching intents (injected for testability)
     */
    fun onShareClicked(navigator: Navigator) = viewModelScope.launch {
        if (!checkRateLimit()) return@launch
        if (validateInputs()) {
            val message = generateMessage()
            navigator.shareText(message)
        }
    }

    /**
     * Handles Copy button click - validates inputs and copies to clipboard via Navigator.
     * @param navigator The Navigator instance for launching intents (injected for testability)
     */
    fun onCopyClicked(navigator: Navigator) = viewModelScope.launch {
        if (!checkRateLimit()) return@launch
        if (validateInputs()) {
            val message = generateMessage()
            navigator.copyToClipboard("Customer Message", message)
            _uiEvent.emit(UiEvent.ShowToast("Message copied to clipboard"))
        }
    }

    private fun validateInputs(): Boolean {
        var hasError = false
        val currentState = _uiState.value

        // Validate customer name
        if (currentState.customerName.isBlank()) {
            _uiState.update { it.copy(customerNameError = ERROR_NAME_EMPTY) }
            hasError = true
        }
        
        // Validate phone number (US only - see PhoneUtils documentation)
        if (currentState.customerPhone.isBlank()) {
            _uiState.update { it.copy(customerPhoneError = ERROR_PHONE_EMPTY) }
            hasError = true
        } else if (!PhoneUtils.isValid(currentState.customerPhone)) {
            _uiState.update { it.copy(customerPhoneError = ERROR_PHONE_INVALID) }
            hasError = true
        }
        
        // Validate SSID (max 32 characters per WiFi spec)
        if (currentState.ssid.isBlank()) {
            _uiState.update { it.copy(ssidError = ERROR_SSID_EMPTY) }
            hasError = true
        } else if (currentState.ssid.length > 32) {
            _uiState.update { it.copy(ssidError = ERROR_SSID_TOO_LONG) }
            hasError = true
        }
        
        // Validate WiFi password (WPA/WPA2: 8-63 characters, can be numeric-only)
        when {
            currentState.password.isBlank() -> {
                _uiState.update { it.copy(passwordError = ERROR_PASSWORD_EMPTY) }
                hasError = true
            }
            currentState.password.length < 8 -> {
                _uiState.update { it.copy(passwordError = ERROR_PASSWORD_TOO_SHORT) }
                hasError = true
            }
            currentState.password.length > 63 -> {
                _uiState.update { it.copy(passwordError = ERROR_PASSWORD_TOO_LONG) }
                hasError = true
            }
        }
        
        // Validate account number
        if (currentState.accountNumber.isBlank()) {
            _uiState.update { it.copy(accountNumberError = ERROR_ACCOUNT_EMPTY) }
            hasError = true
        }

        return !hasError
    }

    /**
     * Generates the welcome message from template and current UI state.
     */
    private suspend fun generateMessage(): String {
        val uiState = _uiState.value
        val techProfile = settingsStore.techProfileFlow.first()
        val template = settingsStore.activeTemplateFlow.first()
        val customerData = uiState.toCustomerData()
        val baseMessage = MessageTemplate.generate(template, customerData)
        return baseMessage + buildSignature(techProfile)
    }
    
    private fun buildSignature(profile: TechProfile): String {
        val lines = listOf(profile.name, profile.title, profile.dept)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return if (lines.isEmpty()) "" else "\n\n" + lines.joinToString("\n")
    }
}
