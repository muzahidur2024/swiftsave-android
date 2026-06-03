package com.swiftsave.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.swiftsave.app.R
import com.swiftsave.app.ui.screens.HomeScreen
import com.swiftsave.app.ui.screens.LibraryScreen
import com.swiftsave.app.ui.screens.SettingsScreen
import com.swiftsave.app.ui.screens.TutorialScreen
import com.swiftsave.app.ui.theme.SwiftSaveTheme

sealed class TabDestination(val route: String, val labelRes: Int, val icon: ImageVector) {
    data object Home : TabDestination("home", R.string.tab_download, Icons.Filled.Download)

    data object Library : TabDestination("library", R.string.tab_library, Icons.Filled.Movie)

    data object Settings : TabDestination("settings", R.string.tab_settings, Icons.Filled.Settings)
}

private val tabs = listOf(TabDestination.Home, TabDestination.Library, TabDestination.Settings)

@Composable
fun SwiftSaveApp() {
    SwiftSaveTheme {
        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val current = backStack?.destination
        val showBottomBar = current?.route != "tutorial"
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    ) {
                        tabs.forEach { tab ->
                            val selected = current?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = { Icon(tab.icon, contentDescription = null) },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors =
                                    NavigationBarItemDefaults.colors(
                                        indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                        selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                        unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    ),
                            )
                        }
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = TabDestination.Home.route,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                composable(TabDestination.Home.route) {
                    HomeScreen(
                        onOpenTutorial = { navController.navigate("tutorial") },
                    )
                }
                composable(TabDestination.Library.route) { LibraryScreen() }
                composable(TabDestination.Settings.route) {
                    SettingsScreen(onOpenTutorial = { navController.navigate("tutorial") })
                }
                composable("tutorial") {
                    TutorialScreen(
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
}
