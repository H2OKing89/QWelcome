package com.kingpaging.qwelcome.data

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class QrCodeImageServiceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun styledShareImageDecodesToActualWifiCredentials() =
        runBlocking {
            val payload = WifiQrGenerator.generateWifiString("Office;Guest", "password123", isHidden = true)
            val uri = QrCodeImageService(context).prepareShare(payload, "Office;Guest")

            val bitmap = context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }!!
            try {
                assertEquals(payload, decode(bitmap))
            } finally {
                bitmap.recycle()
            }
        }

    @Test
    fun sharingSameSsidAgainKeepsEarlierRecipientImage() =
        runBlocking {
            val service = QrCodeImageService(context)
            val firstPayload = WifiQrGenerator.generateWifiString("Office", "password123")
            val secondPayload = WifiQrGenerator.generateWifiString("Office", "password456")
            val first = service.prepareShare(firstPayload, "Office")
            val second = service.prepareShare(secondPayload, "Office")

            assertNotEquals(first, second)
            val bitmap = context.contentResolver.openInputStream(first).use { BitmapFactory.decodeStream(it) }!!
            try {
                assertEquals(firstPayload, decode(bitmap))
            } finally {
                bitmap.recycle()
            }
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
                    QrCodeImageService(wrapper) { bitmap }.prepareShare("payload", "Office")
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
            val job = launch { service.prepareShare("payload", "Cancellation") }
            try {
                withContext(Dispatchers.IO) { assertTrue(enteredRenderer.await(5, TimeUnit.SECONDS)) }
                job.cancel()
            } finally {
                releaseRenderer.countDown()
                job.join()
            }
            assertTrue(bitmap.isRecycled)
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
