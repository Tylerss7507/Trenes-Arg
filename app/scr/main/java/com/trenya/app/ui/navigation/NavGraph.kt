package com.trenya.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trenya.app.R
import com.trenya.app.ui.favorites.FavoritesScreen
import com.trenya.app.ui.home.HomeScreen
import com.trenya.app.ui.lines.LinesScreen
import com.trenya.app.ui.planner.TripPlannerScreen
import com.trenya.app.ui.search.SearchScreen
import com.trenya.app.ui.settings.SettingsScreen
import com.trenya.app.ui.stationdetail.StationDetailScreen

private object Routes {
    const val HOME = "home"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val LINES = "lines"
    const val SETTINGS = "settings"
    const val STATION_DETAIL = "station/{stationId}"
    const val PLANNER = "planner?origin={originId}"

    fun station(id: String) = "station/$id"
    fun planner(originId: String? = null) = if (originId != null) "planner?origin=$originId" else "planner"
}

private data class TopLevelDestination(val route: String, val labelRes: Int, val icon: ImageVector)

private val topLevelDestinations = listOf(
    TopLevelDestination(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    TopLevelDestination(Routes.SEARCH, R.string.nav_search, Icons.Filled.Search),
    TopLevelDestination(Routes.FAVORITES, R.string.nav_favorites, Icons.Filled.Star),
    TopLevelDestination(Routes.LINES, R.string.nav_lines, Icons.Filled.Train),
    TopLevelDestination(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings)
)

@Composable
fun TrenYaNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination
    val showBottomBar = topLevelDestinations.any { currentRoute?.hierarchy?.any { h -> h.route == it.route } == true }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { dest ->
                        val selected = currentRoute?.hierarchy?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(stringResource(dest.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                HomeScreen(onStationClick = { navController.navigate(Routes.station(it)) })
            }
            composable(Routes.SEARCH) {
                SearchScreen(onStationClick = { navController.navigate(Routes.station(it)) })
            }
            composable(Routes.FAVORITES) {
                FavoritesScreen(onStationClick = { navController.navigate(Routes.station(it)) })
            }
            composable(Routes.LINES) {
                LinesScreen(onLineClick = { navController.navigate(Routes.SEARCH) })
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
            composable(
                route = Routes.STATION_DETAIL,
                arguments = listOf(navArgument("stationId") { defaultValue = "" })
            ) { entry ->
                val stationId = entry.arguments?.getString("stationId").orEmpty()
                StationDetailScreen(
                    stationId = stationId,
                    onBack = { navController.popBackStack() },
                    onPlanTrip = { originId -> navController.navigate(Routes.planner(originId)) }
                )
            }
            composable(
                route = Routes.PLANNER,
                arguments = listOf(
                    navArgument("originId") {
                        defaultValue = ""
                        nullable = true
                    }
                )
            ) { entry ->
                val originId = entry.arguments?.getString("originId")?.takeIf { it.isNotBlank() }
                TripPlannerScreen(presetOriginId = originId)
            }
        }
    }
}
