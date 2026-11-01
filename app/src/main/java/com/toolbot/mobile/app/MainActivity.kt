package com.toolbot.mobile.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.tooling.preview.Preview
import com.toolbot.mobile.core.datastore.AppThemeMode
import com.toolbot.mobile.core.datastore.ThemePreferencesManager
import com.toolbot.mobile.core.designsystem.ToolBotMobileTheme
import com.toolbot.mobile.core.navigation.ToolBotNavHost
import com.toolbot.mobile.feature.home.ToolBotHomeRoute
import com.toolbot.mobile.feature.favorites.FavoriteStore
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(this)
        FavoriteStore.init(applicationContext)
        enableEdgeToEdge()
        val themeManager = ThemePreferencesManager.getInstance(applicationContext)
        setContent {
            val themeMode by themeManager.themeModeFlow.collectAsState(initial = AppThemeMode.SYSTEM)
            ToolBotMobileTheme(themeMode = themeMode) {
                ToolBotNavHost()
            }
        }
    }
}

@Composable
fun HomePreview() {
    ToolBotMobileTheme {
        HomePreview()
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ToolBotMobileTheme {
        HomePreview()
    }
}
