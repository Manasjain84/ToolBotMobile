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
fun WordToPdfScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedDocx by remember { mutableStateOf<Uri?>(null) }
    var statusMessage by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val pickDocxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) {
            statusMessage = "No Word document selected."
            return@rememberLauncherForActivityResult
        }

        if (!isDocxUri(uri, context.contentResolver)) {
            statusMessage = "Please select a valid .docx Word file."
            selectedDocx = null
            return@rememberLauncherForActivityResult
        }

        selectedDocx = uri
        statusMessage = "Selected: ${PdfDocumentUtils.getDisplayName(uri, context.contentResolver)}"
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { outputUri ->
        if (outputUri == null) {
            statusMessage = "PDF export cancelled."
            isProcessing = false
            return@rememberLauncherForActivityResult
        }

        val inputUri = selectedDocx ?: run {
            isProcessing = false
            statusMessage = "Please select a Word document first."
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                convertDocxToPdf(inputUri, outputUri, context.contentResolver)
            }
            isProcessing = false
            if (result.isSuccess) {
                statusMessage = "PDF created successfully!"
                delay(1200)
                onBack()
            } else {
                statusMessage = result.exceptionOrNull()?.message ?: "Unable to convert the DOCX file to PDF."
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Word to PDF") },
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
                text = "Convert a DOCX Word file into a locally generated PDF while keeping the text readable and paginated.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = {
                    pickDocxLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing,
            ) {
                Text("Select Word Document")
            }

            if (selectedDocx != null) {
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
                            text = PdfDocumentUtils.getDisplayName(selectedDocx!!, context.contentResolver),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Ready to convert to PDF",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (selectedDocx == null) {
                        statusMessage = "Please select a Word document first."
                        return@Button
                    }
                    isProcessing = true
                    statusMessage = "Extracting text and creating PDF..."
                    saveLauncher.launch("converted_document.pdf")
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedDocx != null && !isProcessing,
            ) {
                Text("Convert to PDF")
            }

            if (statusMessage.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (statusMessage.contains("successfully", ignoreCase = true) || statusMessage.contains("Ready", ignoreCase = true)) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else if (statusMessage.contains("cancelled", ignoreCase = true) || statusMessage.contains("Please", ignoreCase = true) || statusMessage.contains("Unable", ignoreCase = true)) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        },
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
                    selectedDocx = null
                    statusMessage = "Selection cleared."
                },
            ) {
                Text("Clear")
            }
        }
    }
}
