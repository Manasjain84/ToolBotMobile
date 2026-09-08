package com.toolbot.mobile.feature.pdfdocuments.splitter

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
fun PdfSplitterScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedPdf by remember { mutableStateOf<Uri?>(null) }
    var pageCount by remember { mutableStateOf<Int?>(null) }
    var startPage by remember { mutableStateOf("1") }
    var endPage by remember { mutableStateOf("1") }
    var statusMessage by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

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
            pageCount = null
            return@rememberLauncherForActivityResult
        }

        selectedPdf = uri
        pageCount = null
        statusMessage = "Reading PDF details..."

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                getPdfPageCount(uri, context.contentResolver)
            }
            if (result.isSuccess) {
                pageCount = result.getOrNull()
                startPage = "1"
                endPage = pageCount?.toString() ?: "1"
                statusMessage = "PDF selected: ${PdfDocumentUtils.getDisplayName(uri, context.contentResolver)}"
            } else {
                selectedPdf = null
                pageCount = null
                statusMessage = result.exceptionOrNull()?.message ?: "The selected file could not be read as a PDF."
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { outputUri ->
        if (outputUri == null) {
            statusMessage = "Split cancelled."
            isProcessing = false
            return@rememberLauncherForActivityResult
        }

        val inputUri = selectedPdf ?: run {
            isProcessing = false
            statusMessage = "Please select a PDF first."
            return@rememberLauncherForActivityResult
        }

        val parsedStart = startPage.toIntOrNull()
        val parsedEnd = endPage.toIntOrNull()
        if (parsedStart == null || parsedEnd == null || parsedStart < 1 || parsedEnd < parsedStart || pageCount == null || parsedEnd > pageCount!!) {
            statusMessage = "Invalid page range. Please review the start and end page values."
            isProcessing = false
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                splitPdfRange(inputUri, parsedStart, parsedEnd, outputUri, context.contentResolver)
            }
            isProcessing = false
            if (result.isSuccess) {
                statusMessage = "PDF split successfully!"
                delay(1200)
                onBack()
            } else {
                statusMessage = result.exceptionOrNull()?.message ?: "Unable to split the PDF."
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("PDF Splitter") },
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
                text = "Extract a page range from a PDF and save it as a new document.",
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
                            text = selectedPdf?.let { PdfDocumentUtils.getDisplayName(it, context.contentResolver) } ?: "No PDF selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (pageCount != null) "Total pages: $pageCount" else "Reading page count...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = startPage,
                    onValueChange = { startPage = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Start page") },
                    singleLine = true,
                    enabled = selectedPdf != null && !isProcessing,
                )
                OutlinedTextField(
                    value = endPage,
                    onValueChange = { endPage = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("End page") },
                    singleLine = true,
                    enabled = selectedPdf != null && !isProcessing,
                )
            }

            Button(
                onClick = {
                    if (selectedPdf == null) {
                        statusMessage = "Please select a PDF first."
                        return@Button
                    }
                    val parsedStart = startPage.toIntOrNull()
                    val parsedEnd = endPage.toIntOrNull()
                    if (parsedStart == null || parsedEnd == null) {
                        statusMessage = "Please enter valid page numbers."
                        return@Button
                    }
                    if (parsedStart < 1 || parsedEnd < parsedStart) {
                        statusMessage = "Start page must be at least 1 and end page must be greater than or equal to the start page."
                        return@Button
                    }
                    if (pageCount == null || parsedEnd > pageCount!!) {
                        statusMessage = "End page must not exceed the total page count."
                        return@Button
                    }
                    isProcessing = true
                    statusMessage = "Preparing split output..."
                    saveLauncher.launch("split_document.pdf")
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedPdf != null && !isProcessing,
            ) {
                Text("Split PDF")
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
                    pageCount = null
                    startPage = "1"
                    endPage = "1"
                    statusMessage = "Selection cleared."
                },
                modifier = Modifier,
            ) {
                Text("Clear")
            }
        }
    }
}
