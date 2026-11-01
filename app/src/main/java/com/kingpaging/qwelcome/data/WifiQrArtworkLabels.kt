package com.kingpaging.qwelcome.data

import android.content.res.Resources
import com.kingpaging.qwelcome.R

internal data class WifiQrArtworkLabels(
    val title: String = "Connect to Wi-Fi",
    val instruction: String = "Scan with your phone’s camera to connect.",
    val networkLabel: String = "NETWORK NAME",
    val passwordLabel: String = "PASSWORD",
    val openPassword: String = "No password",
)

internal fun wifiQrArtworkLabels(resources: Resources): WifiQrArtworkLabels =
    WifiQrArtworkLabels(
        title = resources.getString(R.string.strings_qr_artwork_title),
        instruction = resources.getString(R.string.strings_qr_artwork_instruction),
        networkLabel = resources.getString(R.string.strings_qr_artwork_network_label),
        passwordLabel = resources.getString(R.string.strings_qr_artwork_password_label),
        openPassword = resources.getString(R.string.strings_qr_artwork_open_password),
    )
