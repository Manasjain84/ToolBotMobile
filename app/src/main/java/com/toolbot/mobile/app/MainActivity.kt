package com.toolbot.mobile.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.toolbot.mobile.core.designsystem.ToolBotMobileTheme
import com.toolbot.mobile.core.navigation.ToolBotNavHost
import com.toolbot.mobile.feature.home.ToolBotHomeRoute
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(this)
        enableEdgeToEdge()
        setContent {
            ToolBotMobileTheme {
                ToolBotNavHost()
            }
        }
    }
}

@Composable
fun HomePreview() {
    ToolBotHomeRoute()
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ToolBotMobileTheme {
        HomePreview()
    }
}