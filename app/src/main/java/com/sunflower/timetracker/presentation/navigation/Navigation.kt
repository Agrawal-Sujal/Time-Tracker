package com.sunflower.timetracker.presentation.navigation

sealed class Screen(val route: String) {
    object Home     : Screen("home")
    object Analysis : Screen("analysis")
    object Tags     : Screen("tags")
}