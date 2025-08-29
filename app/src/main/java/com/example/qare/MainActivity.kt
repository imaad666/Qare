package com.example.qare

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.min
import androidx.compose.ui.unit.lerp
import kotlin.random.Random
import com.example.qare.ui.theme.QAreTheme
import com.example.qare.ui.theme.RequinerFamily
import com.example.qare.ui.theme.SuperchargeFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QAreTheme { QAreAppScreen() }
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
private const val KEY_PRESET_PIXEL_COLOR = "preset_pixel_color"
private const val KEY_PRESET_EYE_COLOR = "preset_eye_color"
private const val KEY_PRESET_PIXEL_STYLE = "preset_pixel_style"
private const val KEY_PRESET_EYE_OUTLINE_STYLE = "preset_eye_outline_style"
private const val KEY_PRESET_PUPIL_STYLE = "preset_pupil_style"
private const val DEFAULT_PIXEL_COLOR_HEX = "#EF4444"
private const val DEFAULT_EYE_COLOR_HEX = "#000000"

@Composable
fun QAreAppScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    var rollNumber by remember { mutableStateOf(prefs.getString(KEY_ROLL, null)) }

    // Defaults
    var pixelStyle by remember { mutableStateOf(PixelStyle.Rounded) }
    var eyeOutlineStyle by remember { mutableStateOf(EyeOutlineStyle.Rounded) }
    var pupilStyle by remember { mutableStateOf(PupilStyle.Rounded) }
    var pixelColor by remember { mutableStateOf(prefs.getInt(KEY_PIXEL_COLOR, Color.parseColor(DEFAULT_PIXEL_COLOR_HEX))) }
    var eyeColor by remember { mutableStateOf(prefs.getInt(KEY_EYE_COLOR, Color.parseColor(DEFAULT_EYE_COLOR_HEX))) }
    val backgroundColor by remember(pixelColor, eyeColor) { mutableStateOf(deriveReadableBackground(pixelColor, eyeColor)) }

    // Load style choices if saved (and coerce disallowed styles)
    LaunchedEffect(Unit) {
        prefs.getString(KEY_PIXEL_STYLE, null)?.let { saved ->
            if (saved == PixelStyle.Split.name) {
                pixelStyle = PixelStyle.Rounded
                prefs.edit().putString(KEY_PIXEL_STYLE, PixelStyle.Rounded.name).apply()
            } else {
                PixelStyle.values().firstOrNull { it.name == saved }?.let { pixelStyle = it }
            }
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
            if (saved == EyeOutlineStyle.Diamond.name || saved == EyeOutlineStyle.Circle.name) {
                eyeOutlineStyle = EyeOutlineStyle.Rounded
                prefs.edit().putString(KEY_EYE_OUTLINE_STYLE, EyeOutlineStyle.Rounded.name).apply()
            } else {
                EyeOutlineStyle.values().firstOrNull { it.name == saved }?.let { eyeOutlineStyle = it }
            }
        }
        prefs.getString(KEY_PUPIL_STYLE, null)?.let { saved ->
            if (saved == PupilStyle.TinyDot.name || saved == PupilStyle.ThickBorderDot.name) {
                pupilStyle = PupilStyle.Rounded
                prefs.edit().putString(KEY_PUPIL_STYLE, PupilStyle.Rounded.name).apply()
            } else {
                PupilStyle.values().firstOrNull { it.name == saved }?.let { pupilStyle = it }
            }
        }
    }

    // Keep bgColor as faint version of pixelColor
    LaunchedEffect(backgroundColor) {
        prefs.edit().putInt(KEY_BG_COLOR, backgroundColor).apply()
    }
    
    // Ensure new default colors are applied on first startup
    LaunchedEffect(Unit) {
        if (!prefs.contains(KEY_PIXEL_COLOR)) {
            pixelColor = Color.parseColor(DEFAULT_PIXEL_COLOR_HEX)
            prefs.edit().putInt(KEY_PIXEL_COLOR, pixelColor).apply()
        }
        if (!prefs.contains(KEY_EYE_COLOR)) {
            eyeColor = Color.parseColor(DEFAULT_EYE_COLOR_HEX)
            prefs.edit().putInt(KEY_EYE_COLOR, eyeColor).apply()
        }
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
                                // Ensure default colors are initialized to red pixels and black eyes on first setup
                                val defaultPixel = Color.parseColor(DEFAULT_PIXEL_COLOR_HEX)
                                val defaultEye = Color.parseColor(DEFAULT_EYE_COLOR_HEX)
                                val savedPixel = prefs.getInt(KEY_PIXEL_COLOR, Color.BLACK)
                                val savedEye = prefs.getInt(KEY_EYE_COLOR, Color.BLACK)
                                if (!prefs.contains(KEY_PIXEL_COLOR) || savedPixel == Color.BLACK) {
                                    pixelColor = defaultPixel
                                    prefs.edit().putInt(KEY_PIXEL_COLOR, defaultPixel).apply()
                                }
                                if (!prefs.contains(KEY_EYE_COLOR) || savedEye == Color.BLACK) {
                                    eyeColor = defaultEye
                                    prefs.edit().putInt(KEY_EYE_COLOR, defaultEye).apply()
                                }
                            }
                        }
                    }
            }
        }
    }

    // Runtime permission launcher for legacy external storage write (Android 9 and below)
    val writePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        // No-op here; we check again when trying to save
    }

    val backgroundCompose = ComposeColor(backgroundColor)
    val bgLum = (0.299f * backgroundCompose.red + 0.587f * backgroundCompose.green + 0.114f * backgroundCompose.blue)
    val onBackgroundColor = if (bgLum < 0.5f) ComposeColor.White else ComposeColor.Black

    val friendlyPixelLabels = mapOf(
        PixelStyle.Square to "Square",
        PixelStyle.Rounded to "Rounded",
        PixelStyle.Dot to "Dot",
        PixelStyle.Continuous to "Continuous",
        PixelStyle.Star to "Star",
        PixelStyle.Heart to "Petal",
        PixelStyle.Flower to "Flower",
        PixelStyle.Blob to "Blob",
        PixelStyle.Split to "Split",
        PixelStyle.Diamond to "Diamond",
        PixelStyle.Hexagon to "Hexagon",
        PixelStyle.Triangle to "Triangle",
        PixelStyle.Cross to "Cross"
    )
    

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundCompose)
    ) {
        if (rollNumber == null) {
            // Centered setup state
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 20.dp)
                ) {
                    AppButton(
                        text = "Upload Old QR From Gallery",
                        backgroundColor = Color.parseColor(DEFAULT_PIXEL_COLOR_HEX),
                        onClick = { pickImageLauncher.launch("image/*") }
                    )
                }
            }
                    } else {
            // Configured state with collapsing QR header
            val today = LocalDate.now(ZoneId.of("Asia/Kolkata"))
            val dateStr = today.format(DateTimeFormatter.ISO_DATE)
            val qrContent = "$rollNumber/$dateStr/student"
            val qrBitmap = remember(qrContent, pixelColor, eyeColor, backgroundColor, pixelStyle, eyeOutlineStyle, pupilStyle) {
                generateQrBitmap(
                    qrContent = qrContent,
                    pixelColor = pixelColor,
                    eyeColor = eyeColor,
                    backgroundColor = backgroundColor,
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
            val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            var showAuthorDialog by remember { mutableStateOf(false) }
            var showQrSplash by remember { mutableStateOf(false) }
            var showPresetDialog by remember { mutableStateOf(false) }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 20.dp, end = 20.dp),
                contentPadding = PaddingValues(top = 48.dp, bottom = 96.dp + bottomInset),
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
                                modifier = Modifier
                                    .size(qrSize)
                                    .clickable { showQrSplash = true }
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
                // removed heading per request
                // Pixel color single slider
                item {
                    ColorSetting(
                        label = "Pixel Color",
                        currentColor = pixelColor,
                        onColorChange = { new ->
                            pixelColor = new
                            prefs.edit().putInt(KEY_PIXEL_COLOR, new).apply()
                        },
                        labelColor = onBackgroundColor
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
                // Eye color single slider
                item {
                    ColorSetting(
                        label = "Eye Color",
                        currentColor = eyeColor,
                        onColorChange = { new ->
                            eyeColor = new
                            prefs.edit().putInt(KEY_EYE_COLOR, new).apply()
                        },
                        labelColor = onBackgroundColor
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
                // Pixel style dropdown
                item {
                    StyleDropdown(
                        label = "Pixel",
                        options = PixelStyle.values().filter { it != PixelStyle.Split },
                        selected = pixelStyle,
                        toLabel = { (friendlyPixelLabels[it] ?: it.name) },
                        onSelected = { sel ->
                            pixelStyle = sel
                            prefs.edit().putString(KEY_PIXEL_STYLE, sel.name).apply()
                        },
                        backgroundColor = pixelColor,
                        labelColor = onBackgroundColor
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
                // Eye outline dropdown
                item {
                    StyleDropdown(
                        label = "Eye",
                        options = EyeOutlineStyle.values().filter { it != EyeOutlineStyle.Diamond && it != EyeOutlineStyle.Circle },
                        selected = eyeOutlineStyle,
                        toLabel = { (mapOf(
                            EyeOutlineStyle.Square to "Square",
                            EyeOutlineStyle.Rounded to "Rounded",
                            EyeOutlineStyle.Leaf to "Petal",
                            EyeOutlineStyle.Octagon to "Octagon",
                            EyeOutlineStyle.BoldFrame to "Bold Frame",
                            EyeOutlineStyle.ThinFrame to "Thin Frame"
                        )[it] ?: it.name) },
                        onSelected = { sel ->
                            eyeOutlineStyle = sel
                            prefs.edit().putString(KEY_EYE_OUTLINE_STYLE, sel.name).apply()
                        },
                        backgroundColor = pixelColor,
                        labelColor = onBackgroundColor
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
                // Pupil style dropdown
                item {
                    StyleDropdown(
                        label = "Pupil",
                        options = PupilStyle.values().filter { it != PupilStyle.TinyDot && it != PupilStyle.ThickBorderDot },
                        selected = pupilStyle,
                        toLabel = { (mapOf(
                            PupilStyle.Square to "Edged",
                            PupilStyle.Rounded to "Rounded",
                            PupilStyle.ThreeBars to "Three Bars",
                            PupilStyle.ThreeBarsRounded to "Three Bars Rounded",
                            PupilStyle.Circle to "Circle",
                            PupilStyle.Diamond to "Diamond",
                            PupilStyle.Cross to "Cross",
                            
                        )[it] ?: it.toString()) },
                        onSelected = { sel ->
                            pupilStyle = sel
                            prefs.edit().putString(KEY_PUPIL_STYLE, sel.name).apply()
                        },
                        backgroundColor = pixelColor,
                        labelColor = onBackgroundColor
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
                // Animated gradient Author button
                item { WildAuthorButton(text = "Author", onClick = { showAuthorDialog = true }, textColor = onBackgroundColor) }
            }

            // Sticky bottom icon action bar
            BottomActionBar(
                pixelColor = pixelColor,
                onReset = {
                    prefs.edit().clear().apply()
                    rollNumber = null
                    pixelStyle = PixelStyle.Rounded
                    eyeOutlineStyle = EyeOutlineStyle.Rounded
                    pupilStyle = PupilStyle.Rounded
                    pixelColor = Color.BLACK
                    eyeColor = Color.BLACK
                },
                onPreset = {
                    // Load preset values
                    val presetPixelColor = prefs.getInt(KEY_PRESET_PIXEL_COLOR, pixelColor)
                    val presetEyeColor = prefs.getInt(KEY_PRESET_EYE_COLOR, eyeColor)
                    val presetPixelStyle = prefs.getString(KEY_PRESET_PIXEL_STYLE, pixelStyle.name)?.let { 
                        PixelStyle.values().firstOrNull { style -> style.name == it } 
                    } ?: pixelStyle
                    val presetEyeOutlineStyle = prefs.getString(KEY_PRESET_EYE_OUTLINE_STYLE, eyeOutlineStyle.name)?.let { 
                        EyeOutlineStyle.values().firstOrNull { style -> style.name == it } 
                    } ?: eyeOutlineStyle
                    val presetPupilStyle = prefs.getString(KEY_PRESET_PUPIL_STYLE, pupilStyle.name)?.let { 
                        PupilStyle.values().firstOrNull { style -> style.name == it } 
                    } ?: pupilStyle
                    
                    pixelColor = presetPixelColor
                    eyeColor = presetEyeColor
                    pixelStyle = presetPixelStyle
                    eyeOutlineStyle = presetEyeOutlineStyle
                    pupilStyle = presetPupilStyle
                    
                    prefs.edit()
                        .putInt(KEY_PIXEL_COLOR, presetPixelColor)
                        .putInt(KEY_EYE_COLOR, presetEyeColor)
                        .putString(KEY_PIXEL_STYLE, presetPixelStyle.name)
                        .putString(KEY_EYE_OUTLINE_STYLE, presetEyeOutlineStyle.name)
                        .putString(KEY_PUPIL_STYLE, presetPupilStyle.name)
                        .apply()
                },
                onPresetLongPress = {
                    showPresetDialog = true
                },
                onShuffle = {
                    // Randomize from allowed sets only
                    val newPixel = Random.nextInt(0x000000, 0xFFFFFF) or 0xFF000000.toInt()
                    val newEye = Random.nextInt(0x000000, 0xFFFFFF) or 0xFF000000.toInt()
                    val allowedPixels = PixelStyle.values().filter { it != PixelStyle.Split }
                    val allowedEyes = EyeOutlineStyle.values().filter { it != EyeOutlineStyle.Diamond && it != EyeOutlineStyle.Circle }
                    val allowedPupils = PupilStyle.values().filter { it != PupilStyle.TinyDot && it != PupilStyle.ThickBorderDot }
                    val newPixelStyle = allowedPixels[Random.nextInt(allowedPixels.size)]
                    val newEyeOutline = allowedEyes[Random.nextInt(allowedEyes.size)]
                    val newPupil = allowedPupils[Random.nextInt(allowedPupils.size)]
                    pixelColor = newPixel
                    eyeColor = newEye
                    pixelStyle = newPixelStyle
                    eyeOutlineStyle = newEyeOutline
                    pupilStyle = newPupil
                    prefs.edit()
                        .putInt(KEY_PIXEL_COLOR, newPixel)
                        .putInt(KEY_EYE_COLOR, newEye)
                        .putString(KEY_PIXEL_STYLE, newPixelStyle.name)
                        .putString(KEY_EYE_OUTLINE_STYLE, newEyeOutline.name)
                        .putString(KEY_PUPIL_STYLE, newPupil.name)
                        .apply()
                },
                onDownloadPng = {
                    // For Android 9 and below, ensure WRITE_EXTERNAL_STORAGE is granted
                    val needsLegacyWrite = android.os.Build.VERSION.SDK_INT <= 28
                    if (needsLegacyWrite) {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            writePermLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            Toast.makeText(context, "Grant storage permission and try again", Toast.LENGTH_SHORT).show()
                            return@BottomActionBar
                        }
                    }
                    val ok = exportQrPng(
                        context = context,
                        bitmap = qrBitmap,
                        baseFileName = "QAre_${today.format(DateTimeFormatter.BASIC_ISO_DATE)}"
                    )
                    if (ok) Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show() else Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 20.dp, top = 0.dp, end = 20.dp, bottom = 12.dp)
            )

            if (showAuthorDialog) {
                AuthorDialog(onDismiss = { showAuthorDialog = false })
            }
            
            if (showPresetDialog) {
                PresetEditDialog(
                    currentPixelColor = pixelColor,
                    currentEyeColor = eyeColor,
                    currentPixelStyle = pixelStyle,
                    currentEyeOutlineStyle = eyeOutlineStyle,
                    currentPupilStyle = pupilStyle,
                    onSave = { presetPixelColor, presetEyeColor, presetPixelStyle, presetEyeOutlineStyle, presetPupilStyle ->
                        prefs.edit()
                            .putInt(KEY_PRESET_PIXEL_COLOR, presetPixelColor)
                            .putInt(KEY_PRESET_EYE_COLOR, presetEyeColor)
                            .putString(KEY_PRESET_PIXEL_STYLE, presetPixelStyle.name)
                            .putString(KEY_PRESET_EYE_OUTLINE_STYLE, presetEyeOutlineStyle.name)
                            .putString(KEY_PRESET_PUPIL_STYLE, presetPupilStyle.name)
                            .apply()
                        showPresetDialog = false
                    },
                    onDismiss = { showPresetDialog = false }
                )
            }
            
            if (showQrSplash) {
                QrSplashScreen(
                    qrBitmap = qrBitmap,
                    onDismiss = { showQrSplash = false }
                )
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
    onSelected: (T) -> Unit,
    backgroundColor: Int,
    labelColor: ComposeColor = ComposeColor.Black
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = labelColor)
        Spacer(Modifier.height(4.dp))
        AppButton(
            text = toLabel(selected),
            backgroundColor = backgroundColor,
            onClick = { expanded = true }
        )
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

// Ensure the QR background has sufficient contrast against both pixel and eye colors.
private fun deriveReadableBackground(pixelColor: Int, eyeColor: Int): Int {
    fun srgbToLinear(x: Double): Double = if (x <= 0.03928) x / 12.92 else Math.pow((x + 0.055) / 1.055, 2.4)
    fun luminance(c: Int): Double {
        val r = srgbToLinear(Color.red(c) / 255.0)
        val g = srgbToLinear(Color.green(c) / 255.0)
        val b = srgbToLinear(Color.blue(c) / 255.0)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
    fun contrast(a: Int, b: Int): Double {
        val l1 = luminance(a)
        val l2 = luminance(b)
        val hi = maxOf(l1, l2)
        val lo = minOf(l1, l2)
        return (hi + 0.05) / (lo + 0.05)
    }
    fun blend(from: Int, to: Int, t: Double): Int {
        val r = (Color.red(from) * (1.0 - t) + Color.red(to) * t).toInt().coerceIn(0, 255)
        val g = (Color.green(from) * (1.0 - t) + Color.green(to) * t).toInt().coerceIn(0, 255)
        val b = (Color.blue(from) * (1.0 - t) + Color.blue(to) * t).toInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    val pixelLum = luminance(pixelColor)
    val primaryTarget = if (pixelLum > 0.6) Color.BLACK else Color.WHITE
    val secondaryTarget = if (primaryTarget == Color.BLACK) Color.WHITE else Color.BLACK
    val thresholds = 3.0 // WCAG-ish minimum to keep modules distinct

    fun search(target: Int): Pair<Int, Double> {
        var best = blend(pixelColor, target, 0.8)
        var bestScore = minOf(contrast(best, pixelColor), contrast(best, eyeColor))
        var t = 0.8
        while (t <= 1.0) {
            val bg = blend(pixelColor, target, t)
            val score = minOf(contrast(bg, pixelColor), contrast(bg, eyeColor))
            if (score > bestScore) { best = bg; bestScore = score }
            if (score >= thresholds) return Pair(bg, score)
            t += 0.05
        }
        return Pair(best, bestScore)
    }

    val first = search(primaryTarget)
    if (first.second >= thresholds) return first.first
    val second = search(secondaryTarget)
    return if (second.second > first.second) second.first else first.first
}

// ExportFormat removed; we only support PNG download to the gallery

@Composable
private fun BottomActionBar(
    pixelColor: Int,
    onReset: () -> Unit,
    onPreset: () -> Unit,
    onPresetLongPress: () -> Unit,
    onShuffle: () -> Unit,
    onDownloadPng: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = ComposeColor(pixelColor)
    val luminance = (0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue)
    val iconColor = if (luminance < 0.5f) ComposeColor.White else ComposeColor.Black

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 0.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            RoundedActionButton(
                pixelColor = 0xFFEF4444.toInt(),
                contentColor = ComposeColor.White,
                onClick = onReset,
                contentDescription = "Reset",
                icon = { Icon(Icons.Filled.Restore, contentDescription = null, tint = ComposeColor.White) }
            )
            PresetActionButton(
                pixelColor = pixelColor,
                contentColor = iconColor,
                onClick = onPreset,
                onLongClick = onPresetLongPress,
                contentDescription = "Preset"
            )
            RoundedActionButton(
                pixelColor = pixelColor,
                contentColor = iconColor,
                onClick = onShuffle,
                contentDescription = "Shuffle",
                icon = { Icon(Icons.Filled.Shuffle, contentDescription = null, tint = iconColor) }
            )
            RoundedActionButton(
                pixelColor = pixelColor,
                contentColor = iconColor,
                onClick = onDownloadPng,
                contentDescription = "Download",
                icon = { Icon(Icons.Filled.Download, contentDescription = null, tint = iconColor) }
            )
        }
    }
}

@Composable
private fun RoundedActionButton(
    pixelColor: Int,
    contentColor: ComposeColor,
    onClick: () -> Unit,
    contentDescription: String,
    size: Dp = 56.dp,
    icon: @Composable () -> Unit
) {
    val shape = androidx.compose.foundation.shape.CircleShape
    Box(
        modifier = Modifier
            .size(size)
            .shadow(6.dp, shape)
            .background(ComposeColor(pixelColor), shape = shape)
            .border(width = 2.dp, color = ComposeColor.Black, shape = shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
private fun PresetActionButton(
    pixelColor: Int,
    contentColor: ComposeColor,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    contentDescription: String,
    size: Dp = 56.dp
) {
    val shape = androidx.compose.foundation.shape.CircleShape
    Box(
        modifier = Modifier
            .size(size)
            .shadow(6.dp, shape)
            .background(ComposeColor(pixelColor), shape = shape)
            .border(width = 2.dp, color = ComposeColor.Black, shape = shape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Star, // Using a star icon for preset
            contentDescription = contentDescription,
            tint = contentColor
        )
    }
}

@Composable
private fun WildAuthorButton(text: String, onClick: () -> Unit, textColor: ComposeColor = ComposeColor.Black) {
    val corner = 25.dp
    Box(
        modifier = Modifier
            .wrapContentWidth()
            .height(48.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(corner))
            .background(
                color = ComposeColor(0xFFE0E0E0.toInt()).copy(alpha = 0.3f), // Translucent light gray
                shape = androidx.compose.foundation.shape.RoundedCornerShape(corner)
            )
            .border(
                width = 1.dp,
                color = ComposeColor(0xFFCECECE.toInt()).copy(alpha = 0.6f), // Translucent border
                shape = androidx.compose.foundation.shape.RoundedCornerShape(corner)
            )
            .shadow(
                elevation = 8.dp,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(corner),
                spotColor = ComposeColor(0xFFBCBCBC.toInt()).copy(alpha = 0.4f),
                ambientColor = ComposeColor(0xFFFFFFFF.toInt()).copy(alpha = 0.3f)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val display = text.trim().lowercase().replaceFirstChar { it.titlecase() }
        Text(
            text = display,
            color = textColor, // Use the passed text color parameter
            fontFamily = RequinerFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun AuthorDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {},
        title = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "About Author",
                    fontFamily = RequinerFamily,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Imaad", fontFamily = RequinerFamily, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("java easy", fontFamily = RequinerFamily)
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
                    SocialIconVector(drawableName = "ic_linkedin") { openUrl(context, "https://in.linkedin.com/in/imaadwani") }
                    SocialIconVector(drawableName = "ic_github") { openUrl(context, "https://github.com/imaad666") }
                    SocialIconVector(drawableName = "ic_twitter_x") { openUrl(context, "https://x.com/thenotoriousimi") }
                }
            }
        }
    )
}

@Composable
private fun SocialIconVector(drawableName: String, onClick: () -> Unit) {
    val context = LocalContext.current
    val resId = remember(drawableName) {
        context.resources.getIdentifier(drawableName, "drawable", context.packageName)
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(ComposeColor.Black)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (resId != 0) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = resId),
                contentDescription = drawableName,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        context.startActivity(intent)
    } catch (_: Exception) {}
}

private fun exportQrPng(
    context: Context,
    bitmap: Bitmap?,
    baseFileName: String
) : Boolean {
    if (bitmap == null) return false
    return savePng(context, bitmap, "$baseFileName.png")
}

private fun savePng(context: Context, bitmap: Bitmap, fileName: String): Boolean {
    try {
        val resolver = context.contentResolver
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/QAre")
                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            resolver.openOutputStream(it)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                values.clear()
                values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(it, values, null, null)
            } else {
                // Force media scan so image shows up in Gallery immediately on older devices
                try {
                    context.sendBroadcast(android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, it))
                } catch (_: Exception) {}
            }
            return true
        }
    } catch (_: Exception) {}
    return false
}

@Composable
private fun AppButton(
    text: String,
    backgroundColor: Int,
    onClick: () -> Unit,
    height: Dp = 52.dp
) {
    val bg = ComposeColor(backgroundColor)
    val luminance = 0.299f * bg.red + 0.587f * bg.green + 0.114f * bg.blue
    val content = if (luminance < 0.5f) ComposeColor.White else ComposeColor.Black
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(0.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(ComposeColor.Black)
            // Asymmetric insets to leave a thicker bottom border (visual)
            .padding(start = 2.dp, top = 2.dp, end = 2.dp, bottom = 4.dp)
    ) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxSize(),
            colors = ButtonDefaults.buttonColors(containerColor = bg),
            shape = shape
        ) { Text(text, color = content, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun ColorSwatch(color: Int, onClick: () -> Unit, size: Dp = 48.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(ComposeColor(color), shape = androidx.compose.foundation.shape.CircleShape)
            .clickable { onClick() }
    )
}

@Composable
private fun ColorSetting(label: String, currentColor: Int, onColorChange: (Int) -> Unit, labelColor: ComposeColor = ComposeColor.Black) {
    var dialogOpen by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = labelColor)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ColorSwatch(color = currentColor, onClick = { dialogOpen = true }, size = 36.dp)
            AppButton(text = "Pick Color", backgroundColor = currentColor, onClick = { dialogOpen = true })
        }
    }

    if (dialogOpen) {
        ColorPickerDialog(
            initialColor = currentColor,
            onDismiss = { dialogOpen = false },
            onConfirm = { new -> onColorChange(new); dialogOpen = false }
        )
    }
}

@Composable
private fun ColorPickerDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val hsv = remember {
        FloatArray(3).also { arr ->
            Color.colorToHSV(initialColor, arr)
        }
    }
    var hue by remember { mutableStateOf(hsv[0]) }
    var sat by remember { mutableStateOf(hsv[1]) }
    var value by remember { mutableStateOf(hsv[2]) }
    var workingColor by remember { mutableStateOf(Color.HSVToColor(floatArrayOf(hue, sat, value))) }
    var hexField by remember { mutableStateOf(TextFieldValue(String.format("#%06X", 0xFFFFFF and workingColor))) }
    // Transparent option removed per request

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(workingColor) }) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Select a Color", style = MaterialTheme.typography.titleMedium.copy(fontFamily = RequinerFamily)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 2D SV palette for chosen hue
                val hueColor = ComposeColor(Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                val paletteModifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .drawWithCache {
                        val horiz = Brush.horizontalGradient(listOf(ComposeColor.White, hueColor))
                        val vert = Brush.verticalGradient(listOf(ComposeColor.Transparent, ComposeColor.Black))
                        onDrawBehind {
                            drawRect(horiz)
                            drawRect(vert)
                        }
                    }
                    .pointerInput(hue) {
                        detectTapGestures { offset ->
                            val w = this.size.width.toFloat().coerceAtLeast(1f)
                            val h = this.size.height.toFloat().coerceAtLeast(1f)
                            sat = (offset.x / w).coerceIn(0f, 1f)
                            value = (1f - (offset.y / h)).coerceIn(0f, 1f)
                            workingColor = Color.HSVToColor(floatArrayOf(hue, sat, value))
                            hexField = TextFieldValue(String.format("#%06X", 0xFFFFFF and workingColor))
                            
                        }
                    }
                    .pointerInput(hue) {
                        detectDragGestures { change, _ ->
                            val w = this.size.width.toFloat().coerceAtLeast(1f)
                            val h = this.size.height.toFloat().coerceAtLeast(1f)
                            val x = change.position.x.coerceIn(0f, w)
                            val y = change.position.y.coerceIn(0f, h)
                            sat = (x / w).coerceIn(0f, 1f)
                            value = (1f - (y / h)).coerceIn(0f, 1f)
                            workingColor = Color.HSVToColor(floatArrayOf(hue, sat, value))
                            hexField = TextFieldValue(String.format("#%06X", 0xFFFFFF and workingColor))
                            
                        }
                    }

                Box(modifier = paletteModifier)

                Spacer(Modifier.height(16.dp))

                // Hue slider
                Text("Hue")
                Slider(
                    value = hue,
                    onValueChange = {
                        hue = it
                        workingColor = Color.HSVToColor(floatArrayOf(hue, sat, value))
                        hexField = TextFieldValue(String.format("#%06X", 0xFFFFFF and workingColor))
                    },
                    valueRange = 0f..360f
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = hexField,
                        onValueChange = {
                            hexField = it
                            val text = it.text.trim()
                            if (text.isNotEmpty()) {
                                val parsedColor = if (text.startsWith("#")) {
                                    val cleaned = text.removePrefix("#")
                                    cleaned.toLongOrNull(16)?.toInt()?.let { v -> 0xFF000000.toInt() or v }
                                } else {
                                    text.toLongOrNull()?.toInt()
                                }
                                parsedColor?.let { pc ->
                                    workingColor = pc
                                    val arr = FloatArray(3)
                                    Color.colorToHSV(workingColor, arr)
                                    hue = arr[0]; sat = arr[1]; value = arr[2]
                                }
                            }
                        },
                        label = { Text("HEX or Decimal") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(ComposeColor(workingColor), shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Preset swatches in dialog
                val presets = listOf(
                    0xFF3B82F6.toInt(), // blue
                    0xFFEF4444.toInt(), // red
                    0xFFF59E0B.toInt(), // amber
                    0xFFFACC15.toInt(), // yellow
                    0xFF22C55E.toInt(), // green
                    0xFF06B6D4.toInt(), // cyan
                    0xFF8B5CF6.toInt(), // violet
                    0xFF000000.toInt(), // black
                    0xFFFFFFFF.toInt()  // white
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    presets.forEach { c ->
                        ColorSwatch(color = c, onClick = {
                            workingColor = c
                            val arr = FloatArray(3)
                            Color.colorToHSV(c, arr)
                            hue = arr[0]; sat = arr[1]; value = arr[2]
                            hexField = TextFieldValue(String.format("#%06X", 0xFFFFFF and workingColor))
                        }, size = 32.dp)
                    }
                }

                Spacer(Modifier.height(12.dp))

            }
        }
    )
}

@Composable
private fun PresetEditDialog(
    currentPixelColor: Int,
    currentEyeColor: Int,
    currentPixelStyle: PixelStyle,
    currentEyeOutlineStyle: EyeOutlineStyle,
    currentPupilStyle: PupilStyle,
    onSave: (Int, Int, PixelStyle, EyeOutlineStyle, PupilStyle) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { 
                onSave(currentPixelColor, currentEyeColor, currentPixelStyle, currentEyeOutlineStyle, currentPupilStyle)
            }) { 
                Text("Save Preset", color = ComposeColor(0xFF22C55E.toInt())) 
            }
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text("Cancel", color = ComposeColor(0xFFEF4444.toInt())) 
            }
        },
        title = {
            Text(
                "Save Current Style as Preset",
                fontFamily = RequinerFamily,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "This will save your current customization as a preset.",
                    fontFamily = RequinerFamily,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Tap the star button to apply the preset instantly!",
                    fontFamily = RequinerFamily,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = ComposeColor.Gray
                )
            }
        }
    )
}

@Composable
private fun QrSplashScreen(
    qrBitmap: Bitmap?,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black)
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        qrBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Full Screen QR",
                modifier = Modifier.fillMaxSize(0.9f)
            )
        }
    }
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
