package com.chaos.finalfantasysettracker.repository

import com.chaos.finalfantasysettracker.database.ItemStatusRow
import com.chaos.finalfantasysettracker.database.TrackerDao
import com.chaos.finalfantasysettracker.model.CollectionOverview
import com.chaos.finalfantasysettracker.model.CollectionPartProgress
import com.chaos.finalfantasysettracker.model.CollectibleItemStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CollectionRepository(private val dao: TrackerDao) {
    fun observePartProgress(): Flow<List<CollectionPartProgress>> = dao.observePartProgress().map { rows ->
        rows.map { CollectionPartProgress(it.id, it.name, it.description, it.ownedCount, it.totalCount) }
    }

    fun observeOverview(): Flow<CollectionOverview> = dao.observeOverviewProgress().map { row ->
        CollectionOverview(row.ownedCount, row.totalCount)
    }

    fun observeItemsForPart(partId: Long): Flow<List<CollectibleItemStatus>> = dao.observeItemsForPart(partId).map { rows ->
        rows.map { it.toModel() }
    }

    suspend fun setOwned(item: CollectibleItemStatus, owned: Boolean) {
        dao.setOwned(item.id, owned)
    }

    private fun ItemStatusRow.toModel(): CollectibleItemStatus = CollectibleItemStatus(
        id = id,
        partId = partId,
        name = name,
        setCode = setCode,
        itemType = itemType,
        finishRequirement = finishRequirement,
        variantType = variantType,
        collectorNumber = collectorNumber,
        promoSource = promoSource,
        owned = owned
    )
}
