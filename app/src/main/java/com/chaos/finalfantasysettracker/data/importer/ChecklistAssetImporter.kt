package com.chaos.finalfantasysettracker.data.importer

import android.content.Context
import android.util.Log
import com.chaos.finalfantasysettracker.database.CollectibleItemEntity
import com.chaos.finalfantasysettracker.database.CollectionPartEntity
import com.chaos.finalfantasysettracker.database.TrackerDao
import com.chaos.finalfantasysettracker.model.CollectionPartType
import com.chaos.finalfantasysettracker.model.FinishRequirement
import com.chaos.finalfantasysettracker.model.ItemType
import com.chaos.finalfantasysettracker.model.VariantType
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

class ChecklistAssetImporter(
    private val context: Context,
    private val dao: TrackerDao
) {
    companion object {
        private const val TAG = "ChecklistAssetImporter"
    }

    suspend fun importIfEmpty() {
        if (dao.getPartCount() > 0) return

        val payloads = loadPayloads()
        val partIds = dao.insertParts(payloads.map { it.toPartEntity() })

        val items = buildList {
            payloads.forEachIndexed { index, payload ->
                val partId = partIds[index]
                addAll(payload.items.map { it.toEntity(partId) })
            }
        }
        logImportDebug(items)
        dao.insertItems(items)

        val afinInserted = items.count { it.setCode.equals(CollectionPartType.AFIN.name, ignoreCase = true) }
        val aficInserted = items.count { it.setCode.equals(CollectionPartType.AFIC.name, ignoreCase = true) }
        val wfinInserted = items.count { it.setCode.equals(CollectionPartType.WFIN.name, ignoreCase = true) }
        Log.d(TAG, "[DB_INSERT] importIfEmpty inserted AFIN=$afinInserted AFIC=$aficInserted WFIN=$wfinInserted")
    }

    suspend fun repairAfinAficIfNeeded() {
        val payloads = loadPayloads()
            .filter { it.partType == CollectionPartType.AFIN || it.partType == CollectionPartType.AFIC }

        payloads.forEach { payload ->
            val partId = dao.getPartIdByType(payload.partType) ?: return@forEach
            val expectedItems = payload.items
            val currentCount = dao.getItemCountForPart(partId)

            if (currentCount == expectedItems.size) {
                Log.d(TAG, "[AF_FIX] ${payload.partType.name} already aligned count=$currentCount")
                return@forEach
            }

            val ownershipByKey = dao.getOwnershipSnapshotsForPart(partId)
                .groupBy { "${it.setCode.orEmpty().uppercase()}::${it.collectorNumber.orEmpty()}" }
                .mapValues { (_, rows) -> rows.any { it.owned } }

            dao.deleteItemsForPart(partId)

            val repairedItems = expectedItems.map { item ->
                val key = "${item.setCode.orEmpty().uppercase()}::${item.collectorNumber.orEmpty()}"
                item.copy(owned = ownershipByKey[key] == true).toEntity(partId)
            }
            dao.insertItems(repairedItems)

            Log.d(
                TAG,
                "[AF_FIX] rebuilt ${payload.partType.name}: oldCount=$currentCount expected=${expectedItems.size} inserted=${repairedItems.size}"
            )
        }
    }

    suspend fun repairWfinIfNeeded() {
        val payload = loadPayloads().firstOrNull { it.partType == CollectionPartType.WFIN } ?: return
        val partId = dao.getPartIdByType(CollectionPartType.WFIN) ?: return
        val expectedItems = payload.items
        val currentCount = dao.getItemCountForPart(partId)

        if (currentCount == expectedItems.size) {
            Log.d(TAG, "[WF_FIX] WFIN already aligned count=$currentCount")
            return
        }

        val ownershipByKey = dao.getOwnershipSnapshotsForPart(partId)
            .groupBy { "${it.setCode.orEmpty().uppercase()}::${it.collectorNumber.orEmpty()}" }
            .mapValues { (_, rows) -> rows.any { it.owned } }

        dao.deleteItemsForPart(partId)

        val repairedItems = expectedItems.map { item ->
            val key = "${item.setCode.orEmpty().uppercase()}::${item.collectorNumber.orEmpty()}"
            item.copy(owned = ownershipByKey[key] == true).toEntity(partId)
        }
        dao.insertItems(repairedItems)

        Log.d(TAG, "[WF_FIX] rebuilt WFIN: oldCount=$currentCount expected=${expectedItems.size} inserted=${repairedItems.size}")
    }

    suspend fun backfillImageMetadata() {
        val payloads = loadPayloads()
        val entries = payloads.flatMap { it.items }
        entries.forEach { item ->
            dao.updateScryfallMetadataByChecklistId(
                checklistId = item.checklistId,
                scryfallId = item.scryfallId,
                imageUrlSmall = item.imageUrlSmall,
                imageUrlNormal = item.imageUrlNormal,
                imageUrlLarge = item.imageUrlLarge
            )
        }
        Log.d(TAG, "Backfilled image metadata from assets for ${entries.size} checklist entries")
    }

    private fun loadPayloads(): List<ChecklistAssetPayload> =
        loadFinalFantasySets(context).sets.mapNotNull { set ->
            val partType = runCatching { CollectionPartType.valueOf(set.setCode) }.getOrNull() ?: return@mapNotNull null
            ChecklistAssetPayload(
                partType = partType,
                partName = set.setName.ifBlank { partType.displayName() },
                partDescription = "Imported from ${set.setCode} slim checklist",
                displayOrder = partType.defaultDisplayOrder(),
                items = set.cards.mapIndexed { index, card -> card.toChecklistItemPayload(partType, index) }
            )
        }.sortedBy { it.displayOrder }

    private fun SlimCard.toChecklistItemPayload(partType: CollectionPartType, index: Int): ChecklistItemPayload {
        val normalizedSetCode = setCode.ifBlank { partType.name }
        val normalizedName = name.ifBlank { printedName ?: "Unknown Card" }
        val normalizedCollectorNumber = number.ifBlank { null }
        val resolvedImageUrl = imageUrl ?: scryfallId?.toScryfallImageUrl("normal")

        return ChecklistItemPayload(
            checklistId = id.ifBlank {
                "${normalizedSetCode.lowercase()}-${normalizedCollectorNumber ?: "idx$index"}-${normalizedName.lowercase().replace(' ', '-')}"
            },
            name = normalizedName,
            setCode = normalizedSetCode,
            collectorNumber = normalizedCollectorNumber,
            itemType = partType.inferItemType(type, sourceKind),
            finishRequirement = partType.defaultFinishRequirement(),
            variantType = partType.defaultVariantType(),
            promoSource = promoTypes?.joinToString(", "),
            owned = false,
            scryfallId = scryfallId,
            imageUrlSmall = resolvedImageUrl,
            imageUrlNormal = resolvedImageUrl,
            imageUrlLarge = resolvedImageUrl,
            priceUsd = price?.toString(),
            priceUsdFoil = null,
            rarity = rarity,
            manaCost = null,
            typeLine = type
        )
    }

    private fun logImportDebug(items: List<CollectibleItemEntity>) {
        val withScryfall = items.count { !it.scryfallId.isNullOrBlank() }
        val withImages = items.count { !it.imageUrlNormal.isNullOrBlank() }
        Log.d(TAG, "[IMPORT] totals items=${items.size}, withScryfallId=$withScryfall, withImages=$withImages")
    }

    private fun String.toScryfallImageUrl(version: String): String =
        "https://api.scryfall.com/cards/$this?format=image&version=$version"

    private fun ChecklistAssetPayload.toPartEntity() = CollectionPartEntity(
        type = partType,
        name = partName,
        description = partDescription,
        displayOrder = displayOrder
    )

    private fun ChecklistItemPayload.toEntity(partId: Long) = CollectibleItemEntity(
        partId = partId,
        checklistId = checklistId,
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

    private fun CollectionPartType.defaultDisplayOrder(): Int = when (this) {
        CollectionPartType.FIN -> 1
        CollectionPartType.FCA -> 2
        CollectionPartType.FIC -> 3
        CollectionPartType.AFIN -> 4
        CollectionPartType.AFIC -> 5
        CollectionPartType.PFIN -> 6
        CollectionPartType.PSS5 -> 7
        CollectionPartType.RFIN -> 8
        CollectionPartType.WFIN -> 9
    }

    private fun CollectionPartType.displayName(): String = when (this) {
        CollectionPartType.FIN -> "Final Fantasy"
        CollectionPartType.FCA -> "Final Fantasy Through the Ages"
        CollectionPartType.FIC -> "Final Fantasy Commander"
        CollectionPartType.AFIN -> "Final Fantasy Art Series"
        CollectionPartType.AFIC -> "Final Fantasy Scene Box"
        CollectionPartType.PFIN -> "Final Fantasy Promos"
        CollectionPartType.PSS5 -> "Final Fantasy Standard Showdown"
        CollectionPartType.RFIN -> "Final Fantasy Regional Promos"
        CollectionPartType.WFIN -> "FIN Asia WPN Promo Tokens"
    }

    private fun CollectionPartType.defaultFinishRequirement(): FinishRequirement = when (this) {
        CollectionPartType.FIN, CollectionPartType.AFIN, CollectionPartType.AFIC, CollectionPartType.WFIN -> FinishRequirement.FOIL_ONLY
        CollectionPartType.FIC -> FinishRequirement.SURGE_FOIL_ONLY
        CollectionPartType.FCA, CollectionPartType.PFIN, CollectionPartType.PSS5, CollectionPartType.RFIN -> FinishRequirement.FOIL_OR_NONFOIL
    }

    private fun CollectionPartType.defaultVariantType(): VariantType = when (this) {
        CollectionPartType.AFIN -> VariantType.SIGNED
        CollectionPartType.FIC -> VariantType.PRODUCT
        CollectionPartType.PFIN, CollectionPartType.PSS5, CollectionPartType.RFIN -> VariantType.PROMO
        else -> VariantType.STANDARD
    }

    private fun CollectionPartType.inferItemType(typeLine: String?, sourceKind: String?): ItemType = when {
        this == CollectionPartType.FIC -> ItemType.PRECON
        this == CollectionPartType.AFIN -> ItemType.ART_CARD
        this == CollectionPartType.WFIN || sourceKind.equals("tokens", ignoreCase = true) || typeLine.orEmpty().contains("token", ignoreCase = true) -> ItemType.TOKEN
        this == CollectionPartType.PFIN || this == CollectionPartType.PSS5 || this == CollectionPartType.RFIN -> ItemType.PROMO
        else -> ItemType.CARD
    }
}

fun loadFinalFantasySets(context: Context): SlimRoot {
    val json = context.assets.open("final_fantasy_all_slim.json")
        .bufferedReader()
        .use { it.readText() }

    return Gson().fromJson(json, SlimRoot::class.java)
}

data class SlimCard(
    val id: String,
    val name: String,
    val number: String,
    val setCode: String,
    val setName: String,
    val sourceKind: String?,
    val type: String?,
    val rarity: String?,
    val layout: String?,
    val finishes: List<String>?,
    val promoTypes: List<String>?,
    val printedName: String?,
    val scryfallId: String?,
    val imageUrl: String?,
    val price: Double?
)

data class SlimSet(
    val setCode: String,
    val setName: String,
    @SerializedName(value = "cards", alternate = ["items"])
    val cards: List<SlimCard>
)

data class SlimRoot(
    val sets: List<SlimSet>
)

data class ChecklistAssetPayload(
    val partType: CollectionPartType,
    val partName: String,
    val partDescription: String,
    val displayOrder: Int,
    val items: List<ChecklistItemPayload>
)

data class ChecklistItemPayload(
    val checklistId: String,
    val name: String,
    val setCode: String?,
    val collectorNumber: String?,
    val itemType: ItemType,
    val finishRequirement: FinishRequirement,
    val variantType: VariantType,
    val promoSource: String?,
    val owned: Boolean,
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
