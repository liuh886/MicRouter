package com.liuh886.microuter.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.liuh886.microuter.MicRouterApp
import com.liuh886.microuter.ui.dashboard.DashboardScreen
import com.liuh886.microuter.ui.inspector.InspectorScreen
import com.liuh886.microuter.ui.mictest.MicTestScreen

private data class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val tabs = listOf(
    Tab("dashboard", "Dashboard", Icons.Filled.Speaker),
    Tab("inspector", "Inspector", Icons.Filled.History),
    Tab("mictest", "Mic Test", Icons.Filled.Mic)
)

@Composable
fun RootScaffold(
    app: MicRouterApp,
    micPermissionGranted: Boolean,
    onRequestMicPermission: () -> Unit
) {
    val navController = rememberNavController()
    val dark = isSystemInDarkTheme()
    val navBarColor = MaterialTheme.colorScheme.surface.copy(alpha = if (dark) 0.80f else 0.86f)
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = navBarColor,
                tonalElevation = 0.dp
            ) {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(padding)
        ) {
            composable("dashboard") {
                DashboardScreen(repository = app.audioRepository)
            }
            composable("inspector") {
                InspectorScreen(repository = app.audioRepository)
            }
            composable("mictest") {
                MicTestScreen(
                    repository = app.audioRepository,
                    tester = app.micTester,
                    micPermissionGranted = micPermissionGranted,
                    onRequestMicPermission = onRequestMicPermission
                )
            }
        }
    }
}
