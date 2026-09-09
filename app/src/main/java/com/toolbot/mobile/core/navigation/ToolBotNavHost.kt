package com.toolbot.mobile.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.toolbot.mobile.feature.aitext.grammar.GrammarImproverScreen
import com.toolbot.mobile.feature.aitext.keywords.KeywordExtractorScreen
import com.toolbot.mobile.feature.aitext.summarizer.AiTextSummarizerScreen
import com.toolbot.mobile.feature.aitext.translator.TextTranslatorScreen
import com.toolbot.mobile.feature.favorites.FavoritesScreen
import com.toolbot.mobile.feature.generalutilities.emicalculator.EmiCalculatorScreen
import com.toolbot.mobile.feature.generalutilities.scientificcalculator.ScientificCalculatorScreen
import com.toolbot.mobile.feature.generalutilities.unitconverter.UnitConverterScreen
import com.toolbot.mobile.feature.home.ToolBotHomeRoute
import com.toolbot.mobile.feature.pdfdocuments.compressor.PdfCompressorScreen
import com.toolbot.mobile.feature.pdfdocuments.merger.PdfMergerScreen
import com.toolbot.mobile.feature.pdfdocuments.splitter.PdfSplitterScreen
import com.toolbot.mobile.feature.pdfdocuments.word.PdfToWordScreen
import com.toolbot.mobile.feature.pdfdocuments.word.WordToPdfScreen
import com.toolbot.mobile.feature.settings.SettingsScreen
import com.toolbot.mobile.feature.smarteducation.attendance.SmartAttendanceScreen
import com.toolbot.mobile.feature.smarteducation.cgpa.CgpaScreen
import com.toolbot.mobile.feature.smarteducation.gpa.GpaSgpaScreen
import com.toolbot.mobile.feature.smarteducation.percentage.PercentageGradeScreen
import com.toolbot.mobile.feature.tools.ToolsScreen
import com.toolbot.mobile.feature.imagecamera.compressor.ImageCompressorScreen
import com.toolbot.mobile.feature.imagecamera.imagetopdf.ImageToPdfScreen
import com.toolbot.mobile.feature.imagecamera.resizer.ImageResizerScreen
import com.toolbot.mobile.feature.imagecamera.converter.ImageFormatConverterScreen

private const val HOME_ROUTE = "home"
private const val TOOLS_ROUTE = "tools"
private const val FAVORITES_ROUTE = "favorites"
private const val SETTINGS_ROUTE = "settings"
private const val GPA_SGPA_ROUTE = "gpa_sgpa"
private const val CGPA_ROUTE = "cgpa"
private const val SMART_ATTENDANCE_ROUTE = "smart_attendance"
private const val PERCENTAGE_GRADE_ROUTE = "percentage_grade"
private const val PDF_MERGER_ROUTE = "pdf_merger"
private const val PDF_SPLITTER_ROUTE = "pdf_splitter"
private const val PDF_COMPRESSOR_ROUTE = "pdf_compressor"
private const val PDF_TO_WORD_ROUTE = "pdf_to_word"
private const val WORD_TO_PDF_ROUTE = "word_to_pdf"
private const val IMAGE_COMPRESSOR_ROUTE = "image_compressor"
private const val IMAGE_TO_PDF_ROUTE = "image_to_pdf"
private const val IMAGE_RESIZER_ROUTE = "image_resizer"
private const val IMAGE_CONVERTER_ROUTE = "image_converter"
private const val SCIENTIFIC_CALCULATOR_ROUTE = "scientific_calculator"
private const val UNIT_CONVERTER_ROUTE = "unit_converter"
private const val EMI_CALCULATOR_ROUTE = "emi_calculator"
private const val AI_TEXT_SUMMARIZER_ROUTE = "ai_text_summarizer"
private const val GRAMMAR_IMPROVER_ROUTE = "grammar_improver"
private const val TEXT_TRANSLATOR_ROUTE = "text_translator"
private const val KEYWORD_EXTRACTOR_ROUTE = "keyword_extractor"

