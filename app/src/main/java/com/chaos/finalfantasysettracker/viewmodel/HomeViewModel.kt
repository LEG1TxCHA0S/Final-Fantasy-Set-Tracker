package com.chaos.finalfantasysettracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chaos.finalfantasysettracker.model.CollectionOverview
import com.chaos.finalfantasysettracker.model.CollectionPartProgress
import com.chaos.finalfantasysettracker.repository.CollectionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val overview: CollectionOverview = CollectionOverview(0, 0),
    val parts: List<CollectionPartProgress> = emptyList()
)

class HomeViewModel(repository: CollectionRepository) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        repository.observeOverview(),
        repository.observePartProgress()
    ) { overview, parts ->
        HomeUiState(overview = overview, parts = parts)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(ViewModelFactory.repository(this)) }
        }
    }
}
