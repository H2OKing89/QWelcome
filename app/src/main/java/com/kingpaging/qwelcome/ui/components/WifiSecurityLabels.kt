package com.kingpaging.qwelcome.ui.components

import androidx.annotation.StringRes
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.util.WifiQrGenerator

@StringRes
internal fun WifiQrGenerator.SecurityType.displayLabelResource(): Int =
    when (this) {
        WifiQrGenerator.SecurityType.OPEN -> R.string.security_off
        WifiQrGenerator.SecurityType.WPA2_PSK -> R.string.security_wpa2
        WifiQrGenerator.SecurityType.WPA_WPA2_PERSONAL -> R.string.security_wpa_wpa2_personal
        WifiQrGenerator.SecurityType.WPA3_SAE -> R.string.security_wpa3_sae
        WifiQrGenerator.SecurityType.WPA2_WPA3_PERSONAL -> R.string.security_wpa2_wpa3_personal
    }
