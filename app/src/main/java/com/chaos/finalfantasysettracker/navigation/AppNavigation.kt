package com.chaos.finalfantasysettracker.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chaos.finalfantasysettracker.TrackerApplication
import com.chaos.finalfantasysettracker.ui.screens.CollectionPartsScreen
import com.chaos.finalfantasysettracker.ui.screens.HomeScreen
import com.chaos.finalfantasysettracker.ui.screens.PartDetailScreen
import com.chaos.finalfantasysettracker.viewmodel.CollectionPartsViewModel
import com.chaos.finalfantasysettracker.viewmodel.HomeViewModel
import com.chaos.finalfantasysettracker.viewmodel.PartDetailViewModel
import com.chaos.finalfantasysettracker.viewmodel.PartDetailViewModelFactory

sealed class Destination(val route: String, val label: String) {
    data object Home : Destination("home", "Home")
    data object Parts : Destination("parts", "Parts")
    data object PartDetail : Destination("parts/{partId}", "Part Detail") {
        fun createRoute(partId: Long) = "parts/$partId"
    }
}

@Composable
fun FinalFantasyTrackerApp() {
    val navController = rememberNavController()
    val tabs = listOf(Destination.Home, Destination.Parts)
    val backstack by navController.currentBackStackEntryAsState()
    val currentRoute = backstack?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(
                    when {
                        currentRoute == Destination.Home.route -> "Collection Dashboard"
                        currentRoute == Destination.Parts.route -> "Collection Parts"
                        else -> "Part Details"
                    }
                )
            })
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (destination == Destination.Home) Icons.Default.Home else Icons.Default.List,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            modifier = Modifier.padding(paddingValues),
            navController = navController,
            startDestination = Destination.Home.route
        ) {
            composable(Destination.Home.route) {
                val vm: HomeViewModel = viewModel(factory = HomeViewModel.factory)
                val state by vm.uiState.collectAsStateWithLifecycle()
                HomeScreen(uiState = state, onPartClick = { navController.navigate(Destination.PartDetail.createRoute(it)) })
            }
            composable(Destination.Parts.route) {
                val vm: CollectionPartsViewModel = viewModel(factory = CollectionPartsViewModel.factory)
                val parts by vm.parts.collectAsStateWithLifecycle()
                CollectionPartsScreen(parts = parts, onPartClick = { navController.navigate(Destination.PartDetail.createRoute(it)) })
            }
            composable(
                route = Destination.PartDetail.route,
                arguments = listOf(navArgument("partId") { type = NavType.LongType })
            ) { entry ->
                val app = LocalContext.current.applicationContext as TrackerApplication
                val partId = entry.arguments?.getLong("partId") ?: 0L
                val vm: PartDetailViewModel = viewModel(factory = PartDetailViewModelFactory(partId, app.container.repository))
                val state by vm.uiState.collectAsStateWithLifecycle()
                PartDetailScreen(
                    uiState = state,
                    onQueryChanged = vm::onQueryChanged,
                    onSortChanged = vm::onSortChanged,
                    onOwnedToggle = vm::onOwnedToggled
                )
            }
        }
    }
}
