package com.toolbot.mobile.feature.favorites

import android.content.Context
import androidx.compose.runtime.mutableStateSetOf
import com.toolbot.mobile.core.db.ToolBotRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object FavoriteStore {
    private val favoriteToolNames = mutableStateSetOf<String>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var repository: ToolBotRepository? = null

    fun init(context: Context) {
        if (repository == null) {
            repository = ToolBotRepository.getInstance(context)
            scope.launch(Dispatchers.IO) {
                val list = repository?.getAllFavoritesList().orEmpty()
                withContext(Dispatchers.Main) {
                    favoriteToolNames.clear()
                    favoriteToolNames.addAll(list.map { it.toolName })
                }
            }
            scope.launch {
                repository?.favoritesFlow?.collectLatest { list ->
                    favoriteToolNames.clear()
                    favoriteToolNames.addAll(list.map { it.toolName })
                }
            }
        }
    }

    fun isFavorite(toolName: String): Boolean = favoriteToolNames.contains(toolName)

    fun toggle(toolName: String) {
        val repo = repository ?: return
        scope.launch(Dispatchers.IO) {
            repo.toggleFavorite(toolName)
        }
        if (favoriteToolNames.contains(toolName)) {
            favoriteToolNames.remove(toolName)
        } else {
            favoriteToolNames.add(toolName)
        }
    }

    fun add(toolName: String) {
        val repo = repository ?: return
        scope.launch(Dispatchers.IO) {
            repo.addFavorite(toolName)
        }
        favoriteToolNames.add(toolName)
    }

    fun remove(toolName: String) {
        val repo = repository ?: return
        scope.launch(Dispatchers.IO) {
            repo.removeFavorite(toolName)
        }
        favoriteToolNames.remove(toolName)
    }

    fun getAll(): Set<String> = favoriteToolNames.toSet()
}
