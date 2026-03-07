package com.chaos.finalfantasysettracker.repository

import com.chaos.finalfantasysettracker.database.ItemStatusRow
import com.chaos.finalfantasysettracker.database.OwnershipEntity
import com.chaos.finalfantasysettracker.database.TrackerDao
import com.chaos.finalfantasysettracker.model.CollectionOverview
import com.chaos.finalfantasysettracker.model.CollectionPartProgress
import com.chaos.finalfantasysettracker.model.CollectibleItemStatus
import com.chaos.finalfantasysettracker.model.OwnershipRule
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
        val entity = when (item.ownershipRule) {
            OwnershipRule.FOIL_ONLY -> OwnershipEntity(itemId = item.id, ownedFoil = owned)
            OwnershipRule.FOIL_OR_NONFOIL -> OwnershipEntity(itemId = item.id, ownedNormal = owned)
            OwnershipRule.SURGE_FOIL_ONLY -> OwnershipEntity(itemId = item.id, ownedSurgeFoil = owned)
            OwnershipRule.SIGNED_OR_UNSIGNED -> OwnershipEntity(itemId = item.id, ownedNormal = owned)
            OwnershipRule.SIGNED_ONLY -> OwnershipEntity(itemId = item.id, ownedSigned = owned)
        }
        dao.upsertOwnership(entity)
    }

    private fun ItemStatusRow.toModel(): CollectibleItemStatus = CollectibleItemStatus(
        id = id,
        partId = partId,
        name = name,
        details = details,
        ownershipRule = ownershipRule,
        ownedNormal = ownedNormal,
        ownedFoil = ownedFoil,
        ownedSurgeFoil = ownedSurgeFoil,
        ownedSigned = ownedSigned
    )
}
