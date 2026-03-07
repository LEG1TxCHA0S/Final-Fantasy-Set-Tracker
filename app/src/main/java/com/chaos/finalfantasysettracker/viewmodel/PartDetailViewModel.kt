package com.chaos.finalfantasysettracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.chaos.finalfantasysettracker.model.CollectibleItemStatus
import com.chaos.finalfantasysettracker.model.ItemSortOption
import com.chaos.finalfantasysettracker.repository.CollectionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PartDetailUiState(
    val query: String = "",
    val sortOption: ItemSortOption = ItemSortOption.NAME_ASC,
    val items: List<CollectibleItemStatus> = emptyList()
)

class PartDetailViewModel(
    private val partId: Long,
    private val repository: CollectionRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val sortOption = MutableStateFlow(ItemSortOption.NAME_ASC)

    val uiState: StateFlow<PartDetailUiState> = combine(
        repository.observeItemsForPart(partId),
        query,
        sortOption
    ) { items, searchQuery, sort ->
        val filtered = items.filter { it.name.contains(searchQuery, ignoreCase = true) }
        val sorted = when (sort) {
            ItemSortOption.NAME_ASC -> filtered.sortedBy { it.name }
            ItemSortOption.NAME_DESC -> filtered.sortedByDescending { it.name }
            ItemSortOption.OWNED_FIRST -> filtered.sortedWith(compareByDescending<CollectibleItemStatus> { it.isOwned }.thenBy { it.name })
            ItemSortOption.MISSING_FIRST -> filtered.sortedWith(compareBy<CollectibleItemStatus> { it.isOwned }.thenBy { it.name })
        }
        PartDetailUiState(query = searchQuery, sortOption = sort, items = sorted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PartDetailUiState())

    fun onQueryChanged(newValue: String) = query.update { newValue }

    fun onSortChanged(option: ItemSortOption) = sortOption.update { option }

    fun onOwnedToggled(item: CollectibleItemStatus, owned: Boolean) {
        viewModelScope.launch { repository.setOwned(item, owned) }
    }
}

class PartDetailViewModelFactory(
    private val partId: Long,
    private val repository: CollectionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PartDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PartDetailViewModel(partId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
