package com.kingpaging.qwelcome.ui.components

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.CancellationSignal
import android.os.Environment
import android.os.OperationCanceledException
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.kingpaging.qwelcome.data.WifiQrImageRequest
import com.kingpaging.qwelcome.util.WifiQrGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class WifiQrPrintDocumentAdapterTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun letterPdfUsesSelectedPaperAndItsRasterizedCodeScans() {
        assertPrintedPdfScans(PrintAttributes.MediaSize.NA_LETTER, "qwelcome-wifi-sign-letter.pdf")
    }

    @Test
    fun a4PdfUsesSelectedPaperAndItsRasterizedCodeScans() {
        assertPrintedPdfScans(PrintAttributes.MediaSize.ISO_A4, "qwelcome-wifi-sign-a4.pdf")
    }

    @Test
    fun preCancelledWriteDoesNotPublishPdf() {
        val adapter = WifiQrPrintDocumentAdapter(context, WifiQrImageRequest("King", "12345678"))
        val file = File.createTempFile("cancelled_wifi_print_", ".pdf", context.cacheDir)
        try {
            val signal = CancellationSignal().apply { cancel() }
            assertThrows(OperationCanceledException::class.java) {
                writeDocument(adapter, attributes(PrintAttributes.MediaSize.NA_LETTER), file, signal)
            }
            assertEquals(0L, file.length())
        } finally {
            finish(adapter)
            file.delete()
        }
    }

    private fun assertPrintedPdfScans(
        mediaSize: PrintAttributes.MediaSize,
        previewName: String,
    ) {
        val request = WifiQrImageRequest("King", "12345678")
        val adapter = WifiQrPrintDocumentAdapter(context, request)
        val file = File.createTempFile("wifi_print_", ".pdf", context.cacheDir)
        try {
            val pages = writeDocument(adapter, attributes(mediaSize), file)
            assertEquals(listOf(PageRange(0, 0)), pages.toList())
            assertTrue(file.length() > 0)

            PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
                assertEquals(1, renderer.pageCount)
                renderer.openPage(0).use { page ->
                    assertEquals(mediaSize.widthMils * 72f / 1000f, page.width.toFloat(), 1f)
                    assertEquals(mediaSize.heightMils * 72f / 1000f, page.height.toFloat(), 1f)
                    val bitmap =
                        Bitmap.createBitmap(
                            1200,
                            page.height * 1200 / page.width,
                            Bitmap.Config.ARGB_8888,
                        )
                    try {
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        assertEquals(
                            WifiQrGenerator.generateWifiString("King", "12345678"),
                            decode(bitmap),
                        )
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
            writePublicPreviewIfRequested(file, previewName)
        } finally {
            finish(adapter)
            file.delete()
        }
    }

    private fun attributes(mediaSize: PrintAttributes.MediaSize): PrintAttributes =
        PrintAttributes
            .Builder()
            .setMediaSize(mediaSize.asPortrait())
            .setResolution(PrintAttributes.Resolution("test", "Test", 300, 300))
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .build()

    private fun writeDocument(
        adapter: WifiQrPrintDocumentAdapter,
        attributes: PrintAttributes,
        file: File,
        cancellationSignal: CancellationSignal = CancellationSignal(),
    ): Array<PageRange> =
        runBlocking(Dispatchers.IO) {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_WRITE_ONLY).use { destination ->
                adapter.writeAndCloseDocument(
                    attributes,
                    arrayOf(PageRange.ALL_PAGES),
                    destination,
                    cancellationSignal,
                )
            }
        }

    private fun finish(adapter: WifiQrPrintDocumentAdapter) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { adapter.onFinish() }
    }

    private fun writePublicPreviewIfRequested(
        file: File,
        displayName: String,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        if (InstrumentationRegistry.getArguments().getString("qrArtworkExportPreview") != "true") return

        val resolver = context.contentResolver
        val values =
            ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        val uri =
            resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Failed to create synthetic print preview")
        var completed = false
        try {
            val output = resolver.openOutputStream(uri) ?: throw IOException("Failed to open preview output")
            output.use { sink -> file.inputStream().use { it.copyTo(sink) } }
            val published = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
            if (resolver.update(uri, published, null, null) == 0) {
                throw IOException("Failed to publish synthetic print preview")
            }
            completed = true
        } finally {
            if (!completed) resolver.delete(uri, null, null)
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
