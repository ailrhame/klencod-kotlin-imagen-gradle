package com.example.imagen.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.imagen.ui.screens.AboutScreen
import com.example.imagen.ui.screens.BinScreen
import com.example.imagen.ui.screens.GenerateScreen
import com.example.imagen.ui.screens.GalleryScreen
import com.example.imagen.ui.screens.PromptHistoryScreen
import com.example.imagen.ui.screens.SettingsScreen

sealed class DrawerRoutes(val route: String, val title: String) {
    object Generate : DrawerRoutes("generate", "Generate")
    object Gallery : DrawerRoutes("gallery", "Gallery")
    object PromptHistory : DrawerRoutes("prompt_history", "Prompt History")
    object Settings : DrawerRoutes("settings", "Settings")
    object Bin : DrawerRoutes("bin", "Bin")
    object About : DrawerRoutes("about", "About")
}

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = DrawerRoutes.Generate.route) {
        composable(DrawerRoutes.Generate.route) { GenerateScreen() }
        composable(DrawerRoutes.Gallery.route) { GalleryScreen() }
        composable(DrawerRoutes.PromptHistory.route) { PromptHistoryScreen() }
        composable(DrawerRoutes.Settings.route) { SettingsScreen() }
        composable(DrawerRoutes.Bin.route) { BinScreen() }
        composable(DrawerRoutes.About.route) { AboutScreen() }
    }
}