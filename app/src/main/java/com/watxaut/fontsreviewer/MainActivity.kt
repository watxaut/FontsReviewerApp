package com.watxaut.fontsreviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.watxaut.fontsreviewer.presentation.navigation.NavGraph
import com.watxaut.fontsreviewer.presentation.navigation.Screen
import com.watxaut.fontsreviewer.ui.theme.FontsReviewerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FontsReviewerTheme {
                FontsReviewerAppContent()
            }
        }
    }
}

@Composable
fun FontsReviewerAppContent() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Bottom navigation items
    val bottomNavItems = listOf(
        BottomNavItem(
            route = Screen.Map.route,
            titleRes = R.string.nav_map,
            iconRes = android.R.drawable.ic_dialog_map
        ),
        BottomNavItem(
            route = Screen.Stats.route,
            titleRes = R.string.nav_stats,
            iconRes = android.R.drawable.ic_menu_info_details
        ),
        BottomNavItem(
            route = Screen.Leaderboard.route,
            titleRes = R.string.nav_leaderboard,
            iconRes = android.R.drawable.star_big_on
        ),
        BottomNavItem(
            route = Screen.Profile.route,
            titleRes = R.string.nav_profile,
            iconRes = android.R.drawable.ic_menu_myplaces
        )
    )

    Scaffold(
        bottomBar = {
            // Show bottom nav only on main screens
            if (currentDestination?.route in bottomNavItems.map { it.route }) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(id = item.iconRes),
                                    contentDescription = stringResource(id = item.titleRes)
                                )
                            },
                            label = {
                                Text(text = stringResource(id = item.titleRes))
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            // Start at map screen - anonymous browsing allowed
            startDestination = Screen.Map.route,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

data class BottomNavItem(
    val route: String,
    val titleRes: Int,
    val iconRes: Int
)