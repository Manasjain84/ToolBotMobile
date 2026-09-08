package com.toolbot.mobile.feature.aitext.translator

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
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
fun TextTranslatorScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
) {
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val languages = OfflineTranslator.supportedLanguages
    var sourceLanguage by rememberSaveable { mutableStateOf("en") }
    var targetLanguage by rememberSaveable { mutableStateOf("hi") }
    var inputText by rememberSaveable { mutableStateOf("") }
    var outputText by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var sourceExpanded by remember { mutableStateOf(false) }
    var targetExpanded by remember { mutableStateOf(false) }
    var status by rememberSaveable { mutableStateOf<String?>(null) }

    fun updateLanguagePair(source: String, target: String) {
        sourceLanguage = source
        targetLanguage = target
        if (source == target) {
            targetLanguage = if (source == "en") "hi" else "en"
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Text Translator") },
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
                text = "Offline local translator for the supported pair: English ↔ Hindi.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ExposedDropdownMenuBox(
                    expanded = sourceExpanded,
                    onExpandedChange = { sourceExpanded = !sourceExpanded },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = languages.first { it.code == sourceLanguage }.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("From") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = sourceExpanded,
                        onDismissRequest = { sourceExpanded = false },
                    ) {
                        languages.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(language.label) },
                                onClick = {
                                    sourceLanguage = language.code
                                    if (sourceLanguage == targetLanguage) {
                                        targetLanguage = if (language.code == "en") "hi" else "en"
                                    }
                                    sourceExpanded = false
                                },
                            )
                        }
                    }
                }

                IconButton(onClick = {
                    val previousSource = sourceLanguage
                    sourceLanguage = targetLanguage
                    targetLanguage = previousSource
                }) {
                    Icon(Icons.Outlined.SwapHoriz, contentDescription = "Swap languages")
                }

                ExposedDropdownMenuBox(
                    expanded = targetExpanded,
                    onExpandedChange = { targetExpanded = !targetExpanded },
                    modifier = Modifier.weight(1f),
                ) {
                    OutlinedTextField(
                        value = languages.first { it.code == targetLanguage }.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("To") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                    )
                    ExposedDropdownMenu(
                        expanded = targetExpanded,
                        onDismissRequest = { targetExpanded = false },
                    ) {
                        languages.forEach { language ->
                            DropdownMenuItem(
                                text = { Text(language.label) },
                                onClick = {
                                    targetLanguage = language.code
                                    if (sourceLanguage == targetLanguage) {
                                        sourceLanguage = if (language.code == "en") "hi" else "en"
                                    }
                                    targetExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Source text") },
                minLines = 7,
                maxLines = 15,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        if (inputText.isBlank()) {
                            status = "Enter text to translate."
                            return@Button
                        }
                        isLoading = true
                        status = null
                        coroutineScope.launch {
                            val translated = withContext(Dispatchers.Default) {
                                OfflineTranslator.translate(inputText, sourceLanguage, targetLanguage)
                            }
                            isLoading = false
                            outputText = translated
                            status = if (translated.isBlank()) "Unsupported input" else "Translation ready"
                        }
                    },
                    enabled = !isLoading,
                ) {
                    Text(if (isLoading) "Translating..." else "Translate")
                }
                OutlinedButton(onClick = {
                    inputText = ""
                    outputText = ""
                    status = null
                }) {
                    Text("Clear")
                }
            }

            if (status != null) {
                Text(
                    text = status.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

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
                            text = "Translation",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(outputText))
                        }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy translation")
                        }
                    }
                    Text(
                        text = outputText.ifBlank { "Your translated text will appear here." },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}
