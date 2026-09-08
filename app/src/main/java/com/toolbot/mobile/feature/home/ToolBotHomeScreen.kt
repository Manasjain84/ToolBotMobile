package com.toolbot.mobile.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Summarize
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.toolbot.mobile.core.designsystem.ToolBotMobileTheme
import com.toolbot.mobile.core.navigation.ToolBotBottomDestination

@Composable
fun ToolBotHomeRoute(
    modifier: Modifier = Modifier,
    selectedBottomDestination: ToolBotBottomDestination = ToolBotBottomDestination.HOME,
    onBottomDestinationSelected: (ToolBotBottomDestination) -> Unit = {},
) {
    ToolBotHomeScreen(
        modifier = modifier,
        selectedBottomDestination = selectedBottomDestination,
        onBottomDestinationSelected = onBottomDestinationSelected,
    )
}

@Composable
fun ToolBotHomeScreen(
    modifier: Modifier = Modifier,
    selectedBottomDestination: ToolBotBottomDestination = ToolBotBottomDestination.HOME,
    onBottomDestinationSelected: (ToolBotBottomDestination) -> Unit = {},
) {
    val quickAccessItems = remember {
        listOf(
            QuickAccessItem(name = "Smart Attendance", icon = Icons.Outlined.School),
            QuickAccessItem(name = "GPA Calculator", icon = Icons.Outlined.Calculate),
            QuickAccessItem(name = "PDF Compressor", icon = Icons.Outlined.Compress),
            QuickAccessItem(name = "AI Summarizer", icon = Icons.Outlined.Summarize),
        )
    }

    val categories = remember {
        listOf(
            ToolCategoryItem(name = "Smart Education", icon = Icons.Outlined.AutoStories),
            ToolCategoryItem(name = "PDF & Documents", icon = Icons.Outlined.Description),
            ToolCategoryItem(name = "AI & Text", icon = Icons.Outlined.Psychology),
            ToolCategoryItem(name = "Developer Tools", icon = Icons.Outlined.Build),
            ToolCategoryItem(name = "Image & Camera", icon = Icons.Outlined.Image),
            ToolCategoryItem(name = "General Utilities", icon = Icons.Outlined.Tune),
        )
    }

    var searchQuery by remember { mutableStateOf("") }
    Scaffold(
        modifier = modifier,
        bottomBar = {
            ToolBotBottomBar(
                selected = selectedBottomDestination,
                onSelected = onBottomDestinationSelected,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 24.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search tools") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
                    },
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle(title = "Quick Access")
                    QuickAccessRow(items = quickAccessItems)
                }
            }

            item {
                SectionTitle(title = "Explore")
            }

            items(categories) { category ->
                CategoryRow(item = category)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickAccessRow(items: List<QuickAccessItem>) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.clickable { },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(item: ToolCategoryItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { }
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ToolBotBottomBar(
    selected: ToolBotBottomDestination,
    onSelected: (ToolBotBottomDestination) -> Unit,
) {
    NavigationBar {
        ToolBotBottomDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == selected,
                onClick = { onSelected(destination) },
                icon = {
                    Icon(
                        imageVector = destinationIcon(destination),
                        contentDescription = destination.label,
                    )
                },
                label = { Text(destination.label) },
            )
        }
    }
}

private fun destinationIcon(destination: ToolBotBottomDestination) = when (destination) {
    ToolBotBottomDestination.HOME -> Icons.Outlined.Home
    ToolBotBottomDestination.TOOLS -> Icons.Outlined.Build
    ToolBotBottomDestination.FAVORITES -> Icons.Outlined.FavoriteBorder
    ToolBotBottomDestination.SETTINGS -> Icons.Outlined.Settings
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
