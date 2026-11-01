package com.kingpaging.qwelcome.data

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.kingpaging.qwelcome.util.WifiQrGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.DataInputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class QrCodeImageServiceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun printableShareImageHasA3DimensionsAndDecodesActualCredentials() =
        runBlocking {
            val request = WifiQrImageRequest(ssid = "King", password = "12345678")
            val uri = QrCodeImageService(context).prepareShare(request)
            val signature = ByteArray(8)
            context.contentResolver.openInputStream(uri).use { input ->
                checkNotNull(input)
                DataInputStream(input).readFully(signature)
            }
            assertArrayEquals(
                byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A),
                signature,
            )
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri).use {
                BitmapFactory.decodeStream(it, null, bounds)
            }

            assertEquals(3508, bounds.outWidth)
            assertEquals(4961, bounds.outHeight)
            assertEquals("image/png", bounds.outMimeType)
            assertEquals("WIFI:T:WPA;S:King;P:12345678;;", decodeSharedImage(uri))
        }

    @Test
    fun sharingSameSsidAgainKeepsEarlierRecipientImage() =
        runBlocking {
            val service = QrCodeImageService(context)
            val firstRequest = WifiQrImageRequest("Office", "password123")
            val secondRequest = WifiQrImageRequest("Office", "password456")
            val first = service.prepareShare(firstRequest)
            val second = service.prepareShare(secondRequest)

            assertNotEquals(first, second)
            assertEquals(WifiQrGenerator.generateWifiString("Office", "password123"), decodeSharedImage(first))
            assertEquals(WifiQrGenerator.generateWifiString("Office", "password456"), decodeSharedImage(second))
        }

    @Test
    fun rendererReceivesCompleteRequestAndBitmapIsRecycledOnSuccess() =
        runBlocking {
            val request =
                WifiQrImageRequest(
                    ssid = "Hidden WPA3",
                    password = "password123",
                    securityType = WifiQrGenerator.SecurityType.WPA3_SAE,
                    isHiddenNetwork = true,
                )
            val captured = AtomicReference<WifiQrImageRequest>()
            val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
            try {
                QrCodeImageService(context) {
                    captured.set(it)
                    bitmap
                }.prepareShare(request)

                assertEquals(request, captured.get())
                assertTrue(bitmap.isRecycled)
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }

    @Test
    fun prepareShareWrapsRendererOutOfMemoryAsIOException() {
        val memoryError = OutOfMemoryError("Synthetic QR renderer allocation failure")
        val service = QrCodeImageService(context) { throw memoryError }

        val failure =
            assertThrows(IOException::class.java) {
                runBlocking { service.prepareShare(WifiQrImageRequest("King", "12345678")) }
            }

        assertSame(memoryError, failure.cause)
    }

    @Test
    fun saveToGalleryWrapsRendererOutOfMemoryAsIOException() {
        val memoryError = OutOfMemoryError("Synthetic QR renderer allocation failure")
        val service = QrCodeImageService(context) { throw memoryError }

        val failure =
            assertThrows(IOException::class.java) {
                runBlocking { service.saveToGallery(WifiQrImageRequest("King", "12345678")) }
            }

        assertSame(memoryError, failure.cause)
    }

    @Test
    fun bitmapIsRecycledWhenCacheWriteFails() =
        runBlocking {
            val blockedCache = File.createTempFile("blocked_qr_cache", ".tmp", context.cacheDir)
            val wrapper =
                object : ContextWrapper(context) {
                    override fun getApplicationContext(): Context = this

                    override fun getCacheDir(): File = blockedCache
                }
            val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
            try {
                try {
                    QrCodeImageService(wrapper) { bitmap }.prepareShare(WifiQrImageRequest("Office", "password123"))
                    throw AssertionError("Expected IOException")
                } catch (_: IOException) {
                    assertTrue(bitmap.isRecycled)
                }
            } finally {
                blockedCache.delete()
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }

    @Test
    fun dismissingDuringPreparationStillRecyclesBitmap() =
        runBlocking {
            val enteredRenderer = CountDownLatch(1)
            val releaseRenderer = CountDownLatch(1)
            val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
            val service =
                QrCodeImageService(context) {
                    enteredRenderer.countDown()
                    check(releaseRenderer.await(5, TimeUnit.SECONDS))
                    bitmap
                }
            val job = launch { service.prepareShare(WifiQrImageRequest("Cancellation", "password123")) }
            try {
                withContext(Dispatchers.IO) { assertTrue(enteredRenderer.await(5, TimeUnit.SECONDS)) }
                job.cancel()
            } finally {
                releaseRenderer.countDown()
                job.join()
            }
            assertTrue(bitmap.isRecycled)
        }

    private fun decodeSharedImage(uri: Uri): String {
        val options = BitmapFactory.Options().apply { inSampleSize = 4 }
        val bitmap =
            context.contentResolver.openInputStream(uri).use {
                BitmapFactory.decodeStream(it, null, options)
            }!!
        return try {
            decode(bitmap)
        } finally {
            bitmap.recycle()
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
