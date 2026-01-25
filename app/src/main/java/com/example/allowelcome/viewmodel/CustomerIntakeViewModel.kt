package com.example.allowelcome.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.allowelcome.data.CustomerData
import com.example.allowelcome.data.MessageTemplate
import com.example.allowelcome.data.SettingsStore
import com.example.allowelcome.data.TechProfile
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

    fun onSmsClicked(context: Context) = viewModelScope.launch {
        if (!checkRateLimit()) return@launch
        if (validateInputs()) {
            val message = generateMessage(context)
            sendSms(context, _uiState.value.customerPhone, message)
        }
    }

    fun onShareClicked(context: Context) = viewModelScope.launch {
        if (!checkRateLimit()) return@launch
        if (validateInputs()) {
            val message = generateMessage(context)
            shareMessage(context, message)
        }
    }

    fun onCopyClicked(context: Context) = viewModelScope.launch {
        if (!checkRateLimit()) return@launch
        if (validateInputs()) {
            val message = generateMessage(context)
            copyToClipboard(context, message)
            _uiEvent.emit(UiEvent.ShowToast("Message copied to clipboard"))
        }
    }

    private fun validateInputs(): Boolean {
        var hasError = false
        val currentState = _uiState.value

        if (currentState.customerName.isBlank()) {
            _uiState.update { it.copy(customerNameError = "Customer name cannot be empty") }
            hasError = true
        }
        if (currentState.customerPhone.isBlank()) {
            _uiState.update { it.copy(customerPhoneError = "Customer phone cannot be empty") }
            hasError = true
        } else if (!PhoneUtils.isValid(currentState.customerPhone)) {
            _uiState.update { it.copy(customerPhoneError = "Invalid phone number") }
            hasError = true
        }
        if (currentState.ssid.isBlank()) {
            _uiState.update { it.copy(ssidError = "SSID cannot be empty") }
            hasError = true
        }
        if (currentState.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Password cannot be empty") }
            hasError = true
        }
        if (currentState.accountNumber.isBlank()) {
            _uiState.update { it.copy(accountNumberError = "Account number cannot be empty") }
            hasError = true
        }

        return !hasError
    }

    private suspend fun generateMessage(context: Context): String {
        val uiState = _uiState.value
        val techProfile = settingsStore.techProfileFlow.first()
        val template = settingsStore.activeTemplateFlow.first()
        val customerData = CustomerData(
            customerName = StringUtils.toTitleCase(uiState.customerName),
            customerPhone = uiState.customerPhone,
            ssid = uiState.ssid,
            password = uiState.password,
            accountNumber = uiState.accountNumber
        )
        val baseMessage = MessageTemplate.generate(template, customerData)
        return baseMessage + buildSignature(techProfile)
    }
    
    private fun buildSignature(profile: TechProfile): String {
        val lines = listOf(profile.name, profile.title, profile.dept)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return if (lines.isEmpty()) "" else "\n\n" + lines.joinToString("\n")
    }

    private fun sendSms(context: Context, phone: String, message: String) {
        val normalizedPhone = PhoneUtils.normalize(phone)
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$normalizedPhone")).apply {
            putExtra("sms_body", message)
        }
        context.startActivity(Intent.createChooser(intent, "Send message via..."))
    }

    private fun shareMessage(context: Context, message: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(intent, "Share via..."))
    }

    private fun copyToClipboard(context: Context, message: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Customer Message", message))
    }
}
