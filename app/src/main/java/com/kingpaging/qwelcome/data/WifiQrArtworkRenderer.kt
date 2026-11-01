package com.kingpaging.qwelcome.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.createBitmap
import io.github.alexzhirkevich.qrose.QrCodePainter
import io.github.alexzhirkevich.qrose.options.QrBackground
import io.github.alexzhirkevich.qrose.options.QrBrush
import io.github.alexzhirkevich.qrose.options.QrColors
import io.github.alexzhirkevich.qrose.options.QrOptions
import io.github.alexzhirkevich.qrose.options.solid
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Canvas as ComposeCanvas
import androidx.compose.ui.graphics.Color as ComposeColor

internal const val WIFI_QR_PRINT_WIDTH = 3508
internal const val WIFI_QR_PRINT_HEIGHT = 4961

private const val MIN_ARTWORK_WIDTH = 480
private const val LETTER_HEIGHT_RATIO = 792f / 612f
private const val QUIET_ZONE_MODULES = 4

// QRose 1.1.2 publishes an intrinsic size of ten pixels per encoded module.
private const val QROSE_INTRINSIC_MODULE_SIZE = 10f
private const val FRAME_MARGIN = 0.06f
private const val FRAME_CORNER = 0.028f
private const val FRAME_STROKE = 0.001f
private const val ICON_DOT_Y = 0.113f
private const val ICON_DOT_RADIUS = 0.004f
private const val ICON_STROKE = 0.0036f
private const val ICON_INNER_RADIUS = 0.023f
private const val ICON_OUTER_RADIUS = 0.045f
private const val ICON_START_ANGLE = 225f
private const val ICON_SWEEP_ANGLE = 90f
private const val HEADING_TOP = 0.14f
private const val HEADING_SIZE = 0.063f
private const val TEXT_WIDTH = 0.84f
private const val INSTRUCTION_GAP = 0.007f
private const val INSTRUCTION_SIZE = 0.022f
private const val QR_TOP_GAP = 0.026f
private const val QR_MAX_SIDE = 0.82f
private const val CREDENTIAL_BOTTOM_MARGIN = 0.09f
private const val CREDENTIAL_QR_GAP = 0.034f
private val Ink = Color.rgb(23, 32, 51)
private val MutedInk = Color.rgb(80, 87, 104)
private val Accent = Color.rgb(106, 76, 195)
private val Rule = Color.rgb(218, 212, 232)

/** Render a print-sized sign; the phone preview uses this exact layout at a smaller width. */
internal fun renderWifiQrBitmap(
    request: WifiQrImageRequest,
    width: Int = WIFI_QR_PRINT_WIDTH,
    labels: WifiQrArtworkLabels = WifiQrArtworkLabels(),
): Bitmap {
    require(width in MIN_ARTWORK_WIDTH..WIFI_QR_PRINT_WIDTH)
    val height = (width.toLong() * WIFI_QR_PRINT_HEIGHT / WIFI_QR_PRINT_WIDTH).toInt()
    val bitmap = createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.density = Bitmap.DENSITY_NONE
    var completed = false
    try {
        drawWifiQrArtwork(Canvas(bitmap), request, width, height, labels)
        completed = true
        return bitmap
    } finally {
        if (!completed) bitmap.recycle()
    }
}

/** Native paths and text also draw directly into PDF pages, keeping printed edges sharp. */
internal fun drawWifiQrArtwork(
    canvas: Canvas,
    request: WifiQrImageRequest,
    width: Int,
    height: Int,
    labels: WifiQrArtworkLabels,
) {
    require(width > 0 && height > 0)
    val pageHeight = min(height.toFloat(), width.toFloat() * WIFI_QR_PRINT_HEIGHT / WIFI_QR_PRINT_WIDTH)
    val pageWidth = min(width.toFloat(), pageHeight / LETTER_HEIGHT_RATIO)
    val fields = WifiQrCredentialFields(request, labels, pageWidth, Ink, MutedInk)
    val fieldsTop = pageHeight - pageWidth * CREDENTIAL_BOTTOM_MARGIN - fields.height
    val painter = QrCodePainter(request.payload(), wifiQrOptions())
    canvas.drawColor(Color.WHITE)
    canvas.save()
    try {
        // Landscape and unusually tall media keep a centered portrait sign.
        canvas.translate((width - pageWidth) / 2f, (height - pageHeight) / 2f)
        drawFrame(canvas, pageWidth, pageHeight)
        drawWifiIcon(canvas, pageWidth)
        val headingBottom = drawHeading(canvas, pageWidth, labels)
        val qrTop = headingBottom + pageWidth * QR_TOP_GAP
        val qrSide = min(pageWidth * QR_MAX_SIDE, fieldsTop - pageWidth * CREDENTIAL_QR_GAP - qrTop)
        require(qrSide > 0) { "The page is too small for the WiFi sign" }
        drawQr(canvas, pageWidth, qrTop, qrSide, painter)
        fields.draw(canvas, fieldsTop)
    } finally {
        canvas.restore()
    }
}

