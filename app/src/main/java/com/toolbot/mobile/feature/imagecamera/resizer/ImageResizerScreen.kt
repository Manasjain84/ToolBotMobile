package com.toolbot.mobile.feature.imagecamera.resizer

import android.graphics.Bitmap
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
import androidx.compose.ui.Alignment
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
fun ImageResizerScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("") }
    var originalDims by remember { mutableStateOf(Pair(0, 0)) }
    var originalSize by remember { mutableStateOf<Long?>(null) }

    var width by remember { mutableStateOf(0) }
    var height by remember { mutableStateOf(0) }
    var keepAspect by remember { mutableStateOf(true) }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            selectedUri = null
            status = "No image selected"
            return@rememberLauncherForActivityResult
        }
        selectedUri = uri
        originalDims = ImageUtils.getBitmapDimensions(context, uri)
        originalSize = ImageUtils.getFileSize(context, uri)
        width = originalDims.first
        height = originalDims.second
        status = "Selected: ${ImageUtils.getFileName(context, uri)}"
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/*")) { outUri ->
        if (outUri == null) { status = "Save cancelled"; return@rememberLauncherForActivityResult }
        val input = selectedUri ?: run { status = "Select an image first"; return@rememberLauncherForActivityResult }
        scope.launch {
            status = "Resizing..."
            val result = withContext(Dispatchers.IO) {
                val bmp = ImageUtils.loadBitmapSafely(context, input, maxSide = 4096) ?: return@withContext false
                // clamp dimensions
                val tw = width.coerceAtLeast(1)
                val th = height.coerceAtLeast(1)
                val resized = if (bmp.width == tw && bmp.height == th) bmp else ImageUtils.resizeBitmap(bmp, tw, th)
                val mime = context.contentResolver.getType(input) ?: "image/jpeg"
                val format = when {
                    mime.contains("png") -> Bitmap.CompressFormat.PNG
                    mime.contains("webp") -> Bitmap.CompressFormat.WEBP
                    else -> Bitmap.CompressFormat.JPEG
                }
                ImageUtils.compressAndSave(context, resized, outUri, format, 90)
            }
            status = if (result) "Image resized and saved" else "Resize failed"
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Image Resizer") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
        })
    }) { inner ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(inner)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Select an image and choose new dimensions. Keep aspect ratio to auto-calculate one side.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { pickLauncher.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Select Image") }

            selectedUri?.let { uri ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(ImageUtils.getFileName(context, uri), fontWeight = FontWeight.SemiBold)
                        if (originalDims.first > 0) Text("Original: ${originalDims.first} × ${originalDims.second}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        originalSize?.let { if (it > 0) Text("Original size: ${it / 1024} KB", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = width.takeIf { it>0 }?.toString() ?: "", onValueChange = {
                        val v = it.toIntOrNull() ?: 0
                        width = v.coerceAtLeast(0)
                        if (keepAspect && originalDims.first>0 && originalDims.second>0 && v>0) {
                            height = (v.toFloat() * originalDims.second / originalDims.first).toInt().coerceAtLeast(1)
                        }
                    }, label = { Text("Width") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = height.takeIf { it>0 }?.toString() ?: "", onValueChange = {
                        val v = it.toIntOrNull() ?: 0
                        height = v.coerceAtLeast(0)
                        if (keepAspect && originalDims.first>0 && originalDims.second>0 && v>0) {
                            width = (v.toFloat() * originalDims.first / originalDims.second).toInt().coerceAtLeast(1)
                        }
                    }, label = { Text("Height") }, modifier = Modifier.weight(1f))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Maintain aspect ratio", modifier = Modifier.weight(1f))
                    Switch(checked = keepAspect, onCheckedChange = { keepAspect = it })
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = {
                        if (originalDims.first>0) {
                            width = (originalDims.first * 0.25).toInt().coerceAtLeast(1)
                            if (keepAspect) height = (width.toFloat() * originalDims.second / originalDims.first).toInt().coerceAtLeast(1)
                        }
                    }) { Text("25%") }
                    Button(onClick = {
                        if (originalDims.first>0) {
                            width = (originalDims.first * 0.5).toInt().coerceAtLeast(1)
                            if (keepAspect) height = (width.toFloat() * originalDims.second / originalDims.first).toInt().coerceAtLeast(1)
                        }
                    }) { Text("50%") }
                    Button(onClick = {
                        if (originalDims.first>0) {
                            width = (originalDims.first * 0.75).toInt().coerceAtLeast(1)
                            if (keepAspect) height = (width.toFloat() * originalDims.second / originalDims.first).toInt().coerceAtLeast(1)
                        }
                    }) { Text("75%") }
                }

                Button(onClick = {
                    // validation
                    if (width <= 0 || height <= 0) { status = "Dimensions must be > 0"; return@Button }
                    if (width > 10000 || height > 10000) { status = "Dimensions too large"; return@Button }
                    val suggested = ImageUtils.getFileName(context, uri).substringBeforeLast('.') + "_resized.jpg"
                    saveLauncher.launch(suggested)
                }, modifier = Modifier.fillMaxWidth()) { Text("Resize & Save") }
            }

            if (status.isNotBlank()) Text(status)
        }
    }
}
