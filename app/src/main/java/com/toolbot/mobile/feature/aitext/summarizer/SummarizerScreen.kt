package com.toolbot.mobile.feature.aitext.summarizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalClipboardManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTextSummarizerScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var inputText by rememberSaveable { mutableStateOf("") }
    var selectedLength by rememberSaveable { mutableStateOf(SummaryLength.MEDIUM) }
    var result by rememberSaveable { mutableStateOf<SummaryResult?>(null) }
    var status by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val summarizer = remember { TextSummarizerEngine() }

    fun clearAll() {
        inputText = ""
        result = null
        status = null
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("AI Text Summarizer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Create a concise summary from any student text or notes.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryLength.entries.forEach { length ->
                    FilterChip(
                        selected = selectedLength == length,
                        onClick = { selectedLength = length },
                        label = { Text(length.label) },
                    )
                }
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Input text") },
                minLines = 8,
                maxLines = 16,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (inputText.isBlank()) {
                            status = "Add some text before summarizing."
                            return@Button
                        }
                        isLoading = true
                        status = null
                        coroutineScope.launch {
                            val finalResult = withContext(Dispatchers.Default) {
                                try {
                                    summarizer.summarize(inputText, selectedLength)
                                } catch (error: IllegalArgumentException) {
                                    status = error.message ?: "Could not summarize this input."
                                    null
                                }
                            }
                            isLoading = false
                            if (finalResult != null) {
                                result = finalResult
                                status = "Summary ready"
                            }
                        }
                    },
                    enabled = !isLoading,
                ) {
                    Text(if (isLoading) "Summarizing..." else "Summarize")
                }
                OutlinedButton(onClick = { clearAll() }) {
                    Text("Clear")
                }
            }

            if (status != null) {
                Text(
                    text = status.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (status == "Summary ready") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }

            result?.let { summaryResult ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(summaryResult.summary))
                            }) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy summary")
                            }
                        }

                        Text(
                            text = summaryResult.summary,
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        Text(
                            text = "Original words: ${summaryResult.originalWordCount} • Summary words: ${summaryResult.summaryWordCount} • Reduction: ${summaryResult.reductionPercent}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
