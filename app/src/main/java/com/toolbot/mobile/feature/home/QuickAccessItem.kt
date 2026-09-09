package com.toolbot.mobile.feature.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class QuickAccessItem(
    val name: String,
    val icon: ImageVector,
    val accent: Color = Color.Unspecified,
)
