package com.watxaut.fontsreviewer.presentation.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Map : Screen("map")
    object FountainDetails : Screen("fountain/{fountainId}") {
        fun createRoute(fountainId: String) = "fountain/$fountainId"
    }
    object Review : Screen("review/{fountainId}") {
        fun createRoute(fountainId: String) = "review/$fountainId"
    }
    object Stats : Screen("stats")
    object Leaderboard : Screen("leaderboard")
    object Profile : Screen("profile")
}
