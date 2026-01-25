package com.example.allowelcome.ui

data class CustomerIntakeUiState(
    val customerName: String = "",
    val customerNameError: String? = null,
    val customerPhone: String = "",
    val customerPhoneError: String? = null,
    val ssid: String = "",
    val ssidError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val accountNumber: String = "",
    val accountNumberError: String? = null,
)