package com.kingpaging.qwelcome.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.os.CancellationSignal
import android.os.OperationCanceledException
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.util.Log
import com.kingpaging.qwelcome.R
import com.kingpaging.qwelcome.data.WifiQrArtworkLabels
import com.kingpaging.qwelcome.data.WifiQrImageRequest
import com.kingpaging.qwelcome.data.drawWifiQrArtwork
import com.kingpaging.qwelcome.data.wifiQrArtworkLabels
import com.kingpaging.qwelcome.util.sanitizeFileName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream

private const val PRINT_RESOLUTION_DPI = 300
private const val FIRST_PAGE = 0
private const val PAGE_COUNT = 1

internal fun wifiQrPrintAttributes(context: Context): PrintAttributes =
    PrintAttributes
        .Builder()
        .setMediaSize(PrintAttributes.MediaSize.NA_LETTER.asPortrait())
        .setResolution(
            PrintAttributes.Resolution(
                "qwelcome_wifi",
                context.getString(R.string.qr_print_document_title),
                PRINT_RESOLUTION_DPI,
                PRINT_RESOLUTION_DPI,
            ),
        ).setMinMargins(PrintAttributes.Margins.NO_MARGINS)
        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
        .build()

internal fun printWifiQrArtwork(
    context: Context,
    request: WifiQrImageRequest,
    onFinished: () -> Unit,
) {
    val activity = context.printActivity() ?: error("Printing requires an Activity")
    val manager = activity.getSystemService(PrintManager::class.java) ?: error("Printing is unavailable")
    val adapter = WifiQrPrintDocumentAdapter(activity, request, onFinished = onFinished)
    var submitted = false
    try {
        checkNotNull(
            manager.print(
                activity.getString(R.string.qr_print_job_name, request.ssid),
                adapter,
                wifiQrPrintAttributes(activity),
            ),
        ) { "The print dialog could not be opened" }
        submitted = true
    } finally {
        if (!submitted) adapter.onFinish()
    }
}

private tailrec fun Context.printActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> if (baseContext === this) null else baseContext.printActivity()
        else -> null
    }

/** One immutable sign, relaid out at the paper size selected by Android's print UI. */
internal class WifiQrPrintDocumentAdapter(
    context: Context,
    private val request: WifiQrImageRequest,
    private val labels: WifiQrArtworkLabels = wifiQrArtworkLabels(context.resources),
    private val onFinished: () -> Unit = {},
) : PrintDocumentAdapter() {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val errorMessage = context.getString(R.string.qr_print_failed)
    private var attributes: PrintAttributes? = null
    private var finished = false

    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        callback: LayoutResultCallback,
        extras: Bundle?,
    ) {
        if (cancellationSignal.isCanceled) {
            callback.onLayoutCancelled()
        } else if (newAttributes.mediaSize == null) {
            callback.onLayoutFailed(errorMessage)
        } else {
            attributes = newAttributes
            val info =
                PrintDocumentInfo
                    .Builder("WiFi_${sanitizeFileName(request.ssid)}.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PAGE_COUNT)
                    .build()
            callback.onLayoutFinished(info, oldAttributes != newAttributes)
        }
    }

    override fun onWrite(
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        callback: WriteResultCallback,
    ) {
        val selectedAttributes = attributes
        val writeJob =
            scope.launch {
                try {
                    val writtenPages =
                        writeAndCloseDocument(selectedAttributes, pages, destination, cancellationSignal)
                    cancellationSignal.throwIfCanceled()
                    callback.onWriteFinished(writtenPages)
                } catch (_: CancellationException) {
                    withContext(NonCancellable) { callback.onWriteCancelled() }
                } catch (_: OperationCanceledException) {
                    callback.onWriteCancelled()
                } catch (exception: IOException) {
                    reportWriteFailure(callback, exception)
                } catch (exception: IllegalArgumentException) {
                    reportWriteFailure(callback, exception)
                } catch (exception: IllegalStateException) {
                    reportWriteFailure(callback, exception)
                } catch (exception: SecurityException) {
                    reportWriteFailure(callback, exception)
                } finally {
                    cancellationSignal.setOnCancelListener(null)
                }
            }
        cancellationSignal.setOnCancelListener { writeJob.cancel() }
    }

    override fun onFinish() {
        if (finished) return
        finished = true
        scope.cancel()
        onFinished()
    }

    internal suspend fun writeAndCloseDocument(
        selectedAttributes: PrintAttributes?,
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
    ): Array<PageRange> =
        try {
            withContext(Dispatchers.IO) {
                writeDocument(selectedAttributes, pages, destination, cancellationSignal)
            }
        } finally {
            // Also close when cancellation prevents the IO block from starting.
            withContext(NonCancellable + Dispatchers.IO) { destination.close() }
        }

    private suspend fun writeDocument(
        selectedAttributes: PrintAttributes?,
        pages: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
    ): Array<PageRange> {
        val coroutineContext = currentCoroutineContext()
        val checkCancellation = {
            coroutineContext.ensureActive()
            cancellationSignal.throwIfCanceled()
        }
        return ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output ->
            checkCancellation()
            requireNotNull(selectedAttributes) { "Print layout is not ready" }
            if (pages.none { FIRST_PAGE in it.start..it.end }) return@use emptyArray()
            val document = PrintedPdfDocument(appContext, selectedAttributes)
            try {
                drawPage(document, checkCancellation)
                document.writeTo(CancellationCheckingOutputStream(output, checkCancellation))
                checkCancellation()
                arrayOf(PageRange(FIRST_PAGE, FIRST_PAGE))
            } finally {
                document.close()
            }
        }
    }

    private fun drawPage(
        document: PrintedPdfDocument,
        checkCancellation: () -> Unit,
    ) {
        val page = document.startPage(FIRST_PAGE)
        try {
            checkCancellation()
            val content = page.info.contentRect
            // PrintedPdfDocument's canvas already starts at the content-area origin.
            drawWifiQrArtwork(page.canvas, request, content.width(), content.height(), labels)
            checkCancellation()
        } finally {
            document.finishPage(page)
        }
    }

    private fun reportWriteFailure(
        callback: WriteResultCallback,
        exception: Exception,
    ) {
        Log.e("WifiQrPrint", "Could not write WiFi sign PDF", exception)
        callback.onWriteFailed(errorMessage)
    }
}

private class CancellationCheckingOutputStream(
    private val output: OutputStream,
    private val checkCancellation: () -> Unit,
) : OutputStream() {
    override fun write(value: Int) {
        checkCancellation()
        output.write(value)
    }

    override fun write(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ) {
        checkCancellation()
        output.write(buffer, offset, length)
    }
}
