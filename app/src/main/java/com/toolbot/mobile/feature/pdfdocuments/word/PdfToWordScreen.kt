package com.toolbot.mobile.feature.pdfdocuments.word

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
fun PdfToWordScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedPdf by remember { mutableStateOf<Uri?>(null) }
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
            return@rememberLauncherForActivityResult
        }

        selectedPdf = uri
        statusMessage = "Selected: ${PdfDocumentUtils.getDisplayName(uri, context.contentResolver)}"
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    ) { outputUri ->
        if (outputUri == null) {
            statusMessage = "Word export cancelled."
            isProcessing = false
            return@rememberLauncherForActivityResult
        }

        val inputUri = selectedPdf ?: run {
            isProcessing = false
            statusMessage = "Please select a PDF first."
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                convertPdfToWord(inputUri, outputUri, context.contentResolver)
            }
            isProcessing = false
            if (result.isSuccess) {
                statusMessage = "Word document created successfully!"
                delay(1200)
                onBack()
            } else {
                statusMessage = result.exceptionOrNull()?.message ?: "Unable to convert the PDF to Word."
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("PDF to Word") },
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
                text = "Convert a PDF into a Word document while preserving the text content and basic page structure.",
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
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = PdfDocumentUtils.getDisplayName(selectedPdf!!, context.contentResolver),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Ready to export as .docx",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (selectedPdf == null) {
                        statusMessage = "Please select a PDF file first."
                        return@Button
                    }
                    isProcessing = true
                    statusMessage = "Extracting text and creating Word document..."
                    saveLauncher.launch("converted_document.docx")
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedPdf != null && !isProcessing,
            ) {
                Text("Convert to Word")
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
                    statusMessage = "Selection cleared."
                },
            ) {
                Text("Clear")
            }
        }
    }
}
