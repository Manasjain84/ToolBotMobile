package com.toolbot.mobile.feature.aitext.grammar

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrammarImproverScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var inputText by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf<GrammarResult?>(null) }
    var status by rememberSaveable { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val improver = remember { GrammarImprover() }

    fun clearAll() {
        inputText = ""
        result = null
        status = null
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Grammar Checker / Text Improver") },
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
                text = "Lightweight offline fix-ups for repeated words, punctuation, capitalization, and common grammar mistakes.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Text to improve") },
                minLines = 8,
                maxLines = 18,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (inputText.isBlank()) {
                            status = "Add text before running the grammar checks."
                            return@Button
                        }
                        isLoading = true
                        status = null
                        coroutineScope.launch {
                            val finalResult = withContext(Dispatchers.Default) {
                                improver.improve(inputText)
                            }
                            isLoading = false
                            result = finalResult
                            status = if (finalResult.correctionCount > 0) "${finalResult.correctionCount} improvement(s) found" else "No changes needed"
                        }
                    },
                    enabled = !isLoading,
                ) {
                    Text(if (isLoading) "Checking..." else "Improve text")
                }
                OutlinedButton(onClick = { clearAll() }) {
                    Text("Clear")
                }
            }

            if (status != null) {
                Text(
                    text = status.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (status!!.contains("No changes")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            result?.let { grammarResult ->
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
                                text = "Improved text",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(grammarResult.improvedText))
                            }) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy improved text")
                            }
                        }

                        Text(
                            text = grammarResult.improvedText.ifBlank { "No output generated." },
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        Text(
                            text = "Corrections: ${grammarResult.correctionCount}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        if (grammarResult.changes.isNotEmpty()) {
                            Text(
                                text = "Detected changes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                grammarResult.changes.forEach { change ->
                                    Text(
                                        text = "• $change",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
