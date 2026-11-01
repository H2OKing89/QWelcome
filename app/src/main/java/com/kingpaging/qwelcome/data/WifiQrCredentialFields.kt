package com.kingpaging.qwelcome.data

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint

private const val FIELD_WIDTH = 0.78f
private const val LABEL_SIZE = 0.0175f
private const val VALUE_SIZE = 0.044f
private const val MIN_VALUE_SIZE = 0.027f
private const val VALUE_SIZE_STEP = 0.001f
private const val LABEL_GAP = 0.004f
private const val FIELD_GAP = 0.018f
private const val MAX_FIELDS_HEIGHT = 0.235f

/** Independent labeled fields preserve every credential character, including wrapped values. */
internal class WifiQrCredentialFields(
    request: WifiQrImageRequest,
    labels: WifiQrArtworkLabels,
    private val width: Float,
    ink: Int,
    mutedInk: Int,
) {
    private val labelGap = width * LABEL_GAP
    private val fieldGap = width * FIELD_GAP
    private val labelWidth = (width * FIELD_WIDTH).toInt().coerceAtLeast(1)
    private val networkLabel =
        wifiQrTextLayout(labels.networkLabel, labelWidth, width * LABEL_SIZE, mutedInk, spacing = 0.08f)
    private val passwordLabel =
        wifiQrTextLayout(labels.passwordLabel, labelWidth, width * LABEL_SIZE, mutedInk, spacing = 0.08f)
    private val values: Pair<StaticLayout, StaticLayout>

    init {
        val password = if (request.isOpen) labels.openPassword else request.password
        var textSize = width * VALUE_SIZE
        val minimumSize = width * MIN_VALUE_SIZE
        var candidate = valueLayouts(request.ssid, password, textSize, ink)
        while (totalHeight(candidate) > width * MAX_FIELDS_HEIGHT && textSize > minimumSize) {
            textSize = (textSize - width * VALUE_SIZE_STEP).coerceAtLeast(minimumSize)
            candidate = valueLayouts(request.ssid, password, textSize, ink)
        }
        require(totalHeight(candidate) <= width * MAX_FIELDS_HEIGHT) {
            "Credentials contain too many lines for the print layout"
        }
        values = candidate
    }

    val height: Float
        get() = totalHeight(values)

    fun draw(
        canvas: Canvas,
        top: Float,
    ) {
        var y = top
        drawWifiQrCenteredText(canvas, networkLabel, width, y)
        y += networkLabel.height + labelGap
        drawWifiQrCenteredText(canvas, values.first, width, y)
        y += values.first.height + fieldGap
        drawWifiQrCenteredText(canvas, passwordLabel, width, y)
        y += passwordLabel.height + labelGap
        drawWifiQrCenteredText(canvas, values.second, width, y)
    }

    private fun totalHeight(valueLayouts: Pair<StaticLayout, StaticLayout>): Float =
        networkLabel.height + passwordLabel.height +
            valueLayouts.first.height + valueLayouts.second.height + labelGap * 2 + fieldGap

    private fun valueLayouts(
        ssid: String,
        password: String,
        textSize: Float,
        ink: Int,
    ): Pair<StaticLayout, StaticLayout> =
        wifiQrTextLayout(ssid, labelWidth, textSize, ink, Typeface.DEFAULT_BOLD) to
            wifiQrTextLayout(password, labelWidth, textSize, ink, Typeface.MONOSPACE)
}

internal fun wifiQrTextLayout(
    text: String,
    width: Int,
    textSize: Float,
    color: Int,
    typeface: Typeface = Typeface.DEFAULT,
    spacing: Float = 0f,
): StaticLayout {
    val paint =
        TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            this.textSize = textSize
            this.color = color
            this.typeface = typeface
            letterSpacing = spacing
        }
    return StaticLayout.Builder
        .obtain(text, 0, text.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setTextDirection(TextDirectionHeuristics.LTR)
        .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
        .setIncludePad(true)
        .build()
}

internal fun drawWifiQrCenteredText(
    canvas: Canvas,
    layout: StaticLayout,
    width: Float,
    top: Float,
) {
    canvas.save()
    try {
        canvas.translate((width - layout.width) / 2f, top)
        layout.draw(canvas)
    } finally {
        canvas.restore()
    }
}
