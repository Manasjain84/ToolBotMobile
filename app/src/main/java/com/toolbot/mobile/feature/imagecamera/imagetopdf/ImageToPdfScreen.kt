package com.toolbot.mobile.feature.imagecamera.imagetopdf

import android.graphics.pdf.PdfDocument
import android.graphics.Canvas
import android.graphics.Rect
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
fun ImageToPdfScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("") }
    var outputSize by remember { mutableStateOf<Long?>(null) }

    val pickLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            status = "No image selected"
            selectedUri = null
            return@rememberLauncherForActivityResult
        }
        selectedUri = uri
        status = "Selected: ${ImageUtils.getFileName(context, uri)}"
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { outUri ->
        if (outUri == null) {
            status = "Save cancelled"
            return@rememberLauncherForActivityResult
        }
        val input = selectedUri ?: run { status = "Select an image first"; return@rememberLauncherForActivityResult }
        scope.launch {
            status = "Converting to PDF..."
            val result = withContext(Dispatchers.IO) {
                try {
                    val bmp = ImageUtils.loadBitmapSafely(context, input, maxSide = 2048) ?: return@withContext false
                    val pdf = PdfDocument()
                    val pageInfo = PdfDocument.PageInfo.Builder(bmp.width, bmp.height, 1).create()
                    val page = pdf.startPage(pageInfo)
                    val canvas: Canvas = page.canvas
                    val dest = Rect(0, 0, bmp.width, bmp.height)
                    canvas.drawBitmap(bmp, null, dest, null)
                    pdf.finishPage(page)
                    context.contentResolver.openOutputStream(outUri)?.use { os -> pdf.writeTo(os) }
                    pdf.close()
                    true
                } catch (e: Exception) {
                    false
                }
            }
            outputSize = if (result) ImageUtils.getFileSize(context, outUri) else null
            status = if (result) "PDF created successfully" else "Failed to create PDF"
        }
    }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Image → PDF") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(imageVector = Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
        })
    }) { inner ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(inner)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Convert a single image into a one-page PDF.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { pickLauncher.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Select Image") }

            selectedUri?.let { uri ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(ImageUtils.getFileName(context, uri), fontWeight = FontWeight.SemiBold)
                        val dims = ImageUtils.getBitmapDimensions(context, uri)
                        if (dims.first > 0) Text("${dims.first} × ${dims.second}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Button(onClick = { saveLauncher.launch(ImageUtils.getFileName(context, uri).substringBeforeLast('.') + ".pdf") }, modifier = Modifier.fillMaxWidth()) { Text("Save as PDF") }
            }

            if (status.isNotBlank()) Text(status)
        }
    }
}
