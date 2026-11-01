package com.kingpaging.qwelcome.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.kingpaging.qwelcome.util.WifiQrGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class WifiQrArtworkRenderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun printableArtworkScansAtPhoneAndPhotoSizesForSupportedCredentials() {
        requests().forEachIndexed { index, request ->
            val artwork = renderWifiQrBitmap(request)
            try {
                assertEquals(3508, artwork.width)
                assertEquals(4961, artwork.height)
                assertCredentialsFitInsidePage(artwork)
                if (index == 0) {
                    File(context.cacheDir, "wifi-qr-print-preview.png").outputStream().use {
                        assertTrue(artwork.compress(Bitmap.CompressFormat.PNG, 100, it))
                    }
                    writePublicPreviewIfRequested(artwork)
                }
                intArrayOf(800, 1200).forEach { width ->
                    val height = artwork.height * width / artwork.width
                    val photo = Bitmap.createScaledBitmap(artwork, width, height, true)
                    try {
                        assertEquals(
                            "Scaled artwork must retain the complete WiFi payload",
                            expectedPayload(request),
                            decode(photo),
                        )
                    } finally {
                        photo.recycle()
                    }
                }
            } finally {
                artwork.recycle()
            }
        }
    }

    @Test
    fun letterArtworkScansWithLongAndUnicodeCredentials() {
        val requests =
            listOf(
                WifiQrImageRequest("W".repeat(32), "p".repeat(63)),
                WifiQrImageRequest("🏠".repeat(8), "p".repeat(63)),
            )
        requests.forEach { request ->
            val artwork = Bitmap.createBitmap(1200, 1553, Bitmap.Config.ARGB_8888)
            try {
                drawWifiQrArtwork(Canvas(artwork), request, artwork.width, artwork.height, WifiQrArtworkLabels())

                assertEquals(expectedPayload(request), decode(artwork))
                assertCredentialsFitInsidePage(artwork)
                assertPageMarginsRemainWhite(artwork)
            } finally {
                artwork.recycle()
            }
        }
    }

    @Test
    fun requestNormalizesOpenCompatibilityAndDefaultsToMixedPersonal() {
        val default = WifiQrImageRequest("King", "12345678")
        assertEquals(WifiQrGenerator.SecurityType.WPA2_WPA3_PERSONAL, default.effectiveSecurityType)
        assertEquals(false, default.isOpen)
        val openRequests =
            listOf(
                WifiQrImageRequest("Guest", "ignored-password", securityType = WifiQrGenerator.SecurityType.OPEN),
                WifiQrImageRequest(
                    "Guest",
                    "ignored-password",
                    isOpenNetwork = true,
                    securityType = WifiQrGenerator.SecurityType.WPA3_SAE,
                ),
            )
        openRequests.forEach { request ->
            assertTrue(request.isOpen)
            assertEquals(WifiQrGenerator.SecurityType.OPEN, request.effectiveSecurityType)
            assertEquals("WIFI:T:nopass;S:Guest;;", request.payload())
        }
    }

    private fun requests(): List<WifiQrImageRequest> =
        listOf(
            WifiQrImageRequest("King", "12345678"),
            WifiQrImageRequest(
                ssid = "Office;Guest,\\\"Lobby",
                password = "p;ass,w\\\"rd",
                securityType = WifiQrGenerator.SecurityType.WPA2_PSK,
                isHiddenNetwork = true,
            ),
            WifiQrImageRequest(
                ssid = "WPA3 Network",
                password = "password123",
                securityType = WifiQrGenerator.SecurityType.WPA3_SAE,
            ),
            WifiQrImageRequest("Guest WiFi", "ignored-password", securityType = WifiQrGenerator.SecurityType.OPEN),
            WifiQrImageRequest(
                "W".repeat(32),
                "p".repeat(63),
                securityType = WifiQrGenerator.SecurityType.WPA_WPA2_PERSONAL,
            ),
            WifiQrImageRequest("🏠".repeat(8), "password123"),
        )

    private fun expectedPayload(request: WifiQrImageRequest): String =
        if (request.isOpenNetwork) {
            WifiQrGenerator.generateOpenNetworkString(request.ssid, isHidden = request.isHiddenNetwork)
        } else {
            WifiQrGenerator.generateWifiString(
                ssid = request.ssid,
                password = request.password,
                securityType = request.securityType,
                isHidden = request.isHiddenNetwork,
            )
        }

    private fun writePublicPreviewIfRequested(bitmap: Bitmap) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        if (InstrumentationRegistry.getArguments().getString("qrArtworkExportPreview") != "true") return

        val resolver = context.contentResolver
        val values =
            ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "qwelcome-wifi-qr-print-preview.png")
                put(MediaStore.Downloads.MIME_TYPE, "image/png")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        val uri =
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Failed to create synthetic artwork preview")
        var completed = false
        try {
            val output = resolver.openOutputStream(uri) ?: throw IOException("Failed to open preview output")
            output.use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            val published = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            if (resolver.update(uri, published, null, null) == 0) {
                throw IOException("Failed to publish synthetic artwork preview")
            }
            completed = true
        } finally {
            if (!completed) resolver.delete(uri, null, null)
        }
    }

    private fun assertCredentialsFitInsidePage(bitmap: Bitmap) {
        val left = (bitmap.width * 0.12f).toInt()
        val columns = (bitmap.width * 0.76f).toInt()
        val row = IntArray(columns)
        val credentialTop = (bitmap.height - bitmap.width * 0.30f).toInt()
        val credentialBottom = (bitmap.height - bitmap.width * 0.09f).toInt()
        var hasCredentialInk = false
        for (y in credentialTop until credentialBottom) {
            bitmap.getPixels(row, 0, columns, left, y, columns, 1)
            if (row.any { Color.red(it) + Color.green(it) + Color.blue(it) < 576 }) {
                hasCredentialInk = true
                break
            }
        }
        assertTrue("Credentials must remain visibly printed below the code", hasCredentialInk)

        val blankTop = (bitmap.height - bitmap.width * 0.08f).toInt()
        val blankBottom = (bitmap.height - bitmap.width * 0.065f).toInt()
        for (y in blankTop until blankBottom) {
            bitmap.getPixels(row, 0, columns, left, y, columns, 1)
            assertTrue("Credentials must leave a clear bottom margin", row.all { it == Color.WHITE })
        }
    }

    private fun assertPageMarginsRemainWhite(bitmap: Bitmap) {
        val row = IntArray(bitmap.width)
        val horizontalMargin = bitmap.width / 100
        val verticalMargin = bitmap.height / 100
        for (y in 0 until bitmap.height) {
            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
            val isPageEnd = y < verticalMargin || y >= bitmap.height - verticalMargin
            row.forEachIndexed { x, pixel ->
                if (isPageEnd || x < horizontalMargin || x >= bitmap.width - horizontalMargin) {
                    assertEquals("Artwork must fit inside the page margins", Color.WHITE, pixel)
                }
            }
        }
    }

    private fun decode(bitmap: Bitmap): String {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        return MultiFormatReader()
            .decode(
                BinaryBitmap(HybridBinarizer(source)),
                mapOf(DecodeHintType.TRY_HARDER to true),
            ).text
    }
}
