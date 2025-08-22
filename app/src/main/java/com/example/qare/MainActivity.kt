package com.example.qare

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.BorderStroke
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.min
import androidx.compose.ui.unit.lerp
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                QAreAppScreen()
            }
        }
    }
}

private const val PREFS_NAME = "qare_prefs"
private const val KEY_ROLL = "roll"
private const val KEY_PIXEL_COLOR = "pixel_color"
private const val KEY_EYE_COLOR = "eye_color"
private const val KEY_BG_COLOR = "bg_color"
private const val KEY_PIXEL_STYLE = "pixel_style"
private const val KEY_EYE_STYLE = "eye_style" // legacy, will migrate to outline+pupil
private const val KEY_EYE_OUTLINE_STYLE = "eye_outline_style"
private const val KEY_PUPIL_STYLE = "pupil_style"

@Composable
fun QAreAppScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var rollNumber by remember { mutableStateOf(prefs.getString(KEY_ROLL, null)) }

    // Defaults
    var pixelStyle by remember { mutableStateOf(PixelStyle.Rounded) }
    var eyeOutlineStyle by remember { mutableStateOf(EyeOutlineStyle.Rounded) }
    var pupilStyle by remember { mutableStateOf(PupilStyle.Rounded) }
    var pixelColor by remember { mutableStateOf(prefs.getInt(KEY_PIXEL_COLOR, Color.BLACK)) }
    var eyeColor by remember { mutableStateOf(prefs.getInt(KEY_EYE_COLOR, Color.BLACK)) }
    var bgColor by remember { mutableStateOf(prefs.getInt(KEY_BG_COLOR, blendWithWhite(pixelColor))) }

    // Load style choices if saved
    LaunchedEffect(Unit) {
        prefs.getString(KEY_PIXEL_STYLE, null)?.let { saved ->
            PixelStyle.values().firstOrNull { it.name == saved }?.let { pixelStyle = it }
        }
        // Migrate legacy saved eye style string into outline
        prefs.getString(KEY_EYE_STYLE, null)?.let { legacy ->
            eyeOutlineStyle = when (legacy) {
                "Rounded" -> EyeOutlineStyle.Rounded
                "Square" -> EyeOutlineStyle.Square
                "ThreeBars" -> EyeOutlineStyle.Rounded
                else -> eyeOutlineStyle
            }
        }
        prefs.getString(KEY_EYE_OUTLINE_STYLE, null)?.let { saved ->
            EyeOutlineStyle.values().firstOrNull { it.name == saved }?.let { eyeOutlineStyle = it }
        }
        prefs.getString(KEY_PUPIL_STYLE, null)?.let { saved ->
            PupilStyle.values().firstOrNull { it.name == saved }?.let { pupilStyle = it }
        }
    }

    // Keep bgColor as faint version of pixelColor
    LaunchedEffect(pixelColor) {
        bgColor = blendWithWhite(pixelColor)
        prefs.edit().putInt(KEY_BG_COLOR, bgColor).apply()
    }

    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            val inputStream = context.contentResolver.openInputStream(selectedUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap != null) {
            val image = InputImage.fromBitmap(bitmap, 0)
            val scanner = BarcodeScanning.getClient()
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                        val first = barcodes.firstOrNull()?.rawValue
                        if (!first.isNullOrBlank()) {
                            parseRoll(first)?.let { roll ->
                                rollNumber = roll
                                prefs.edit().putString(KEY_ROLL, roll).apply()
                            }
                        }
                    }
            }
        }
    }

    val backgroundCompose = ComposeColor(bgColor)

    val friendlyPixelLabels = mapOf(
        PixelStyle.Square to "Square",
        PixelStyle.Rounded to "Rounded",
        PixelStyle.Dot to "Dot",
        PixelStyle.Continuous to "Continuous"
    )
    

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundCompose)
            .padding(16.dp)
    ) {
        if (rollNumber == null) {
            // Centered setup state
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("create your new qrs everyday")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { pickImageLauncher.launch("image/*") }) {
                        Text("Upload old QR from gallery")
                    }
                }
            }
                    } else {
            // Configured state with collapsing QR header
            val today = LocalDate.now(ZoneId.of("Asia/Kolkata"))
            val dateStr = today.format(DateTimeFormatter.ISO_DATE)
            val qrContent = "$rollNumber/$dateStr/student"
            val qrBitmap = remember(qrContent, pixelColor, eyeColor, pixelStyle, eyeOutlineStyle, pupilStyle) {
                generateQrBitmap(
                    qrContent = qrContent,
                    pixelColor = pixelColor,
                    eyeColor = eyeColor,
                    backgroundColor = bgColor,
                    pixelStyle = pixelStyle,
                    eyeOutlineStyle = eyeOutlineStyle,
                    pupilStyle = pupilStyle
                )
            }

            val listState = rememberLazyListState()
            val density = LocalDensity.current
            val collapseRangePx = with(density) { 200.dp.toPx() }
            val scrollPx = min(listState.firstVisibleItemScrollOffset.toFloat(), collapseRangePx)
            val frac = scrollPx / collapseRangePx
            val qrSize = lerp(320.dp, 160.dp, frac)

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(qrSize + 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        qrBitmap?.let { bmp ->
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Daily QR",
                                modifier = Modifier.size(qrSize)
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
                // removed heading per request
                // Pixel color single slider
                item {
                    ColorHueSlider(
                        label = "PIXEL COLOR",
                        currentColor = pixelColor,
                        onColorChange = { new ->
                            pixelColor = new
                            prefs.edit().putInt(KEY_PIXEL_COLOR, new).apply()
                        }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
                // Eye color single slider
                item {
                    ColorHueSlider(
                        label = "EYE COLOR",
                        currentColor = eyeColor,
                        onColorChange = { new ->
                            eyeColor = new
                            prefs.edit().putInt(KEY_EYE_COLOR, new).apply()
                        }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
                // Pixel style dropdown
                item {
                    StyleDropdown(
                        label = "PIXEL STYLE",
                        options = PixelStyle.values().toList(),
                        selected = pixelStyle,
                        toLabel = { (friendlyPixelLabels[it] ?: it.name).uppercase() },
                        onSelected = { sel ->
                            pixelStyle = sel
                            prefs.edit().putString(KEY_PIXEL_STYLE, sel.name).apply()
                        }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
                // Eye outline dropdown
                item {
                    StyleDropdown(
                        label = "EYE OUTLINE",
                        options = EyeOutlineStyle.values().toList(),
                        selected = eyeOutlineStyle,
                        toLabel = { (mapOf(
                            EyeOutlineStyle.Square to "EDGED",
                            EyeOutlineStyle.Rounded to "ROUNDED",
                            EyeOutlineStyle.Dotted to "DOTTED"
                        )[it] ?: it.name).uppercase() },
                        onSelected = { sel ->
                            eyeOutlineStyle = sel
                            prefs.edit().putString(KEY_EYE_OUTLINE_STYLE, sel.name).apply()
                        }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
                // Pupil style dropdown
                item {
                    StyleDropdown(
                        label = "PUPIL STYLE",
                        options = PupilStyle.values().toList(),
                        selected = pupilStyle,
                        toLabel = { (mapOf(
                            PupilStyle.Square to "EDGED",
                            PupilStyle.Rounded to "ROUNDED",
                            PupilStyle.ThreeBars to "THREE BARS",
                            PupilStyle.ThreeBarsRounded to "THREE BARS ROUNDED"
                        )[it] ?: it.toString()).uppercase() },
                        onSelected = { sel ->
                            pupilStyle = sel
                            prefs.edit().putString(KEY_PUPIL_STYLE, sel.name).apply()
                        }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
                item {
                    Button(onClick = {
                        prefs.edit().clear().apply()
                        rollNumber = null
                        pixelStyle = PixelStyle.Rounded
                        eyeOutlineStyle = EyeOutlineStyle.Rounded
                        pupilStyle = PupilStyle.Rounded
                        pixelColor = Color.BLACK
                        eyeColor = Color.BLACK
                        bgColor = blendWithWhite(pixelColor)
                    }) {
                        Text("Reset Profile")
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun ColorHueSlider(label: String, currentColor: Int, onColorChange: (Int) -> Unit) {
    var hue by remember {
        val hsv = FloatArray(3)
        Color.colorToHSV(currentColor, hsv)
        mutableStateOf(hsv[0])
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label)
        Slider(
            value = hue,
            onValueChange = {
                hue = it
                val color = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
                onColorChange(color)
            },
            valueRange = 0f..360f
        )
    }
}

@Composable
private fun <T> StyleDropdown(
    label: String,
    options: List<T>,
    selected: T,
    toLabel: (T) -> String,
    onSelected: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label)
        Spacer(Modifier.height(4.dp))
        Button(onClick = { expanded = true }) { Text(toLabel(selected)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(toLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun parseRoll(text: String): String? {
    val parts = text.trim().split("/")
    if (parts.size != 3) return null
    val roll = parts[0].trim()
    if (roll.isEmpty()) return null
    return roll
}

private fun blendWithWhite(color: Int): Int {
    val w = ComposeColor.White.toArgb()
    val r = (Color.red(color) * 0.2 + Color.red(w) * 0.8).toInt().coerceIn(0, 255)
    val g = (Color.green(color) * 0.2 + Color.green(w) * 0.8).toInt().coerceIn(0, 255)
    val b = (Color.blue(color) * 0.2 + Color.blue(w) * 0.8).toInt().coerceIn(0, 255)
    return Color.rgb(r, g, b)
}

private fun generateQrBitmap(
    qrContent: String,
    pixelColor: Int,
    eyeColor: Int,
    backgroundColor: Int,
    pixelStyle: PixelStyle,
    eyeOutlineStyle: EyeOutlineStyle,
    pupilStyle: PupilStyle
): Bitmap? {
    return try {
        QRCodeUtils.createStyledQr(
            content = qrContent,
            size = 800,
            pixelColor = pixelColor,
            eyeColor = eyeColor,
            backgroundColor = backgroundColor,
            pixelStyle = pixelStyle,
            eyeOutlineStyle = eyeOutlineStyle,
            pupilStyle = pupilStyle
        )
    } catch (_: Exception) {
        null
    }
}
