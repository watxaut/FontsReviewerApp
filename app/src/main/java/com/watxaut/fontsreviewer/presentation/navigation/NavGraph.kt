package com.watxaut.fontsreviewer.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.watxaut.fontsreviewer.presentation.auth.login.LoginScreen
import com.watxaut.fontsreviewer.presentation.auth.register.RegisterScreen
import com.watxaut.fontsreviewer.presentation.details.FountainDetailsScreen
import com.watxaut.fontsreviewer.presentation.leaderboard.LeaderboardScreen
import com.watxaut.fontsreviewer.presentation.map.MapScreen
import com.watxaut.fontsreviewer.presentation.profile.ProfileScreen
import com.watxaut.fontsreviewer.presentation.review.ReviewScreen
import com.watxaut.fontsreviewer.presentation.stats.StatsScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Map.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // Navigate back to Profile screen which will refresh and show user info
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    // Navigate back to Profile screen which will refresh and show user info
                    navController.navigate(Screen.Profile.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Map.route) { backStackEntry ->
            MapScreen(
                onFountainClick = { fountainId ->
                    navController.navigate(Screen.FountainDetails.createRoute(fountainId))
                },
                savedStateHandle = backStackEntry.savedStateHandle
            )
        }

        composable(
            route = Screen.FountainDetails.route,
            arguments = listOf(navArgument("fountainId") { type = NavType.StringType })
        ) { backStackEntry ->
            val fountainId = backStackEntry.arguments?.getString("fountainId") ?: return@composable
            FountainDetailsScreen(
                onNavigateBack = { navController.popBackStack() },
                onAddReview = {
                    navController.navigate(Screen.Review.createRoute(fountainId))
                },
                savedStateHandle = backStackEntry.savedStateHandle
            )
        }

        composable(
            route = Screen.Review.route,
            arguments = listOf(navArgument("fountainId") { type = NavType.StringType })
        ) {
            ReviewScreen(
                onNavigateBack = { navController.popBackStack() },
                onReviewSubmitted = {
                    // Mark that a review was submitted so Map can refresh
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("reviewSubmitted", true)
                    // Navigate back to fountain details
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Stats.route) {
            StatsScreen(
                onBestFountainClick = { fountainId ->
                    navController.navigate(Screen.FountainDetails.createRoute(fountainId))
                }
            )
        }

        composable(Screen.Leaderboard.route) {
            LeaderboardScreen()
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLogout = {
                    // Just refresh the profile screen to show NotAuthenticated state
                    // User stays on Profile screen
                }
            )
        }
    }
}
