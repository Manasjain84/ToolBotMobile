package com.toolbot.mobile.feature.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class ToolCategoryItem(
    val name: String,
    val icon: ImageVector,
    val categoryId: String = "",
    val accent: Color = Color.Unspecified,
)
