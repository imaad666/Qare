package com.example.qare

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

enum class PixelStyle { Square, Rounded, Dot, Continuous }
enum class EyeStyle { Square, Rounded, ThreeBars }

object QRCodeUtils {
    fun createStyledQr(
        content: String,
        size: Int,
        pixelColor: Int = Color.BLACK,
        eyeColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE,
        pixelStyle: PixelStyle = PixelStyle.Rounded,
        eyeStyle: EyeStyle = EyeStyle.Rounded
    ): Bitmap {
        val hints = hashMapOf<EncodeHintType, Any>(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H
        )
        val qrCode = Encoder.encode(content, ErrorCorrectionLevel.H, hints)
        val matrix = qrCode.matrix
        val modules = matrix.width
        val moduleSize = size.toFloat() / modules.toFloat()
        val offsetX = ((size - modules * moduleSize) / 2f)
        val offsetY = ((size - modules * moduleSize) / 2f)

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(backgroundColor)

        val paintPixel = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pixelColor }
        val paintEye = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = eyeColor }

        // Draw modules according to style (skip finder regions)
        when (pixelStyle) {
            PixelStyle.Square -> drawSquares(matrix, canvas, paintPixel, moduleSize, offsetX, offsetY)
            PixelStyle.Rounded -> drawRounded(matrix, canvas, paintPixel, moduleSize, offsetX, offsetY)
            PixelStyle.Dot -> drawDots(matrix, canvas, paintPixel, moduleSize, offsetX, offsetY)
            PixelStyle.Continuous -> drawContinuous(matrix, canvas, paintPixel, moduleSize, offsetX, offsetY)
        }

        // Draw eyes over modules so eye color always applies
        drawEyes(canvas, modules, moduleSize, offsetX, offsetY, paintEye, backgroundColor, eyeStyle)

        return bitmap
    }

    private fun drawSquares(
        matrix: com.google.zxing.qrcode.encoder.ByteMatrix,
        canvas: Canvas,
        paint: Paint,
        moduleSize: Float,
        offsetX: Float,
        offsetY: Float
    ) {
        val modules = matrix.width
        for (y in 0 until modules) {
            for (x in 0 until modules) {
                if (matrix.get(x, y).toInt() != 0 && !isEyeModule(x, y, modules)) {
                    val l = offsetX + x * moduleSize
                    val t = offsetY + y * moduleSize
                    canvas.drawRect(l, t, l + moduleSize, t + moduleSize, paint)
                }
            }
        }
    }

    private fun drawRounded(
        matrix: com.google.zxing.qrcode.encoder.ByteMatrix,
        canvas: Canvas,
        paint: Paint,
        moduleSize: Float,
        offsetX: Float,
        offsetY: Float
    ) {
        val radius = moduleSize * 0.45f
        val modules = matrix.width
        for (y in 0 until modules) {
            for (x in 0 until modules) {
                if (matrix.get(x, y).toInt() != 0 && !isEyeModule(x, y, modules)) {
                    val l = offsetX + x * moduleSize
                    val t = offsetY + y * moduleSize
                    canvas.drawRoundRect(l, t, l + moduleSize, t + moduleSize, radius, radius, paint)
                }
            }
        }
    }

    private fun drawDots(
        matrix: com.google.zxing.qrcode.encoder.ByteMatrix,
        canvas: Canvas,
        paint: Paint,
        moduleSize: Float,
        offsetX: Float,
        offsetY: Float
    ) {
        val radius = moduleSize * 0.45f
        val modules = matrix.width
        for (y in 0 until modules) {
            for (x in 0 until modules) {
                if (matrix.get(x, y).toInt() != 0 && !isEyeModule(x, y, modules)) {
                    val cx = offsetX + (x + 0.5f) * moduleSize
                    val cy = offsetY + (y + 0.5f) * moduleSize
                    canvas.drawCircle(cx, cy, radius, paint)
                }
            }
        }
    }

    private fun drawContinuous(
        matrix: com.google.zxing.qrcode.encoder.ByteMatrix,
        canvas: Canvas,
        paint: Paint,
        moduleSize: Float,
        offsetX: Float,
        offsetY: Float
    ) {
        val modules = matrix.width
        for (y in 0 until modules) {
            var x = 0
            while (x < modules) {
                if (matrix.get(x, y).toInt() != 0 && !isEyeModule(x, y, modules)) {
                    val startX = x
                    while (x + 1 < modules && matrix.get(x + 1, y).toInt() != 0 && !isEyeModule(x + 1, y, modules)) {
                        x++
                    }
                    val l = offsetX + startX * moduleSize
                    val t = offsetY + y * moduleSize + moduleSize * 0.2f
                    val r = offsetX + (x + 1) * moduleSize
                    val b = offsetY + (y + 1) * moduleSize - moduleSize * 0.2f
                    val radius = moduleSize * 0.4f
                    canvas.drawRoundRect(l, t, r, b, radius, radius, paint)
                }
                x++
            }
        }
    }

    private fun drawEyes(
        canvas: Canvas,
        modules: Int,
        moduleSize: Float,
        offsetX: Float,
        offsetY: Float,
        eyePaint: Paint,
        backgroundColor: Int,
        eyeStyle: EyeStyle
    ) {
        val eye = 7
        val positions = listOf(
            0 to 0, // top-left
            modules - eye to 0, // top-right
            0 to modules - eye // bottom-left
        )
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backgroundColor }
        val radius = if (eyeStyle == EyeStyle.Rounded) moduleSize else 0f
        for ((ex, ey) in positions) {
            val l = offsetX + ex * moduleSize
            val t = offsetY + ey * moduleSize
            val r = l + eye * moduleSize
            val b = t + eye * moduleSize
            when (eyeStyle) {
                EyeStyle.Square, EyeStyle.Rounded -> {
                    // Outer 7x7
                    canvas.drawRoundRect(l, t, r, b, radius, radius, eyePaint)
                    // Inner 5x5 background
                    val l5 = l + moduleSize
                    val t5 = t + moduleSize
                    val r5 = r - moduleSize
                    val b5 = b - moduleSize
                    canvas.drawRoundRect(l5, t5, r5, b5, radius, radius, bgPaint)
                    // Center 3x3
                    val l3 = l + 2 * moduleSize
                    val t3 = t + 2 * moduleSize
                    val r3 = r - 2 * moduleSize
                    val b3 = b - 2 * moduleSize
                    canvas.drawRoundRect(l3, t3, r3, b3, radius, radius, eyePaint)
                }
                EyeStyle.ThreeBars -> {
                    for (i in 0..2) {
                        val barL = l + i * (eye * moduleSize) / 3f
                        val barR = l + (i + 1) * (eye * moduleSize) / 3f - moduleSize * 0.15f
                        canvas.drawRect(barL, t, barR, b, eyePaint)
                    }
                }
            }
        }
    }

    private fun isEyeModule(x: Int, y: Int, modules: Int): Boolean {
        val eye = 7
        val inTopLeft = x in 0 until eye && y in 0 until eye
        val inTopRight = x in (modules - eye) until modules && y in 0 until eye
        val inBottomLeft = x in 0 until eye && y in (modules - eye) until modules
        return inTopLeft || inTopRight || inBottomLeft
    }
}


