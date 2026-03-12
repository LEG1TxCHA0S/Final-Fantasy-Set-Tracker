package com.chaos.finalfantasysettracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chaos.finalfantasysettracker.repository.HomeDashboardData
import com.chaos.finalfantasysettracker.repository.CollectionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val dashboard: HomeDashboardData = HomeDashboardData(
        totalOwned = 0,
        totalCards = 0,
        completionPercent = 0f,
        totalMissing = 0,
        totalValue = null,
        rarityStats = emptyList(),
        extraStats = emptyList(),
        biggestPriceDrops = emptyList(),
        priceDropMessage = "Price drop tracking will appear after prices have been refreshed over time."
    )
)

class HomeViewModel(repository: CollectionRepository) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = repository.observeHomeDashboard()
        .map { HomeUiState(dashboard = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(ViewModelFactory.repository(this)) }
        }
    }
}
