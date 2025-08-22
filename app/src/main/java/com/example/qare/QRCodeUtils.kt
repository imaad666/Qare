package com.example.qare

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

enum class PixelStyle {
    Square,
    Rounded,
    Dot,
    Continuous,
    Star,
    Heart,
    Flower,
    Blob,
    Split,
    Diamond,
    Hexagon,
    Triangle,
    Cross
}
enum class EyeOutlineStyle {
    Square,
    Rounded,
    Circle,
    Diamond,
    Leaf,
    Octagon,
    BoldFrame,
    ThinFrame
}
enum class PupilStyle {
    Square,
    Rounded,
    ThreeBars,
    ThreeBarsRounded,
    Circle,
    Diamond,
    Cross,
    TinyDot,
    ThickBorderDot
}

object QRCodeUtils {
    fun createStyledQr(
        content: String,
        size: Int,
        pixelColor: Int = Color.BLACK,
        eyeColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE,
        pixelStyle: PixelStyle = PixelStyle.Rounded,
        eyeOutlineStyle: EyeOutlineStyle = EyeOutlineStyle.Rounded,
        pupilStyle: PupilStyle = PupilStyle.Rounded
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
            PixelStyle.Star -> drawStars(matrix, canvas, paintPixel, moduleSize, offsetX, offsetY)
            PixelStyle.Heart -> drawHearts(matrix, canvas, paintPixel, moduleSize, offsetX, offsetY)
            PixelStyle.Flower -> drawFlowers(matrix, canvas, paintPixel, moduleSize, offsetX, offsetY)
            PixelStyle.Blob -> drawBlobs(matrix, canvas, paintPixel, moduleSize, offsetX, offsetY)
            PixelStyle.Split -> drawSplit(matrix, canvas, paintPixel, moduleSize, offsetX, offsetY)
            PixelStyle.Diamond -> drawDiamonds(matrix, canvas, paintPixel, moduleSize, offsetX, offsetY)
            PixelStyle.Hexagon -> drawHexagons(matrix, canvas, paintPixel, moduleSize, offsetX, offsetY)
            PixelStyle.Triangle -> drawTriangles(matrix, canvas, paintPixel, moduleSize, offsetX, offsetY)
            PixelStyle.Cross -> drawCrosses(matrix, canvas, paintPixel, moduleSize, offsetX, offsetY)
        }

        // Draw eyes over modules so eye color always applies
        drawEyes(canvas, modules, moduleSize, offsetX, offsetY, paintEye, backgroundColor, eyeOutlineStyle, pupilStyle)

