package com.toolbot.mobile.core.navigation

enum class ToolBotBottomDestination(
    val label: String,
    val iconName: String,
) {
    HOME(label = "Home", iconName = "home"),
    TOOLS(label = "Tools", iconName = "tools"),
    FAVORITES(label = "Favorites", iconName = "favorites"),
    SETTINGS(label = "Settings", iconName = "settings"),
}
