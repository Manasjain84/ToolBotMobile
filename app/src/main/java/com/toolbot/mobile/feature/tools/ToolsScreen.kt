package com.toolbot.mobile.feature.tools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.GTranslate
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Percent
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Summarize
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Transform
import androidx.compose.material.icons.outlined.WatchLater
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private enum class ToolCategory(val title: String, val accent: Color) {
    SMART_EDUCATION("Smart Education", Color(0xFF1E88E5)),
    PDF_DOCUMENTS("PDF & Documents", Color(0xFFE53935)),
    IMAGE_CAMERA("Image & Camera", Color(0xFFFF9800)),
    AI_TEXT("AI & Text", Color(0xFF8E24AA)),
    GENERAL_UTILITIES("General Utilities", Color(0xFF3949AB)),
}

private data class ToolItem(
    val name: String,
    val category: ToolCategory,
    val icon: ImageVector,
)

private val toolItems = listOf(
    ToolItem("GPA / SGPA Calculator", ToolCategory.SMART_EDUCATION, Icons.Outlined.Calculate),
    ToolItem("CGPA Calculator", ToolCategory.SMART_EDUCATION, Icons.Outlined.ThumbUp),
    ToolItem("Smart Attendance", ToolCategory.SMART_EDUCATION, Icons.Outlined.WatchLater),
    ToolItem("Percentage & Grade", ToolCategory.SMART_EDUCATION, Icons.Outlined.Percent),
    ToolItem("PDF to Word", ToolCategory.PDF_DOCUMENTS, Icons.Outlined.Description),
    ToolItem("Word to PDF", ToolCategory.PDF_DOCUMENTS, Icons.Outlined.PictureAsPdf),
    ToolItem("PDF Merger", ToolCategory.PDF_DOCUMENTS, Icons.Outlined.ContentCopy),
    ToolItem("PDF Splitter", ToolCategory.PDF_DOCUMENTS, Icons.Outlined.DocumentScanner),
    ToolItem("PDF Compressor", ToolCategory.PDF_DOCUMENTS, Icons.Outlined.FlashOn),
    ToolItem("Image Compressor", ToolCategory.IMAGE_CAMERA, Icons.Outlined.Image),
    ToolItem("Image to PDF", ToolCategory.IMAGE_CAMERA, Icons.Outlined.PictureAsPdf),
    ToolItem("QR Scanner", ToolCategory.IMAGE_CAMERA, Icons.Outlined.QrCode2),
    ToolItem("QR Generator", ToolCategory.IMAGE_CAMERA, Icons.Outlined.QrCode2),
    ToolItem("AI Text Summarizer", ToolCategory.AI_TEXT, Icons.Outlined.Summarize),
    ToolItem("Grammar Checker / Text Improver", ToolCategory.AI_TEXT, Icons.Outlined.SmartToy),
    ToolItem("Text Translator", ToolCategory.AI_TEXT, Icons.Outlined.GTranslate),
    ToolItem("Keyword Extractor", ToolCategory.AI_TEXT, Icons.Outlined.TextFields),
    ToolItem("Scientific Calculator", ToolCategory.GENERAL_UTILITIES, Icons.Outlined.Calculate),
    ToolItem("Unit Converter", ToolCategory.GENERAL_UTILITIES, Icons.Outlined.Transform),
    ToolItem("EMI Calculator", ToolCategory.GENERAL_UTILITIES, Icons.Outlined.MenuBook),
)

@Composable
fun ToolsScreen(
    modifier: Modifier = Modifier,
    onToolClick: (String) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    val filteredTools = remember(query) {
        if (query.isBlank()) {
            toolItems
        } else {
            toolItems.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    val groupedTools = remember(filteredTools) {
        ToolCategory.entries.associateWith { category ->
            filteredTools.filter { it.category == category }
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Tools",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Everything you need in one place",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search tools") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                    },
                )
            }

            if (filteredTools.isEmpty()) {
                item {
                    Text(
                        text = "No tools found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                ToolCategory.entries.forEach { category ->
                    val categoryTools = groupedTools.getValue(category)
                    if (categoryTools.isNotEmpty()) {
                        item {
                            CategorySection(
                                category = category,
                                tools = categoryTools,
                                onToolClick = onToolClick,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySection(
    category: ToolCategory,
    tools: List<ToolItem>,
    onToolClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = category.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = category.accent,
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            tools.forEach { tool ->
                ToolCard(
                    tool = tool,
                    accent = category.accent,
                    onClick = { onToolClick(tool.name) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolCard(
    tool: ToolItem,
    accent: Color,
    onClick: () -> Unit,
) {
    var favorite by rememberSaveable(tool.name) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = tool.icon,
                contentDescription = null,
                tint = accent,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tool.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                AssistChip(
                    onClick = { },
                    label = { Text(tool.category.title) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = accent.copy(alpha = 0.10f),
                        labelColor = accent,
                    ),
                )
            }
            IconButton(onClick = { favorite = !favorite }) {
                Icon(
                    imageVector = if (favorite) Icons.Outlined.FavoriteBorder else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (favorite) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
