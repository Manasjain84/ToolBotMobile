package com.toolbot.mobile.feature.imagecamera.compressor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbot.mobile.feature.imagecamera.common.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCompressorScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("") }
    var quality by remember { mutableStateOf(85) }
    var originalSize by remember { mutableStateOf<Long?>(null) }
    var compressedSize by remember { mutableStateOf<Long?>(null) }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            status = "No image selected."
            selectedUri = null
            originalSize = null
            return@rememberLauncherForActivityResult
        }
        selectedUri = uri
        originalSize = ImageUtils.getFileSize(context, uri)
        compressedSize = null
        status = "Selected: ${ImageUtils.getFileName(context, uri)}"
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/*")) { outUri ->
        if (outUri == null) {
            status = "Save cancelled"
            return@rememberLauncherForActivityResult
        }
        val input = selectedUri ?: run {
            status = "Please choose an image first"
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            status = "Compressing..."
            val result = withContext(Dispatchers.IO) {
                val bitmap = ImageUtils.loadBitmapSafely(context, input, maxSide = 2048)
                if (bitmap == null) return@withContext false
                // try to preserve format where possible; default to JPEG
                val mime = context.contentResolver.getType(input) ?: "image/jpeg"
                val format = when {
                    mime.contains("png") -> Bitmap.CompressFormat.PNG
                    mime.contains("webp") -> Bitmap.CompressFormat.WEBP
                    else -> Bitmap.CompressFormat.JPEG
                }
                ImageUtils.compressAndSave(context, bitmap, outUri, format, quality)
            }
            compressedSize = ImageUtils.getFileSize(context, outUri)
            status = if (result) "Image compressed successfully" else "Compression failed"
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Image Compressor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Select an image to reduce file size. Recommended quality >= 60.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { pickLauncher.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Select Image") }

            if (selectedUri != null) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(ImageUtils.getFileName(context, selectedUri!!), fontWeight = FontWeight.SemiBold)
                        originalSize?.let { if (it > 0) Text("Original size: ${it / 1024} KB", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        compressedSize?.let { if (it > 0) Text("Saved size: ${it / 1024} KB", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }

                Text("Quality: $quality", style = MaterialTheme.typography.bodyMedium)
                Slider(value = quality.toFloat(), onValueChange = { quality = it.toInt() }, valueRange = 10f..100f, steps = 8)

                Button(onClick = {
                    val suggested = ImageUtils.getFileName(context, selectedUri!!).substringBeforeLast('.') + "_compressed.jpg"
                    saveLauncher.launch(suggested)
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Save Compressed Image")
                }
            }

            if (status.isNotBlank()) {
                Text(status, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
