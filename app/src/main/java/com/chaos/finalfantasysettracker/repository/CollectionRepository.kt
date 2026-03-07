package com.chaos.finalfantasysettracker.repository

import com.chaos.finalfantasysettracker.database.ItemStatusRow
import com.chaos.finalfantasysettracker.database.TrackerDao
import com.chaos.finalfantasysettracker.model.CollectionOverview
import com.chaos.finalfantasysettracker.model.CollectionPartProgress
import com.chaos.finalfantasysettracker.model.CollectibleItemStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface ScryfallMetadataDataSource {
    suspend fun lookupBySetAndCollectorNumber(setCode: String, collectorNumber: String): ScryfallMetadata?
    suspend fun lookupByName(name: String): ScryfallMetadata?
}

data class ScryfallMetadata(
    val scryfallId: String?,
    val imageUrlSmall: String?,
    val imageUrlNormal: String?,
    val imageUrlLarge: String?,
    val priceUsd: String?,
    val priceUsdFoil: String?,
    val rarity: String?,
    val manaCost: String?,
    val typeLine: String?
)

class CollectionRepository(
    private val dao: TrackerDao,
    private val scryfallDataSource: ScryfallMetadataDataSource? = null
) {
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

    suspend fun enrichItemFromScryfall(item: CollectibleItemStatus) {
        val source = scryfallDataSource ?: return
        val metadata = when {
            !item.setCode.isNullOrBlank() && !item.collectorNumber.isNullOrBlank() -> {
                source.lookupBySetAndCollectorNumber(item.setCode, item.collectorNumber)
            }
            else -> source.lookupByName(item.name)
        } ?: return

        dao.updateScryfallMetadata(
            itemId = item.id,
            scryfallId = metadata.scryfallId,
            imageUrlSmall = metadata.imageUrlSmall,
            imageUrlNormal = metadata.imageUrlNormal,
            imageUrlLarge = metadata.imageUrlLarge,
            priceUsd = metadata.priceUsd,
            priceUsdFoil = metadata.priceUsdFoil,
            rarity = metadata.rarity,
            manaCost = metadata.manaCost,
            typeLine = metadata.typeLine
        )
    }

    private fun ItemStatusRow.toModel(): CollectibleItemStatus = CollectibleItemStatus(
        id = id,
        checklistId = checklistId,
        partId = partId,
        name = name,
        setCode = setCode,
        itemType = itemType,
        finishRequirement = finishRequirement,
        variantType = variantType,
        collectorNumber = collectorNumber,
        promoSource = promoSource,
        owned = owned,
        scryfallId = scryfallId,
        imageUrlSmall = imageUrlSmall,
        imageUrlNormal = imageUrlNormal,
        imageUrlLarge = imageUrlLarge,
        priceUsd = priceUsd,
        priceUsdFoil = priceUsdFoil,
        rarity = rarity,
        manaCost = manaCost,
        typeLine = typeLine
    )
}