        return bitmap
    }

    private fun drawStars(
        matrix: com.google.zxing.qrcode.encoder.ByteMatrix,
        canvas: Canvas,
        paint: Paint,
        moduleSize: Float,
        offsetX: Float,
        offsetY: Float
    ) {
        val modules = matrix.width
        val angleStep = (2.0 * PI / 10.0).toFloat()
        val startAngle = (-PI / 2.0).toFloat()
        for (y in 0 until modules) {
            for (x in 0 until modules) {
                if (matrix.get(x, y).toInt() != 0 && !isEyeModule(x, y, modules)) {
                    val l = offsetX + x * moduleSize
                    val t = offsetY + y * moduleSize
                    val cx = l + moduleSize * 0.5f
                    val cy = t + moduleSize * 0.5f
                    val outer = moduleSize * 0.48f
                    val inner = outer * 0.45f
                    val path = android.graphics.Path()
                    for (i in 0 until 10) {
                        val r = if (i % 2 == 0) outer else inner
                        val a = startAngle + i * angleStep
                        val px = cx + r * cos(a)
                        val py = cy + r * sin(a)
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                    canvas.drawPath(path, paint)
                }
            }
        }
    }

    private fun drawHearts(
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
                    val r = l + moduleSize
                    val b = t + moduleSize
                    val cx = (l + r) / 2f
                    val top = t + moduleSize * 0.15f
                    val path = android.graphics.Path().apply {
                        moveTo(cx, b - moduleSize * 0.12f)
                        cubicTo(
                            r, b - moduleSize * 0.35f,
                            r - moduleSize * 0.05f, top + moduleSize * 0.20f,
                            cx, top
                        )
                        cubicTo(
                            l + moduleSize * 0.05f, top + moduleSize * 0.20f,
                            l, b - moduleSize * 0.35f,
                            cx, b - moduleSize * 0.12f
                        )
                        close()
                    }
                    canvas.drawPath(path, paint)
                }
            }
        }
    }

    private fun drawFlowers(
        matrix: com.google.zxing.qrcode.encoder.ByteMatrix,
        canvas: Canvas,
        paint: Paint,
        moduleSize: Float,
        offsetX: Float,
        offsetY: Float
    ) {
        val modules = matrix.width
        val rp = moduleSize * 0.22f
        for (y in 0 until modules) {
            for (x in 0 until modules) {
                if (matrix.get(x, y).toInt() != 0 && !isEyeModule(x, y, modules)) {
                    val l = offsetX + x * moduleSize
                    val t = offsetY + y * moduleSize
                    val cx = l + moduleSize * 0.5f
                    val cy = t + moduleSize * 0.5f
                    canvas.drawCircle(cx - rp, cy, rp, paint)
                    canvas.drawCircle(cx + rp, cy, rp, paint)
                    canvas.drawCircle(cx, cy - rp, rp, paint)
                    canvas.drawCircle(cx, cy + rp, rp, paint)
                }
            }
        }
    }

    private fun drawBlobs(
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
                    val cx = l + moduleSize * 0.5f
                    val cy = t + moduleSize * 0.5f
                    val base = moduleSize * 0.46f
                    val path = android.graphics.Path()
                    for (i in 0 until 8) {
                        val ang = (2.0 * PI * i / 8.0).toFloat()
                        val noise = 0.85f + 0.25f * noiseFromCoords(x * 31 + i, y * 17 + i, 12345)
                        val r = base * noise
                        val px = cx + r * cos(ang)
                        val py = cy + r * sin(ang)
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                    canvas.drawPath(path, paint)
                }
            }
        }
    }

    private fun drawSplit(
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
                    val r = l + moduleSize
                    val b = t + moduleSize
                    val path = android.graphics.Path()
                    val parity = (x + y) and 1
                    if (parity == 0) {
                        path.moveTo(l, t)
                        path.lineTo(r, t)
                        path.lineTo(l, b)
                        path.close()
                    } else {
                        path.moveTo(r, b)
                        path.lineTo(r, t)
                        path.lineTo(l, b)
                        path.close()
                    }
                    canvas.drawPath(path, paint)
                }
            }
        }
    }

    private fun drawDiamonds(
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
                    val r = l + moduleSize
                    val b = t + moduleSize
                    val cx = (l + r) / 2f
                    val cy = (t + b) / 2f
                    val half = moduleSize * 0.5f
                    val margin = moduleSize * 0.12f
                    val path = android.graphics.Path().apply {
                        moveTo(cx, t + margin)
                        lineTo(r - margin, cy)
                        lineTo(cx, b - margin)
                        lineTo(l + margin, cy)
                        close()
                    }
                    canvas.drawPath(path, paint)
                }
            }
        }
    }

    private fun drawHexagons(
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
                    val cx = l + moduleSize * 0.5f
                    val cy = t + moduleSize * 0.5f
                    val r = moduleSize * 0.48f
                    val path = android.graphics.Path()
                    for (i in 0 until 6) {
                        val a = (-PI / 2 + i * (PI / 3)).toFloat()
                        val px = cx + r * cos(a)
                        val py = cy + r * sin(a)
                        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                    canvas.drawPath(path, paint)
                }
            }
        }
    }

    private fun drawTriangles(
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
                    val r = l + moduleSize
                    val b = t + moduleSize
                    val parity = (x + y) and 1
                    val path = android.graphics.Path()
                    if (parity == 0) {
                        path.moveTo((l + r) / 2f, t + moduleSize * 0.12f)
                        path.lineTo(r - moduleSize * 0.12f, b - moduleSize * 0.12f)
                        path.lineTo(l + moduleSize * 0.12f, b - moduleSize * 0.12f)
                    } else {
                        path.moveTo((l + r) / 2f, b - moduleSize * 0.12f)
                        path.lineTo(r - moduleSize * 0.12f, t + moduleSize * 0.12f)
                        path.lineTo(l + moduleSize * 0.12f, t + moduleSize * 0.12f)
                    }
                    path.close()
                    canvas.drawPath(path, paint)
                }
            }
        }
    }

    private fun drawCrosses(
        matrix: com.google.zxing.qrcode.encoder.ByteMatrix,
        canvas: Canvas,
        paint: Paint,
        moduleSize: Float,
        offsetX: Float,
        offsetY: Float
    ) {
        val modules = matrix.width
        val thickness = moduleSize * 0.3f
        for (y in 0 until modules) {
            for (x in 0 until modules) {
                if (matrix.get(x, y).toInt() != 0 && !isEyeModule(x, y, modules)) {
                    val l = offsetX + x * moduleSize
                    val t = offsetY + y * moduleSize
                    val cx = l + moduleSize * 0.5f
                    val cy = t + moduleSize * 0.5f
                    // vertical bar
                    canvas.drawRoundRect(
                        cx - thickness / 2f,
                        t + moduleSize * 0.12f,
                        cx + thickness / 2f,
                        t + moduleSize - moduleSize * 0.12f,
                        thickness / 2f,
                        thickness / 2f,
                        paint
                    )
                    // horizontal bar
                    canvas.drawRoundRect(
                        l + moduleSize * 0.12f,
                        cy - thickness / 2f,
                        l + moduleSize - moduleSize * 0.12f,
                        cy + thickness / 2f,
                        thickness / 2f,
                        thickness / 2f,
                        paint
                    )
                }
            }
        }
    }

    private fun noiseFromCoords(x: Int, y: Int, salt: Int): Float {
        var n = x * 73856093 xor y * 19349663 xor salt
        n = (n shl 13) xor n
        val t = (n * (n * n * 15731 + 789221) + 1376312589)
        val v = (t and 0x7fffffff) / 1073741824.0f // ~ [0,2)
        return (v * 0.5f).coerceIn(0f, 1f)
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
        eyeOutlineStyle: EyeOutlineStyle,
        pupilStyle: PupilStyle
    ) {
        val eye = 7
        val positions = listOf(
            0 to 0, // top-left
            modules - eye to 0, // top-right
            0 to modules - eye // bottom-left
        )
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backgroundColor }
        for ((ex, ey) in positions) {
            val l = offsetX + ex * moduleSize
            val t = offsetY + ey * moduleSize
            val r = l + eye * moduleSize
            val b = t + eye * moduleSize
            // Draw eye outline ring
            when (eyeOutlineStyle) {
                EyeOutlineStyle.Square -> {
                    // Default ring thickness = 1 module
                    canvas.drawRect(l, t, r, b, eyePaint)
                    val inset = moduleSize
                    canvas.drawRect(l + inset, t + inset, r - inset, b - inset, bgPaint)
                }
                EyeOutlineStyle.Rounded -> {
                    val radius = moduleSize
                    canvas.drawRoundRect(l, t, r, b, radius, radius, eyePaint)
                    val inset = moduleSize
                    canvas.drawRoundRect(l + inset, t + inset, r - inset, b - inset, radius, radius, bgPaint)
                }
                EyeOutlineStyle.Circle -> {
                    val cx = (l + r) / 2f
                    val cy = (t + b) / 2f
                    val outer = (r - l) / 2f
                    val inner = outer - moduleSize
                    canvas.drawCircle(cx, cy, outer, eyePaint)
                    canvas.drawCircle(cx, cy, inner, bgPaint)
                }
                EyeOutlineStyle.Diamond -> {
                    val cx = (l + r) / 2f
                    val cy = (t + b) / 2f
                    val halfW = (r - l) / 2f
                    val halfH = (b - t) / 2f
                    val outerPath = android.graphics.Path().apply {
                        moveTo(cx, t)
                        lineTo(r, cy)
                        lineTo(cx, b)
                        lineTo(l, cy)
                        close()
                    }
                    val inset = moduleSize
                    val innerPath = android.graphics.Path().apply {
                        moveTo(cx, t + inset)
                        lineTo(r - inset, cy)
                        lineTo(cx, b - inset)
                        lineTo(l + inset, cy)
                        close()
                    }
                    canvas.drawPath(outerPath, eyePaint)
                    canvas.drawPath(innerPath, bgPaint)
                }
                EyeOutlineStyle.Leaf -> {
                    // Petal-like by using a large corner radius
                    val outerRadius = moduleSize * 2.2f
                    canvas.drawRoundRect(l, t, r, b, outerRadius, outerRadius, eyePaint)
                    val inset = moduleSize
                    canvas.drawRoundRect(l + inset, t + inset, r - inset, b - inset, outerRadius, outerRadius, bgPaint)
                }
                EyeOutlineStyle.Octagon -> {
                    fun octagonPath(left: Float, top: Float, right: Float, bottom: Float, cut: Float): android.graphics.Path {
                        val p = android.graphics.Path()
                        p.moveTo(left + cut, top)
                        p.lineTo(right - cut, top)
                        p.lineTo(right, top + cut)
                        p.lineTo(right, bottom - cut)
                        p.lineTo(right - cut, bottom)
                        p.lineTo(left + cut, bottom)
                        p.lineTo(left, bottom - cut)
                        p.lineTo(left, top + cut)
                        p.close()
                        return p
                    }
                    val cut = moduleSize
                    val outerPath = octagonPath(l, t, r, b, cut)
                    val inset = moduleSize
                    val innerPath = octagonPath(l + inset, t + inset, r - inset, b - inset, cut)
                    canvas.drawPath(outerPath, eyePaint)
                    canvas.drawPath(innerPath, bgPaint)
                }
                EyeOutlineStyle.BoldFrame -> {
                    val thickness = moduleSize * 1.5f
                    canvas.drawRect(l, t, r, b, eyePaint)
                    canvas.drawRect(l + thickness, t + thickness, r - thickness, b - thickness, bgPaint)
                }
                EyeOutlineStyle.ThinFrame -> {
                    val thickness = moduleSize * 0.6f
                    canvas.drawRect(l, t, r, b, eyePaint)
                    canvas.drawRect(l + thickness, t + thickness, r - thickness, b - thickness, bgPaint)
                }
            }
            // Draw pupil inside center 3x3
            val l3 = l + 2 * moduleSize
            val t3 = t + 2 * moduleSize
            val r3 = r - 2 * moduleSize
            val b3 = b - 2 * moduleSize
            when (pupilStyle) {
                PupilStyle.Square -> canvas.drawRect(l3, t3, r3, b3, eyePaint)
                PupilStyle.Rounded -> canvas.drawRoundRect(l3, t3, r3, b3, moduleSize, moduleSize, eyePaint)
                PupilStyle.ThreeBars, PupilStyle.ThreeBarsRounded -> {
                    val barWidth = (r3 - l3) / 3f
                    val corner = if (pupilStyle == PupilStyle.ThreeBarsRounded) moduleSize * 0.6f else 0f
                    for (i in 0..2) {
                        val bl = l3 + i * barWidth
                        val br = bl + barWidth - moduleSize * 0.1f
                        if (corner > 0f) {
                            canvas.drawRoundRect(bl, t3, br, b3, corner, corner, eyePaint)
                        } else {
                            canvas.drawRect(bl, t3, br, b3, eyePaint)
                        }
                    }
                }
                PupilStyle.Circle -> {
                    val cx = (l3 + r3) / 2f
                    val cy = (t3 + b3) / 2f
                    val radius = (r3 - l3) * 0.5f
                    canvas.drawCircle(cx, cy, radius, eyePaint)
                }
                PupilStyle.Diamond -> {
                    val cx = (l3 + r3) / 2f
                    val cy = (t3 + b3) / 2f
                    val half = (r3 - l3) / 2f
                    val path = android.graphics.Path().apply {
                        moveTo(cx, cy - half)
                        lineTo(cx + half, cy)
                        lineTo(cx, cy + half)
                        lineTo(cx - half, cy)
                        close()
                    }
                    canvas.drawPath(path, eyePaint)
                }
                PupilStyle.Cross -> {
                    val cx = (l3 + r3) / 2f
                    val cy = (t3 + b3) / 2f
                    val thickness = (r3 - l3) * 0.22f
                    // vertical bar
                    canvas.drawRect(cx - thickness / 2f, t3, cx + thickness / 2f, b3, eyePaint)
                    // horizontal bar
                    canvas.drawRect(l3, cy - thickness / 2f, r3, cy + thickness / 2f, eyePaint)
                }
                PupilStyle.TinyDot -> {
                    val cx = (l3 + r3) / 2f
                    val cy = (t3 + b3) / 2f
                    val radius = (r3 - l3) * 0.18f
                    canvas.drawCircle(cx, cy, radius, eyePaint)
                }
                PupilStyle.ThickBorderDot -> {
                    val cx = (l3 + r3) / 2f
                    val cy = (t3 + b3) / 2f
                    val outer = (r3 - l3) * 0.5f
                    val inner = outer * 0.5f
                    // Outer circle
                    canvas.drawCircle(cx, cy, outer, eyePaint)
                    // Punch inner hole with background
                    val bgPaintRing = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backgroundColor }
                    canvas.drawCircle(cx, cy, inner, bgPaintRing)
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


