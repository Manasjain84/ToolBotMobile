package com.toolbot.mobile.feature.favorites

import androidx.compose.runtime.mutableStateSetOf

object FavoriteStore {
    private val favoriteToolNames = mutableStateSetOf<String>()

    fun isFavorite(toolName: String): Boolean = toolName in favoriteToolNames

    fun toggle(toolName: String) {
        if (favoriteToolNames.contains(toolName)) {
            favoriteToolNames.remove(toolName)
        } else {
            favoriteToolNames.add(toolName)
        }
    }

    fun add(toolName: String) {
        favoriteToolNames.add(toolName)
    }

    fun remove(toolName: String) {
        favoriteToolNames.remove(toolName)
    }

    fun getAll(): Set<String> = favoriteToolNames.toSet()
}
