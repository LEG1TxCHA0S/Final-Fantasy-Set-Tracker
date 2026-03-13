package com.chaos.finalfantasysettracker.repository

import android.util.Log
import com.chaos.finalfantasysettracker.data.AssetCardMetadataDataSource
import com.chaos.finalfantasysettracker.data.ScryfallService
import com.chaos.finalfantasysettracker.database.HomeDashboardItemRow
import com.chaos.finalfantasysettracker.database.ItemDetailRow
import com.chaos.finalfantasysettracker.database.ItemStatusRow
import com.chaos.finalfantasysettracker.database.PriceRefreshCandidateRow
import com.chaos.finalfantasysettracker.database.TrackerDao
import com.chaos.finalfantasysettracker.model.CollectionOverview
import com.chaos.finalfantasysettracker.model.CollectionPartProgress
import com.chaos.finalfantasysettracker.model.CollectionPartType
import com.chaos.finalfantasysettracker.model.CollectibleItemStatus
import com.chaos.finalfantasysettracker.model.FinishRequirement
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

data class CachedPrice(
    val price: Double?,
    val fetchedAtMillis: Long
)

data class RarityStat(
    val label: String,
    val owned: Int,
    val total: Int
)

data class HomeStat(
    val label: String,
    val value: String
)

data class PriceDropItem(
    val cardName: String,
    val previousPrice: Double,
    val currentPrice: Double,
    val dropAmount: Double,
    val dropPercent: Double
)

