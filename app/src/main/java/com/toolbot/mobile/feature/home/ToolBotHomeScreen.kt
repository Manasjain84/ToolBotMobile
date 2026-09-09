package com.toolbot.mobile.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Summarize
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.toolbot.mobile.core.designsystem.ToolBotMobileTheme
import com.toolbot.mobile.core.navigation.ToolBotBottomDestination
import com.toolbot.mobile.feature.tools.ToolCategory
import com.toolbot.mobile.feature.tools.toolItems

@Composable
fun ToolBotHomeRoute(
    modifier: Modifier = Modifier,
    selectedBottomDestination: ToolBotBottomDestination = ToolBotBottomDestination.HOME,
    onBottomDestinationSelected: (ToolBotBottomDestination) -> Unit = {},
    onToolClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
) {
    ToolBotHomeScreen(
        modifier = modifier,
        selectedBottomDestination = selectedBottomDestination,
        onBottomDestinationSelected = onBottomDestinationSelected,
        onToolClick = onToolClick,
        onCategoryClick = onCategoryClick,
    )
}

@Composable
fun ToolBotHomeScreen(
    modifier: Modifier = Modifier,
    selectedBottomDestination: ToolBotBottomDestination = ToolBotBottomDestination.HOME,
    onBottomDestinationSelected: (ToolBotBottomDestination) -> Unit = {},
    onToolClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit = {},
) {
    val quickAccessItems = remember {
        listOf(
            QuickAccessItem("Smart Attendance", Icons.Outlined.School, Color(0xFF1E88E5)),
            QuickAccessItem("GPA Calculator", Icons.Outlined.Calculate, Color(0xFF43A047)),
            QuickAccessItem("PDF Compressor", Icons.Outlined.Compress, Color(0xFFE53935)),
            QuickAccessItem("AI Summarizer", Icons.Outlined.Summarize, Color(0xFF8E24AA)),
        )
    }

    val categories = remember {
        ToolCategory.entries.map { category ->
            ToolCategoryItem(
                name = category.title,
                icon = categoryIcon(category),
                categoryId = category.routeKey,
                accent = category.accent,
            )
        }
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    val matchingTools = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList() else {
            toolItems.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        modifier = modifier,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ToolBot",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Your everyday toolkit",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Welcome back",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = "Stay productive today.",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.28f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LibraryBooks,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search tools, shortcuts, and features") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                    },
                )
            }

            if (matchingTools.isNotEmpty()) {
                item {
                    SearchResultsSection(
                        tools = matchingTools,
                        onToolClick = onToolClick,
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle(title = "Quick Access")
                    QuickAccessGrid(items = quickAccessItems, onItemClick = onToolClick)
                }
            }

            item {
                SectionTitle(title = "Explore")
            }

            items(categories) { category ->
                CategoryRow(
                    item = category,
                    onClick = { onCategoryClick(category.categoryId) },
                )
            }
        }
    }
}

@Composable
private fun SearchResultsSection(
    tools: List<com.toolbot.mobile.feature.tools.ToolItem>,
    onToolClick: (String) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Matches",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            tools.take(5).forEach { tool ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToolClick(tool.name) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = null,
                            tint = tool.category.accent,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = tool.name,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun QuickAccessGrid(
    items: List<QuickAccessItem>,
    onItemClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { item ->
                    QuickAccessCard(
                        item = item,
                        modifier = Modifier.weight(1f),
                        onClick = { onItemClick(item.name) },
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickAccessCard(
    item: QuickAccessItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CategoryRow(
    item: ToolCategoryItem,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(item.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = item.accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = Icons.Outlined.Article,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun categoryIcon(category: ToolCategory) = when (category) {
    ToolCategory.SMART_EDUCATION -> Icons.Outlined.School
    ToolCategory.PDF_DOCUMENTS -> Icons.Outlined.Description
    ToolCategory.AI_TEXT -> Icons.Outlined.AutoAwesome
    ToolCategory.IMAGE_CAMERA -> Icons.Outlined.CameraAlt
    ToolCategory.GENERAL_UTILITIES -> Icons.Outlined.Tune
}

@Preview(showBackground = true, widthDp = 380, heightDp = 800)
@Composable
private fun ToolBotHomeScreenLightPreview() {
    ToolBotMobileTheme(darkTheme = false, dynamicColor = false) {
        ToolBotHomeScreen()
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 800)
@Composable
private fun ToolBotHomeScreenDarkPreview() {
    ToolBotMobileTheme(darkTheme = true, dynamicColor = false) {
        ToolBotHomeScreen()
    }
}