@Composable
fun ToolBotNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentBottomDestination = ToolBotBottomDestination.entries.firstOrNull { destination ->
        currentDestination?.hierarchy?.any { it.route?.startsWith(destination.route) == true } == true
    } ?: ToolBotBottomDestination.HOME

    Scaffold(
        bottomBar = {
            ToolBotBottomBar(
                selected = currentBottomDestination,
                onSelected = { destination ->
                    navController.navigateToBottomDestination(destination)
                },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HOME_ROUTE,
            modifier = modifier.padding(innerPadding),
        ) {
        composable(HOME_ROUTE) {
            ToolBotHomeRoute(
                selectedBottomDestination = currentBottomDestination,
                onBottomDestinationSelected = { destination ->
                    navController.navigateToBottomDestination(destination)
                },
                onToolClick = { toolName -> navController.navigateToTool(toolName) },
                onCategoryClick = { categoryId ->
                    navController.navigate("$TOOLS_ROUTE?category=$categoryId") {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(
            route = "$TOOLS_ROUTE?category={category}",
            arguments = listOf(
                navArgument("category") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { backStackEntry ->
            val selectedCategory = backStackEntry.arguments?.getString("category").orEmpty()
            ToolsScreen(
                selectedCategory = selectedCategory,
                onToolClick = { toolName -> navController.navigateToTool(toolName) },
            )
        }
        composable(FAVORITES_ROUTE) {
            FavoritesScreen()
        }
        composable(SETTINGS_ROUTE) {
            SettingsScreen()
        }
        composable(GPA_SGPA_ROUTE) {
            GpaSgpaScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(CGPA_ROUTE) {
            CgpaScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(SMART_ATTENDANCE_ROUTE) {
            SmartAttendanceScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(PERCENTAGE_GRADE_ROUTE) {
            PercentageGradeScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(PDF_MERGER_ROUTE) {
            PdfMergerScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(PDF_SPLITTER_ROUTE) {
            PdfSplitterScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(PDF_COMPRESSOR_ROUTE) {
            PdfCompressorScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(PDF_TO_WORD_ROUTE) {
            PdfToWordScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(WORD_TO_PDF_ROUTE) {
            WordToPdfScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(IMAGE_COMPRESSOR_ROUTE) {
            ImageCompressorScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(IMAGE_TO_PDF_ROUTE) {
            ImageToPdfScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(IMAGE_RESIZER_ROUTE) {
            ImageResizerScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(IMAGE_CONVERTER_ROUTE) {
            ImageFormatConverterScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(SCIENTIFIC_CALCULATOR_ROUTE) {
            ScientificCalculatorScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(UNIT_CONVERTER_ROUTE) {
            UnitConverterScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(EMI_CALCULATOR_ROUTE) {
            EmiCalculatorScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(AI_TEXT_SUMMARIZER_ROUTE) {
            AiTextSummarizerScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(GRAMMAR_IMPROVER_ROUTE) {
            GrammarImproverScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(TEXT_TRANSLATOR_ROUTE) {
            TextTranslatorScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        composable(KEYWORD_EXTRACTOR_ROUTE) {
            KeywordExtractorScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }
        }
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

private val ToolBotBottomDestination.route: String
    get() = when (this) {
        ToolBotBottomDestination.HOME -> HOME_ROUTE
        ToolBotBottomDestination.TOOLS -> TOOLS_ROUTE
        ToolBotBottomDestination.FAVORITES -> FAVORITES_ROUTE
        ToolBotBottomDestination.SETTINGS -> SETTINGS_ROUTE
    }

private fun androidx.navigation.NavHostController.navigateToBottomDestination(
    destination: ToolBotBottomDestination,
) {
    if (currentBackStackEntry?.destination?.route == destination.route) {
        return
    }

    navigate(destination.route) {
        launchSingleTop = true
        restoreState = true
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
    }
}

private fun androidx.navigation.NavHostController.navigateToTool(toolName: String) {
    when (toolName) {
        "GPA / SGPA Calculator", "GPA Calculator" -> navigate(GPA_SGPA_ROUTE)
        "CGPA Calculator" -> navigate(CGPA_ROUTE)
        "Smart Attendance" -> navigate(SMART_ATTENDANCE_ROUTE)
        "Percentage & Grade" -> navigate(PERCENTAGE_GRADE_ROUTE)
        "PDF Merger" -> navigate(PDF_MERGER_ROUTE)
        "PDF Splitter" -> navigate(PDF_SPLITTER_ROUTE)
        "PDF Compressor" -> navigate(PDF_COMPRESSOR_ROUTE)
        "PDF to Word" -> navigate(PDF_TO_WORD_ROUTE)
        "Word to PDF" -> navigate(WORD_TO_PDF_ROUTE)
        "Image Compressor" -> navigate(IMAGE_COMPRESSOR_ROUTE)
        "Image to PDF" -> navigate(IMAGE_TO_PDF_ROUTE)
        "Image Resizer" -> navigate(IMAGE_RESIZER_ROUTE)
        "Image Format Converter" -> navigate(IMAGE_CONVERTER_ROUTE)
        "Scientific Calculator" -> navigate(SCIENTIFIC_CALCULATOR_ROUTE)
        "Unit Converter" -> navigate(UNIT_CONVERTER_ROUTE)
        "EMI Calculator" -> navigate(EMI_CALCULATOR_ROUTE)
        "AI Text Summarizer" -> navigate(AI_TEXT_SUMMARIZER_ROUTE)
        "Grammar Checker / Text Improver" -> navigate(GRAMMAR_IMPROVER_ROUTE)
        "Text Translator" -> navigate(TEXT_TRANSLATOR_ROUTE)
        "Keyword Extractor" -> navigate(KEYWORD_EXTRACTOR_ROUTE)
    }
}