data class HomeDashboardData(
    val totalOwned: Int,
    val totalCards: Int,
    val completionPercent: Float,
    val totalMissing: Int,
    val totalValue: Double?,
    val totalPriceToComplete: Double?,
    val rarityStats: List<RarityStat>,
    val extraStats: List<HomeStat>,
    val biggestPriceDrops: List<PriceDropItem>,
    val priceDropMessage: String
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
    private val assetMetadataDataSource: AssetCardMetadataDataSource? = null,
    private val scryfallService: ScryfallService? = null
) {
    private val priceCacheTtlMs = 6 * 60 * 60 * 1000L
    private val priceRefreshIntervalMs = 12 * 60 * 60 * 1000L
    private val perRequestDelayMs = 75L
    private val livePriceCache = mutableMapOf<String, CachedPrice>()

    suspend fun refreshAllCardPricesIfStale() {
        val now = System.currentTimeMillis()
        val staleBefore = now - priceRefreshIntervalMs
        val candidates = dao.getPriceRefreshCandidates(staleBefore)
        if (candidates.isEmpty()) {
            Log.d(TAG, "[PRICE_REFRESH] no stale candidates")
            return
        }

        Log.d(TAG, "[PRICE_REFRESH] start candidates=${candidates.size}")
        var refreshed = 0
        candidates.forEach { candidate ->
            refreshCandidatePrice(candidate, now)?.let { refreshed++ }
            delay(perRequestDelayMs)
        }
        Log.d(TAG, "[PRICE_REFRESH] complete refreshed=$refreshed total=${candidates.size}")
    }

    private suspend fun refreshCandidatePrice(candidate: PriceRefreshCandidateRow, now: Long): Double? {
        val id = candidate.scryfallId?.trim().orEmpty()
        if (id.isBlank()) return null

        return try {
            val preferFoil = candidate.finishRequirement == FinishRequirement.FOIL_ONLY ||
                candidate.finishRequirement == FinishRequirement.SURGE_FOIL_ONLY
            val fetched = scryfallService?.fetchCardPrice(id, preferFoil)
            val valueToStore = fetched?.let { "%.2f".format(it) }
            dao.updatePriceForItem(
                itemId = candidate.id,
                priceUsd = valueToStore,
                updatedAt = now
            )

            livePriceCache[id] = CachedPrice(
                price = fetched ?: candidate.priceUsd.parsePriceValue(),
                fetchedAtMillis = now
            )

            Log.d(
                TAG,
                "[PRICE_REFRESH] itemId=${candidate.id} scryfallId=$id preferFoil=$preferFoil fetched=$fetched"
            )
            fetched
        } catch (t: Throwable) {
            Log.w(TAG, "[PRICE_REFRESH] failed itemId=${candidate.id} scryfallId=$id error=${t.message}")
            null
        }
    }

    fun observePartProgress(): Flow<List<CollectionPartProgress>> = combine(
        dao.observePartProgress(),
        dao.observeDashboardItems()
    ) { rows, items ->
        val valueByPart = items.groupBy { it.partId }.mapValues { (_, partItems) ->
            val ownedValue = partItems.filter { it.owned }.sumOf { it.effectivePrice() ?: 0.0 }
            val completionCost = partItems.filter { !it.owned }.sumOf { it.effectivePrice() ?: 0.0 }
            ownedValue to completionCost
        }

        rows.map { row ->
            val (ownedValueRaw, completionCostRaw) = valueByPart[row.id] ?: (0.0 to 0.0)
            CollectionPartProgress(
                id = row.id,
                name = row.name,
                description = row.description,
                ownedCount = row.ownedCount,
                totalCount = row.totalCount,
                ownedValue = ownedValueRaw.takeIf { it > 0.0 },
                completionCost = completionCostRaw.takeIf { it > 0.0 }
            )
        }
    }

    fun observeOverview(): Flow<CollectionOverview> = dao.observeOverviewProgress().map { row ->
        CollectionOverview(row.ownedCount, row.totalCount)
    }

    fun observeHomeDashboard(): Flow<HomeDashboardData> = dao.observeDashboardItems().map { rows ->
        val totalCards = rows.size
        val totalOwned = rows.count { it.owned }
        val totalMissing = totalCards - totalOwned
        val completionPercent = if (totalCards == 0) 0f else totalOwned.toFloat() / totalCards

        val ownedPrices = rows.filter { it.owned }.mapNotNull { it.effectivePrice() }
        val totalValue = ownedPrices.takeIf { it.isNotEmpty() }?.sum()

        val missingPrices = rows.filter { !it.owned }.mapNotNull { it.effectivePrice() }
        val totalPriceToComplete = missingPrices.takeIf { it.isNotEmpty() }?.sum()

        val rarityStats = listOf("Mythic", "Rare", "Uncommon", "Common", "Special/Other").map { bucket ->
            val bucketRows = rows.filter { it.rarity.toRarityBucket() == bucket }
            RarityStat(
                label = bucket,
                owned = bucketRows.count { it.owned },
                total = bucketRows.size
            )
        }.filter { it.total > 0 }

        val promoRows = rows.filter { it.partType == CollectionPartType.PROMOS }
        val secretRows = rows.filter { it.partType == CollectionPartType.SECRET_LAIR }
        val artRows = rows.filter { it.partType == CollectionPartType.ART_SERIES || it.partType == CollectionPartType.SCENE_BOX }
        val ownedWithPrice = rows.count { it.owned && it.effectivePrice() != null }

        val highestOwned = rows.filter { it.owned }
            .mapNotNull { row -> row.effectivePrice()?.let { price -> row.name to price } }
            .maxByOrNull { it.second }

        Log.d(TAG, "[VALUATION_HOME] ownedPriced=${ownedPrices.size} missingPriced=${missingPrices.size} totalValue=$totalValue toComplete=$totalPriceToComplete")

        val extraStats = buildList {
            add(HomeStat("Promos", "${promoRows.count { it.owned }} / ${promoRows.size}"))
            add(HomeStat("Secret Lairs", "${secretRows.count { it.owned }} / ${secretRows.size}"))
            add(HomeStat("Art Cards", "${artRows.count { it.owned }} / ${artRows.size}"))
            add(HomeStat("Owned with price", "$ownedWithPrice"))
            highestOwned?.let { (name, price) ->
                add(HomeStat("Highest owned value", "$name (${formatCurrency(price)})"))
            }
        }

        HomeDashboardData(
            totalOwned = totalOwned,
            totalCards = totalCards,
            completionPercent = completionPercent,
            totalMissing = totalMissing,
            totalValue = totalValue,
            totalPriceToComplete = totalPriceToComplete,
            rarityStats = rarityStats,
            extraStats = extraStats,
            biggestPriceDrops = emptyList(),
            priceDropMessage = "Price drop tracking will appear after prices have been refreshed over time."
        )
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

    suspend fun fetchLiveCardPrice(scryfallId: String?, finishes: List<String>): Double? {
        val id = scryfallId?.trim().orEmpty()
        if (id.isBlank()) return null

        val now = System.currentTimeMillis()
        val cached = livePriceCache[id]
        if (cached != null && now - cached.fetchedAtMillis <= priceCacheTtlMs) {
            Log.d(TAG, "[LIVE_PRICE] cache hit id=$id price=${cached.price}")
            return cached.price
        }

        val preferFoil = finishes.any { it.contains("foil", ignoreCase = true) }
        val fetched = scryfallService?.fetchCardPrice(id, preferFoil)
        livePriceCache[id] = CachedPrice(price = fetched, fetchedAtMillis = now)
        Log.d(TAG, "[LIVE_PRICE] cache store id=$id price=$fetched preferFoil=$preferFoil")
        return fetched
    }

    fun buildPlaceholderHistory(price: Double?): List<CardPricePoint> {
        if (price == null) return emptyList()
        return listOf(
            CardPricePoint("D-4", price),
            CardPricePoint("D-3", price),
            CardPricePoint("D-2", price),
            CardPricePoint("D-1", price),
            CardPricePoint("Now", price)
        )
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
            priceLastUpdatedAt = System.currentTimeMillis(),
            rarity = metadata.rarity,
            manaCost = metadata.manaCost,
            typeLine = metadata.typeLine
        )
    }

    private fun HomeDashboardItemRow.effectivePrice(): Double? {
        val cached = scryfallId?.let { livePriceCache[it]?.price }
        return cached ?: priceUsd.parsePriceValue()
    }

    private fun String?.toRarityBucket(): String {
        val normalized = this?.trim()?.lowercase().orEmpty()
        return when {
            normalized == "mythic" || normalized == "mythic rare" -> "Mythic"
            normalized == "rare" -> "Rare"
            normalized == "uncommon" -> "Uncommon"
            normalized == "common" -> "Common"
            else -> "Special/Other"
        }
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

    private fun formatCurrency(value: Double): String = "$" + "%.2f".format(value)

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
