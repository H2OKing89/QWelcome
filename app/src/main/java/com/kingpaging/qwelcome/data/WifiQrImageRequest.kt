package com.kingpaging.qwelcome.data

import com.kingpaging.qwelcome.util.WifiQrGenerator

/** One immutable snapshot supplies both the encoded QR and its printed credentials. */
data class WifiQrImageRequest(
    val ssid: String,
    val password: String,
    val isOpenNetwork: Boolean = false,
    val securityType: WifiQrGenerator.SecurityType = WifiQrGenerator.DEFAULT_SECURITY_TYPE,
    val isHiddenNetwork: Boolean = false,
) {
    override fun toString(): String =
        "WifiQrImageRequest(ssid=$ssid, password=<redacted>, " +
            "isOpenNetwork=$isOpenNetwork, securityType=$securityType, isHiddenNetwork=$isHiddenNetwork)"

    // Normalize the legacy open flag so encoding, artwork and accessibility always agree.
    internal val effectiveSecurityType: WifiQrGenerator.SecurityType
        get() = if (isOpenNetwork) WifiQrGenerator.SecurityType.OPEN else securityType

    internal val isOpen: Boolean
        get() = effectiveSecurityType == WifiQrGenerator.SecurityType.OPEN

    internal fun payload(): String =
        WifiQrGenerator.generateWifiString(ssid, password, effectiveSecurityType, isHiddenNetwork)
}
