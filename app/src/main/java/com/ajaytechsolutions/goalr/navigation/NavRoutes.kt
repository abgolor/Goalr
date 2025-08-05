package com.ajaytechsolutions.goalr.navigation

sealed class NavRoutes(val route: String) {
    object Splash : NavRoutes("splash")
    object Registration : NavRoutes("registration")
    object Home : NavRoutes("home")
}
