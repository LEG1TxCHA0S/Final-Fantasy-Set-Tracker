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
    val sortOption: ItemSortOption = ItemSortOption.COLLECTOR_NUMBER,
    val items: List<CollectibleItemStatus> = emptyList()
)

class PartDetailViewModel(
    private val partId: Long,
    private val repository: CollectionRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val sortOption = MutableStateFlow(ItemSortOption.COLLECTOR_NUMBER)

    private val collectorComparator = compareBy<CollectibleItemStatus>(
        { it.setCode.orEmpty().uppercase() },
        { it.collectorNumberSortKey().primaryNumberGroup },
        { it.collectorNumberSortKey().number ?: Int.MAX_VALUE },
        { it.collectorNumberSortKey().prefix.uppercase() },
        { it.collectorNumberSortKey().suffix.lowercase() },
        { it.collectorNumber.orEmpty().lowercase() },
        { it.name.lowercase() }
    )

    val uiState: StateFlow<PartDetailUiState> = combine(
        repository.observeItemsForPart(partId),
        query,
        sortOption
    ) { items, searchQuery, sort ->
        val filtered = items.filter { it.name.contains(searchQuery, ignoreCase = true) }
        val sorted = when (sort) {
            ItemSortOption.COLLECTOR_NUMBER -> filtered.sortedWith(collectorComparator)
            ItemSortOption.NAME_ASC -> filtered.sortedBy { it.name }
            ItemSortOption.NAME_DESC -> filtered.sortedByDescending { it.name }
            ItemSortOption.OWNED_FIRST -> filtered.sortedWith(compareByDescending<CollectibleItemStatus> { it.isOwned }.then(collectorComparator))
            ItemSortOption.MISSING_FIRST -> filtered.sortedWith(compareBy<CollectibleItemStatus> { it.isOwned }.then(collectorComparator))
        }
        PartDetailUiState(query = searchQuery, sortOption = sort, items = sorted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PartDetailUiState())

    fun onQueryChanged(newValue: String) = query.update { newValue }

    fun onSortChanged(option: ItemSortOption) = sortOption.update { option }

    fun onOwnedToggled(item: CollectibleItemStatus, owned: Boolean) {
        viewModelScope.launch { repository.setOwned(item, owned) }
    }
}

private val collectorPattern = Regex("^([A-Za-z]*)(\\d+)([A-Za-z]*)$")

private data class CollectorNumberSortKey(
    val primaryNumberGroup: Int,
    val prefix: String,
    val number: Int?,
    val suffix: String
)

private fun CollectibleItemStatus.collectorNumberSortKey(): CollectorNumberSortKey {
    val raw = collectorNumber.orEmpty().trim()
    val match = collectorPattern.matchEntire(raw)
    if (match != null) {
        val (prefix, numberPart, suffix) = match.destructured
        return CollectorNumberSortKey(
            primaryNumberGroup = 0,
            prefix = prefix,
            number = numberPart.toIntOrNull(),
            suffix = suffix
        )
    }

    return CollectorNumberSortKey(
        primaryNumberGroup = 1,
        prefix = raw,
        number = null,
        suffix = ""
    )
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
