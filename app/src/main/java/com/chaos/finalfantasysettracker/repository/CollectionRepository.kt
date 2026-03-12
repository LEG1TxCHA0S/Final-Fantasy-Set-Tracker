package com.chaos.finalfantasysettracker.repository

import android.util.Log
import com.chaos.finalfantasysettracker.data.AssetCardMetadataDataSource
import com.chaos.finalfantasysettracker.database.ItemDetailRow
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

data class CardPricePoint(
    val label: String,
    val value: Double
)

data class CardDetailData(
    val item: CollectibleItemStatus,
    val categoryName: String,
    val setName: String?,
    val printedName: String?,
    val layout: String?,
    val finishes: List<String>,
    val promoTypes: List<String>,
    val sourceKind: String?,
    val group: String?,
    val currentPrice: Double?,
    val priceHistory: List<CardPricePoint>
)

class CollectionRepository(
    private val dao: TrackerDao,
    private val scryfallDataSource: ScryfallMetadataDataSource? = null,
    private val assetMetadataDataSource: AssetCardMetadataDataSource? = null
) {
    fun observePartProgress(): Flow<List<CollectionPartProgress>> = dao.observePartProgress().map { rows ->
        rows.map { CollectionPartProgress(it.id, it.name, it.description, it.ownedCount, it.totalCount) }
    }

    fun observeOverview(): Flow<CollectionOverview> = dao.observeOverviewProgress().map { row ->
        CollectionOverview(row.ownedCount, row.totalCount)
    }

    fun observeItemsForPart(partId: Long): Flow<List<CollectibleItemStatus>> = dao.observeItemsForPart(partId).map { rows ->
        rows.take(5).forEach { row ->
            Log.d(TAG, "[DB_READ] name=${row.name}, storedScryfallId=${row.scryfallId}")
        }
        rows.firstOrNull {
            it.name.equals("Summon: Bahamut", ignoreCase = true) &&
                it.setCode.equals("FIN", ignoreCase = true) &&
                it.collectorNumber == "1"
        }?.let { bahamut ->
            Log.d(TAG, "[DB_SANITY] Summon: Bahamut set=FIN collector=1 storedScryfallId=${bahamut.scryfallId}")
        }

        rows.map { it.toModel() }
    }

    fun observeCardDetail(itemId: Long): Flow<CardDetailData?> = dao.observeItemById(itemId).map { row ->
        row?.let {
            val item = it.toModel()
            val extraById = assetMetadataDataSource?.getByChecklistId(item.checklistId)
            val extra = extraById ?: assetMetadataDataSource?.getBySetAndCollectorNumber(item.setCode, item.collectorNumber)
            val dbPrice = item.priceUsd.parsePriceValue()
            val currentPrice = dbPrice ?: extra?.price
            Log.d(
                TAG,
                "[DETAIL_PRICE] name=${item.name}, checklistId=${item.checklistId}, set=${item.setCode}, collector=${item.collectorNumber}, dbPrice=${item.priceUsd}, parsedDbPrice=$dbPrice, assetPrice=${extra?.price}, resolvedPrice=$currentPrice"
            )
            CardDetailData(
                item = item,
                categoryName = it.partName,
                setName = extra?.setName,
                printedName = extra?.printedName,
                layout = extra?.layout,
                finishes = extra?.finishes.orEmpty(),
                promoTypes = extra?.promoTypes.orEmpty(),
                sourceKind = extra?.sourceKind,
                group = extra?.group,
                currentPrice = currentPrice,
                priceHistory = emptyList()
            )
        }
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

    private fun String?.parsePriceValue(): Double? {
        val raw = this?.trim().orEmpty()
        if (raw.isBlank()) return null
        return raw.toDoubleOrNull()
            ?: raw.replace("$", "").replace(",", "").toDoubleOrNull()
    }

    private fun ItemDetailRow.toModel(): CollectibleItemStatus = CollectibleItemStatus(
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


private const val TAG = "CollectionRepository"
