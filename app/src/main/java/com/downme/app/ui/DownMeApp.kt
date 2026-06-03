package com.downme.app.ui

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.downme.app.DownMeApplication
import com.downme.app.data.AppThemeMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.downme.app.R
import com.downme.app.ui.screens.HomeScreen
import com.downme.app.ui.screens.LibraryScreen
import com.downme.app.ui.screens.PlayerScreen
import com.downme.app.ui.screens.SettingsScreen
import com.downme.app.ui.screens.TutorialScreen
import com.downme.app.ui.theme.DownMeTheme

sealed class TabDestination(val route: String, val labelRes: Int, val icon: ImageVector) {
    data object Home : TabDestination("home", R.string.tab_download, Icons.Filled.Download)

    data object Library : TabDestination("library", R.string.tab_library, Icons.Filled.Movie)

    data object Settings : TabDestination("settings", R.string.tab_settings, Icons.Filled.Settings)
}

private val tabs = listOf(TabDestination.Home, TabDestination.Library, TabDestination.Settings)

@Composable
fun DownMeApp(
    pendingSharedDownloadUrl: String? = null,
    onPendingSharedDownloadUrlConsumed: () -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as DownMeApplication
    val themeMode by app.userPreferences.themeMode.collectAsStateWithLifecycle(
        initialValue = AppThemeMode.Dark,
    )
    DownMeTheme(themeMode = themeMode) {
        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val current = backStack?.destination
        LaunchedEffect(pendingSharedDownloadUrl) {
            if (!pendingSharedDownloadUrl.isNullOrBlank()) {
                navController.navigate(TabDestination.Home.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
        val showBottomBar =
            current?.route != "tutorial" && current?.route?.startsWith("player/") != true
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
                        pendingSharedDownloadUrl = pendingSharedDownloadUrl,
                        onPendingSharedDownloadUrlConsumed = onPendingSharedDownloadUrlConsumed,
                    )
                }
                composable(TabDestination.Library.route) {
                    LibraryScreen(
                        onPlay = { jobId -> navController.navigate("player/$jobId") },
                    )
                }
                composable("player/{jobId}") { entry ->
                    val jobId = entry.arguments?.getString("jobId").orEmpty()
                    PlayerScreen(
                        jobId = jobId,
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(TabDestination.Settings.route) {
                    SettingsScreen()
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
