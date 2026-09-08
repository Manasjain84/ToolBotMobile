package com.toolbot.mobile.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.toolbot.mobile.feature.favorites.FavoritesScreen
import com.toolbot.mobile.feature.home.ToolBotHomeRoute
import com.toolbot.mobile.feature.pdfdocuments.compressor.PdfCompressorScreen
import com.toolbot.mobile.feature.pdfdocuments.merger.PdfMergerScreen
import com.toolbot.mobile.feature.pdfdocuments.splitter.PdfSplitterScreen
import com.toolbot.mobile.feature.settings.SettingsScreen
import com.toolbot.mobile.feature.smarteducation.attendance.SmartAttendanceScreen
import com.toolbot.mobile.feature.smarteducation.cgpa.CgpaScreen
import com.toolbot.mobile.feature.smarteducation.gpa.GpaSgpaScreen
import com.toolbot.mobile.feature.smarteducation.percentage.PercentageGradeScreen
import com.toolbot.mobile.feature.tools.ToolsScreen

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

@Composable
fun ToolBotNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val currentBottomDestination = ToolBotBottomDestination.entries.firstOrNull { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    } ?: ToolBotBottomDestination.HOME

    NavHost(
        navController = navController,
        startDestination = HOME_ROUTE,
        modifier = modifier,
    ) {
        composable(HOME_ROUTE) {
            ToolBotHomeRoute(
                selectedBottomDestination = currentBottomDestination,
                onBottomDestinationSelected = { destination ->
                    navController.navigateToBottomDestination(destination)
                },
            )
        }
        composable(TOOLS_ROUTE) {
            ToolsScreen(
                onToolClick = { toolName ->
                    when (toolName) {
                        "GPA / SGPA Calculator" -> navController.navigate(GPA_SGPA_ROUTE)
                        "CGPA Calculator" -> navController.navigate(CGPA_ROUTE)
                        "Smart Attendance" -> navController.navigate(SMART_ATTENDANCE_ROUTE)
                        "Percentage & Grade" -> navController.navigate(PERCENTAGE_GRADE_ROUTE)
                        "PDF Merger" -> navController.navigate(PDF_MERGER_ROUTE)
                        "PDF Splitter" -> navController.navigate(PDF_SPLITTER_ROUTE)
                        "PDF Compressor" -> navController.navigate(PDF_COMPRESSOR_ROUTE)
                    }
                },
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
    }
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
    navigate(destination.route) {
        launchSingleTop = true
    }
}
