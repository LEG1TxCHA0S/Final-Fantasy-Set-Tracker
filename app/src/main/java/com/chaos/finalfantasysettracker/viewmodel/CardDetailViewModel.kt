package com.chaos.finalfantasysettracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chaos.finalfantasysettracker.repository.CardDetailData
import com.chaos.finalfantasysettracker.repository.CollectionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CardDetailUiState(
    val loading: Boolean = true,
    val detail: CardDetailData? = null
)

class CardDetailViewModel(
    itemId: Long,
    private val repository: CollectionRepository
) : ViewModel() {
    val uiState: StateFlow<CardDetailUiState> = repository.observeCardDetail(itemId)
        .map { detail -> CardDetailUiState(loading = false, detail = detail) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardDetailUiState())

    fun onOwnedToggled(owned: Boolean) {
        val item = uiState.value.detail?.item ?: return
        viewModelScope.launch { repository.setOwned(item, owned) }
    }
}

class CardDetailViewModelFactory(
    private val itemId: Long,
    private val repository: CollectionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CardDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CardDetailViewModel(itemId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
