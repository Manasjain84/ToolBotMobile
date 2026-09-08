package com.toolbot.mobile.feature.imagecamera.converter

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
fun ImageFormatConverterScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("") }
    var chosenFormat by remember { mutableStateOf("jpg") }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) { selectedUri = null; status = "No image selected"; return@rememberLauncherForActivityResult }
        selectedUri = uri
        status = "Selected: ${ImageUtils.getFileName(context, uri)}"
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/*")) { outUri ->
        if (outUri == null) { status = "Save cancelled"; return@rememberLauncherForActivityResult }
        val input = selectedUri ?: run { status = "Choose an image first"; return@rememberLauncherForActivityResult }
        scope.launch {
            status = "Converting..."
            val result = withContext(Dispatchers.IO) {
                val bmp = ImageUtils.loadBitmapSafely(context, input, maxSide = 4096) ?: return@withContext false
                val format = when (chosenFormat) {
                    "png" -> Bitmap.CompressFormat.PNG
                    "webp" -> Bitmap.CompressFormat.WEBP
                    else -> Bitmap.CompressFormat.JPEG
                }
                ImageUtils.compressAndSave(context, bmp, outUri, format, 90)
            }
            status = if (result) "Converted and saved" else "Conversion failed"
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Image Format Converter") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
        })
    }) { inner ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(inner)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Convert between JPG, PNG and WebP. Transparency is preserved for PNG/WebP. PNG -> JPG will composite on white.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { pickLauncher.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Select Image") }

            selectedUri?.let { uri ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(ImageUtils.getFileName(context, uri), fontWeight = FontWeight.SemiBold)
                        val dims = ImageUtils.getBitmapDimensions(context, uri)
                        if (dims.first > 0) Text("${dims.first} × ${dims.second}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(selected = chosenFormat=="jpg", onClick = { chosenFormat = "jpg" }, label = { Text("JPG") })
                    FilterChip(selected = chosenFormat=="png", onClick = { chosenFormat = "png" }, label = { Text("PNG") })
                    FilterChip(selected = chosenFormat=="webp", onClick = { chosenFormat = "webp" }, label = { Text("WebP") })
                }

                Button(onClick = {
                    val suggested = ImageUtils.getFileName(context, uri).substringBeforeLast('.') + ".${chosenFormat}"
                    saveLauncher.launch(suggested)
                }, modifier = Modifier.fillMaxWidth()) { Text("Convert & Save") }
            }

            if (status.isNotBlank()) Text(status)
        }
    }
}
