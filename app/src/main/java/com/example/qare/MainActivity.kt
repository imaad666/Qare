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
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.min
import androidx.compose.ui.unit.lerp

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
private const val KEY_EYE_STYLE = "eye_style"

@Composable
fun QAreAppScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var rollNumber by remember { mutableStateOf(prefs.getString(KEY_ROLL, null)) }

    // Defaults
    var pixelStyle by remember { mutableStateOf(PixelStyle.Rounded) }
    var eyeStyle by remember { mutableStateOf(EyeStyle.Rounded) }
    var pixelColor by remember { mutableStateOf(prefs.getInt(KEY_PIXEL_COLOR, Color.BLACK)) }
    var eyeColor by remember { mutableStateOf(prefs.getInt(KEY_EYE_COLOR, Color.BLACK)) }
    var bgColor by remember { mutableStateOf(prefs.getInt(KEY_BG_COLOR, blendWithWhite(pixelColor))) }

    // Load style choices if saved
    LaunchedEffect(Unit) {
        prefs.getString(KEY_PIXEL_STYLE, null)?.let { saved ->
            PixelStyle.values().firstOrNull { it.name == saved }?.let { pixelStyle = it }
        }
        prefs.getString(KEY_EYE_STYLE, null)?.let { saved ->
            EyeStyle.values().firstOrNull { it.name == saved }?.let { eyeStyle = it }
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
    val friendlyEyeLabels = mapOf(
        EyeStyle.Square to "Square",
        EyeStyle.Rounded to "Rounded",
        EyeStyle.ThreeBars to "Three Bars"
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
            val qrBitmap = remember(qrContent, pixelColor, eyeColor, pixelStyle, eyeStyle) {
                generateQrBitmap(
                    qrContent = qrContent,
                    pixelColor = pixelColor,
                    eyeColor = eyeColor,
                    backgroundColor = bgColor,
                    pixelStyle = pixelStyle,
                    eyeStyle = eyeStyle
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
                item { Text("Customisation") }
                item { Spacer(Modifier.height(8.dp)) }
                // Pixel color single slider
                item {
                    ColorHueSlider(
                        label = "Pixel colour",
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
                        label = "Eye colour",
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
                        label = "Pixel style",
                        options = PixelStyle.values().toList(),
                        selected = pixelStyle,
                        toLabel = { friendlyPixelLabels[it] ?: it.name },
                        onSelected = { sel ->
                            pixelStyle = sel
                            prefs.edit().putString(KEY_PIXEL_STYLE, sel.name).apply()
                        }
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
                // Eye style dropdown
                item {
                    StyleDropdown(
                        label = "Eye style",
                        options = EyeStyle.values().toList(),
                        selected = eyeStyle,
                        toLabel = { friendlyEyeLabels[it] ?: it.name },
                        onSelected = { sel ->
                            eyeStyle = sel
                            prefs.edit().putString(KEY_EYE_STYLE, sel.name).apply()
                        }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
                item {
                    Button(onClick = {
                        prefs.edit().clear().apply()
                        rollNumber = null
                        pixelStyle = PixelStyle.Rounded
                        eyeStyle = EyeStyle.Rounded
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
    eyeStyle: EyeStyle
): Bitmap? {
    return try {
        QRCodeUtils.createStyledQr(
            content = qrContent,
            size = 800,
            pixelColor = pixelColor,
            eyeColor = eyeColor,
            backgroundColor = backgroundColor,
            pixelStyle = pixelStyle,
            eyeStyle = eyeStyle
        )
    } catch (_: Exception) {
        null
    }
}
