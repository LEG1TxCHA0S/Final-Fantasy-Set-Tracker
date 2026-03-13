package com.chaos.finalfantasysettracker.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chaos.finalfantasysettracker.repository.CardDetailData
import com.chaos.finalfantasysettracker.repository.CollectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CardDetailUiState(
    val loading: Boolean = true,
    val detail: CardDetailData? = null
)

class CardDetailViewModel(
    itemId: Long,
    private val repository: CollectionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CardDetailUiState())
    val uiState: StateFlow<CardDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeCardDetail(itemId).collect { detail ->
                if (detail == null) {
                    _uiState.value = CardDetailUiState(loading = false, detail = null)
                    return@collect
                }

                val livePrice = repository.fetchLiveCardPrice(
                    scryfallId = detail.item.scryfallId,
                    finishes = detail.finishes
                )
                val resolvedPrice = livePrice ?: detail.currentPrice
                val resolvedHistory = repository.buildPlaceholderHistory(resolvedPrice)
                Log.d(
                    TAG,
                    "[DETAIL_VM_PRICE] card=${detail.item.name}, scryfallId=${detail.item.scryfallId}, livePrice=$livePrice, fallbackPrice=${detail.currentPrice}, resolvedPrice=$resolvedPrice, historyPoints=${resolvedHistory.size}"
                )

                _uiState.value = CardDetailUiState(
                    loading = false,
                    detail = detail.copy(
                        currentPrice = resolvedPrice,
                        priceHistory = resolvedHistory
                    )
                )
            }
        }
    }

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

private const val TAG = "CardDetailViewModel"