/** Solid opaque modules and a separate four-module quiet zone remain scan-safe in monochrome. */
internal fun wifiQrOptions(): QrOptions =
    QrOptions(
        colors =
            QrColors(
                dark = QrBrush.Default,
                light = QrBrush.solid(ComposeColor.White),
                ball = QrBrush.Default,
                frame = QrBrush.Default,
            ),
        background = QrBackground(fill = SolidColor(ComposeColor.White)),
        scale = 1f,
    )

private fun drawFrame(
    canvas: Canvas,
    width: Float,
    height: Float,
) {
    val margin = width * FRAME_MARGIN
    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Rule
            style = Paint.Style.STROKE
            strokeWidth = width * FRAME_STROKE
        }
    canvas.drawRoundRect(
        RectF(margin, margin, width - margin, height - margin),
        width * FRAME_CORNER,
        width * FRAME_CORNER,
        paint,
    )
}

private fun drawWifiIcon(
    canvas: Canvas,
    width: Float,
) {
    val center = width / 2f
    val dotY = width * ICON_DOT_Y
    val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Accent
            style = Paint.Style.STROKE
            strokeWidth = width * ICON_STROKE
            strokeCap = Paint.Cap.ROUND
        }
    floatArrayOf(ICON_INNER_RADIUS, ICON_OUTER_RADIUS).forEach { fraction ->
        val radius = width * fraction
        canvas.drawArc(
            RectF(center - radius, dotY - radius, center + radius, dotY + radius),
            ICON_START_ANGLE,
            ICON_SWEEP_ANGLE,
            false,
            paint,
        )
    }
    paint.style = Paint.Style.FILL
    canvas.drawCircle(center, dotY, width * ICON_DOT_RADIUS, paint)
}

private fun drawHeading(
    canvas: Canvas,
    width: Float,
    labels: WifiQrArtworkLabels,
): Float {
    val textWidth = (width * TEXT_WIDTH).toInt().coerceAtLeast(1)
    val heading = wifiQrTextLayout(labels.title, textWidth, width * HEADING_SIZE, Ink, Typeface.DEFAULT_BOLD)
    val top = width * HEADING_TOP
    drawWifiQrCenteredText(canvas, heading, width, top)
    val instructionTop = top + heading.height + width * INSTRUCTION_GAP
    val instruction = wifiQrTextLayout(labels.instruction, textWidth, width * INSTRUCTION_SIZE, MutedInk)
    drawWifiQrCenteredText(canvas, instruction, width, instructionTop)
    return instructionTop + instruction.height
}

private fun drawQr(
    canvas: Canvas,
    width: Float,
    top: Float,
    maxSide: Float,
    painter: QrCodePainter,
) {
    val modules = (painter.intrinsicSize.width / QROSE_INTRINSIC_MODULE_SIZE).roundToInt()
    val modulePixels = (maxSide / (modules + QUIET_ZONE_MODULES * 2)).toInt().coerceAtLeast(1)
    val quietPixels = modulePixels * QUIET_ZONE_MODULES
    val side = modules * modulePixels
    val left = ((width - side) / 2f).roundToInt()
    val moduleTop = top.roundToInt() + quietPixels
    canvas.save()
    try {
        canvas.translate(left.toFloat(), moduleTop.toFloat())
        with(painter) {
            CanvasDrawScope().draw(
                density = Density(1f, 1f),
                layoutDirection = LayoutDirection.Ltr,
                canvas = ComposeCanvas(canvas),
                size = Size(side.toFloat(), side.toFloat()),
            ) {
                // Public QRose vector path avoids another full-sized QR bitmap.
                onCache()
            }
        }
    } finally {
        canvas.restore()
    }
}
