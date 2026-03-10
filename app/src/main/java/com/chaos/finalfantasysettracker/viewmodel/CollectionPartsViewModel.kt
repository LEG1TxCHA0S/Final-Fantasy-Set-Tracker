package com.chaos.finalfantasysettracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.chaos.finalfantasysettracker.model.CollectionPartProgress
import com.chaos.finalfantasysettracker.repository.CollectionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CollectionPartsViewModel(repository: CollectionRepository) : ViewModel() {
    val parts: StateFlow<List<CollectionPartProgress>> = repository.observePartProgress()
        .map { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { CollectionPartsViewModel(ViewModelFactory.repository(this)) }
        }
    }
}
