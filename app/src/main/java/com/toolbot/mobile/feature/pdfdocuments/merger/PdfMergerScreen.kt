package com.toolbot.mobile.feature.pdfdocuments.merger

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
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
fun PdfMergerScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var statusMessage by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val pickFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isEmpty()) {
            statusMessage = "No PDF files were selected."
            return@rememberLauncherForActivityResult
        }

        val pdfUris = uris.filter { PdfDocumentUtils.isPdfUri(it, context.contentResolver) }
        if (pdfUris.isEmpty()) {
            statusMessage = "Please select valid PDF files."
            return@rememberLauncherForActivityResult
        }

        val updatedUris = selectedUris + pdfUris.filterNot { selectedUris.contains(it) }
        val newlyAddedCount = updatedUris.size - selectedUris.size
        selectedUris = updatedUris
        statusMessage = if (newlyAddedCount > 0) {
            "$newlyAddedCount PDF file(s) added. Total selected: ${selectedUris.size}."
        } else {
            "All selected PDF files were already in the list."
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
    ) { outputUri ->
        if (outputUri == null) {
            statusMessage = "Merge cancelled."
            isProcessing = false
            return@rememberLauncherForActivityResult
        }

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                mergePdfFiles(selectedUris, outputUri, context.contentResolver)
            }
            isProcessing = false
            if (result.isSuccess) {
                statusMessage = "PDFs merged successfully!"
                delay(1200)
                onBack()
            } else {
                result.exceptionOrNull()?.message ?: "Unable to merge the selected PDFs."
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("PDF Merger") },
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
                text = "Combine multiple PDF files into one document in the selected order.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = { pickFilesLauncher.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing,
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add PDF Files")
            }

            if (selectedUris.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Selected files (${selectedUris.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (selectedUris.size > 4) 220.dp else (selectedUris.size * 52).dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            itemsIndexed(selectedUris) { index, uri ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "${index + 1}. ${PdfDocumentUtils.getDisplayName(uri, context.contentResolver)}",
                                        modifier = Modifier.weight(1f),
                                    )
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                selectedUris = selectedUris.toMutableList().apply {
                                                    val current = removeAt(index)
                                                    add(index - 1, current)
                                                }
                                            }
                                        },
                                        enabled = index > 0,
                                    ) {
                                        Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "Move up")
                                    }
                                    IconButton(
                                        onClick = {
                                            if (index < selectedUris.size - 1) {
                                                selectedUris = selectedUris.toMutableList().apply {
                                                    val current = removeAt(index)
                                                    add(index + 1, current)
                                                }
                                            }
                                        },
                                        enabled = index < selectedUris.size - 1,
                                    ) {
                                        Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "Move down")
                                    }
                                    IconButton(
                                        onClick = {
                                            selectedUris = selectedUris.filterNot { it == uri }
                                        },
                                    ) {
                                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Remove file")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (selectedUris.size < 2) {
                        statusMessage = "Please select at least 2 PDF files to merge."
                        return@Button
                    }
                    isProcessing = true
                    statusMessage = "Preparing merged PDF..."
                    saveLauncher.launch("merged_document.pdf")
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing && selectedUris.size >= 2,
            ) {
                Text("Merge PDFs")
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
                    selectedUris = emptyList()
                    statusMessage = "Selection cleared."
                },
                modifier = Modifier.align(Alignment.Start),
            ) {
                Text("Clear")
            }
        }
    }
}
