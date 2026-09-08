package com.toolbot.mobile.feature.pdfdocuments.compressor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.toolbot.mobile.feature.pdfdocuments.common.PdfDocumentUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfCompressorScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedPdf by remember { mutableStateOf<Uri?>(null) }
    var statusMessage by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var compressionLevel by remember { mutableStateOf(CompressionLevel.MEDIUM) }
    var originalSize by remember { mutableStateOf<Long?>(null) }
    var compressedSize by remember { mutableStateOf<Long?>(null) }

    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            statusMessage = "No PDF selected."
            return@rememberLauncherForActivityResult
        }

        if (!PdfDocumentUtils.isPdfUri(uri, context.contentResolver)) {
            statusMessage = "Please select a valid PDF file."
            selectedPdf = null
            originalSize = null
            compressedSize = null
            return@rememberLauncherForActivityResult
        }

        selectedPdf = uri
        originalSize = PdfDocumentUtils.getFileSize(uri, context.contentResolver)
        compressedSize = null
        statusMessage = "Selected: ${PdfDocumentUtils.getDisplayName(uri, context.contentResolver)}"
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { outputUri ->
        if (outputUri == null) {
            statusMessage = "Compression cancelled."
            isProcessing = false
            return@rememberLauncherForActivityResult
        }

        val inputUri = selectedPdf ?: run {
            isProcessing = false
            statusMessage = "Please select a PDF before compressing."
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                compressPdfFile(inputUri, outputUri, compressionLevel, context.contentResolver)
            }
            isProcessing = false

            val savedSize = PdfDocumentUtils.getFileSize(outputUri, context.contentResolver)
            compressedSize = savedSize

            if (result.isSuccess) {
                statusMessage = "PDF compressed successfully!"
                delay(1200)
                onBack()
            } else {
                statusMessage = result.exceptionOrNull()?.message ?: "Unable to compress the PDF."
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("PDF Compressor") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Reduce PDF file size using a best-effort compression level.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = { pickPdfLauncher.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing,
            ) {
                Text("Select PDF")
            }

            if (selectedPdf != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = PdfDocumentUtils.getDisplayName(selectedPdf!!, context.contentResolver),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (originalSize != null && originalSize!! > 0L) {
                            Text(
                                text = "Original size: ${originalSize!! / 1024} KB",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (compressedSize != null && compressedSize!! > 0L) {
                            Text(
                                text = "Compressed size: ${compressedSize!! / 1024} KB",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Text(
                text = "Compression level",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CompressionLevel.entries.forEach { level ->
                    FilterChip(
                        selected = compressionLevel == level,
                        onClick = { compressionLevel = level },
                        label = { Text(level.label) },
                    )
                }
            }

            Button(
                onClick = {
                    if (selectedPdf == null) {
                        statusMessage = "Please select a PDF file first."
                        return@Button
                    }
                    isProcessing = true
                    statusMessage = "Compressing PDF..."
                    saveLauncher.launch("compressed_document.pdf")
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedPdf != null && !isProcessing,
            ) {
                Text("Compress PDF")
            }

            if (statusMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                    ),
                ) {
                    Text(
                        text = statusMessage,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            TextButton(
                onClick = {
                    selectedPdf = null
                    originalSize = null
                    compressedSize = null
                    statusMessage = "Selection cleared."
                },
            ) {
                Text("Clear")
            }
        }
    }
}
