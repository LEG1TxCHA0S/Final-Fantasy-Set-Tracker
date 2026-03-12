package com.chaos.finalfantasysettracker.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.chaos.finalfantasysettracker.ui.screens.CardDetailScreen
import com.chaos.finalfantasysettracker.ui.screens.CollectionPartsScreen
import com.chaos.finalfantasysettracker.ui.screens.HomeScreen
import com.chaos.finalfantasysettracker.ui.screens.PartDetailScreen
import com.chaos.finalfantasysettracker.ui.theme.AbyssBlue
import com.chaos.finalfantasysettracker.ui.theme.CrystalBlue
import com.chaos.finalfantasysettracker.ui.theme.CrystalNight
import com.chaos.finalfantasysettracker.ui.theme.MidnightBlue
import com.chaos.finalfantasysettracker.ui.theme.SoftGold
import com.chaos.finalfantasysettracker.viewmodel.CardDetailViewModel
import com.chaos.finalfantasysettracker.viewmodel.CardDetailViewModelFactory
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
    data object CardDetail : Destination("card/{itemId}", "Card Detail") {
        fun createRoute(itemId: Long) = "card/$itemId"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinalFantasyTrackerApp() {
    val navController = rememberNavController()
    val tabs = listOf(Destination.Home, Destination.Parts)
    val backstack by navController.currentBackStackEntryAsState()
    val currentRoute = backstack?.destination?.route

    Scaffold(
        containerColor = CrystalNight,
        topBar = {
            Box(
                modifier = Modifier.background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(MidnightBlue, AbyssBlue, CrystalBlue.copy(alpha = 0.25f))
                    )
                )
            ) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        titleContentColor = SoftGold
                    ),
                    title = {
                        Text(
                            when {
                                currentRoute == Destination.Home.route -> "Collection Dashboard"
                                currentRoute == Destination.Parts.route -> "Collection Parts"
                                else -> "Part Details"
                            }
                        )
                    },
                    navigationIcon = {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = SoftGold,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MidnightBlue) {
                tabs.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = SoftGold,
                            selectedTextColor = SoftGold,
                            indicatorColor = CrystalBlue.copy(alpha = 0.25f),
                            unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary,
                            unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.secondary
                        ),
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
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 4.dp),
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
                    onOwnershipFilterChanged = vm::onOwnershipFilterChanged,
                    onOwnedToggle = vm::onOwnedToggled,
                    onItemClick = { navController.navigate(Destination.CardDetail.createRoute(it.id)) }
                )
            }
            composable(
                route = Destination.CardDetail.route,
                arguments = listOf(navArgument("itemId") { type = NavType.LongType })
            ) { entry ->
                val app = LocalContext.current.applicationContext as TrackerApplication
                val itemId = entry.arguments?.getLong("itemId") ?: 0L
                val vm: CardDetailViewModel = viewModel(factory = CardDetailViewModelFactory(itemId, app.container.repository))
                val state by vm.uiState.collectAsStateWithLifecycle()
                CardDetailScreen(uiState = state, onOwnedToggled = vm::onOwnedToggled)
            }
        }
    }
}
