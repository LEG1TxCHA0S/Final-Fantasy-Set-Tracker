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

        logImportDebug(payloads, items)
        dao.insertItems(items)
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

    private fun loadPayloads(): List<ChecklistAssetPayload> {
        val grouped = loadFinalFantasySets(context).toCollectionCategories()

        return grouped.map { category ->
            ChecklistAssetPayload(
                partType = category.code,
                partName = category.name,
                partDescription = "Imported from grouped slim checklist",
                displayOrder = category.code.defaultDisplayOrder(),
                items = category.cards.mapIndexed { index, card -> card.toChecklistItemPayload(category.code, index) }
            )
        }.sortedBy { it.displayOrder }
    }

    private fun SlimCard.toChecklistItemPayload(partType: CollectionPartType, index: Int): ChecklistItemPayload {
        val normalizedSetCode = setCode.ifBlank { "UNK" }
        val normalizedName = name.ifBlank { printedName ?: "Unknown Card" }
        val normalizedCollectorNumber = number.ifBlank { null }

        return ChecklistItemPayload(
            checklistId = id.ifBlank {
                "${normalizedSetCode.lowercase()}-${normalizedCollectorNumber ?: "idx$index"}-${normalizedName.lowercase().replace(' ', '-')}"
            },
            name = normalizedName,
            setCode = normalizedSetCode,
            collectorNumber = normalizedCollectorNumber,
            itemType = partType.inferItemType(type, sourceKind, normalizedSetCode),
            finishRequirement = partType.defaultFinishRequirement(),
            variantType = partType.defaultVariantType(),
            promoSource = promoTypes?.joinToString(", "),
            owned = false,
            scryfallId = scryfallId,
            imageUrlSmall = imageUrl,
            imageUrlNormal = imageUrl,
            imageUrlLarge = imageUrl,
            priceUsd = price?.toString(),
            priceUsdFoil = null,
            rarity = rarity,
            manaCost = null,
            typeLine = type
        )
    }

    private fun logImportDebug(payloads: List<ChecklistAssetPayload>, items: List<CollectibleItemEntity>) {
        payloads.forEach { payload ->
            Log.d(TAG, "[IMPORT_GROUP] ${payload.partType} name=${payload.partName} count=${payload.items.size}")
        }
        val withScryfall = items.count { !it.scryfallId.isNullOrBlank() }
        val withImages = items.count { !it.imageUrlNormal.isNullOrBlank() }
        Log.d(TAG, "[IMPORT] totals items=${items.size}, withScryfallId=$withScryfall, withImages=$withImages")
    }

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
        CollectionPartType.MAIN_SET -> 1
        CollectionPartType.THROUGH_THE_AGES -> 2
        CollectionPartType.COMMANDER -> 3
        CollectionPartType.ART_AND_SCENE -> 4
        CollectionPartType.PROMOS -> 5
        CollectionPartType.SECRET_LAIR -> 6
        CollectionPartType.PROMO_TOKENS -> 7
    }

    private fun CollectionPartType.defaultFinishRequirement(): FinishRequirement = when (this) {
        CollectionPartType.MAIN_SET,
        CollectionPartType.ART_AND_SCENE,
        CollectionPartType.PROMO_TOKENS -> FinishRequirement.FOIL_ONLY

        CollectionPartType.COMMANDER -> FinishRequirement.SURGE_FOIL_ONLY

        CollectionPartType.THROUGH_THE_AGES,
        CollectionPartType.PROMOS,
        CollectionPartType.SECRET_LAIR -> FinishRequirement.FOIL_OR_NONFOIL
    }

    private fun CollectionPartType.defaultVariantType(): VariantType = when (this) {
        CollectionPartType.ART_AND_SCENE -> VariantType.SIGNED
        CollectionPartType.COMMANDER -> VariantType.PRODUCT
        CollectionPartType.PROMOS, CollectionPartType.PROMO_TOKENS -> VariantType.PROMO
        else -> VariantType.STANDARD
    }

    private fun CollectionPartType.inferItemType(typeLine: String?, sourceKind: String?, setCode: String): ItemType = when {
        this == CollectionPartType.COMMANDER -> ItemType.PRECON
        this == CollectionPartType.ART_AND_SCENE -> ItemType.ART_CARD
        this == CollectionPartType.PROMO_TOKENS || sourceKind.equals("tokens", ignoreCase = true) || typeLine.orEmpty().contains("token", ignoreCase = true) -> ItemType.TOKEN
        this == CollectionPartType.PROMOS || setCode == "FFBONUS" -> ItemType.PROMO
        else -> ItemType.CARD
    }
}

fun loadFinalFantasySets(context: Context): SlimRoot {
    val json = context.assets.open("final_fantasy_all_slim.json")
        .bufferedReader()
        .use { it.readText() }

    return Gson().fromJson(json, SlimRoot::class.java)
}

private fun SlimRoot.toCollectionCategories(): List<CollectionCategory> {
    val bySetCode = sets.associateBy { it.setCode.uppercase() }
    val categories = listOf(
        CollectionCategory(
            code = CollectionPartType.MAIN_SET,
            name = "Final Fantasy",
            cards = bySetCode.cardsOf("FIN")
        ),
        CollectionCategory(
            code = CollectionPartType.THROUGH_THE_AGES,
            name = "Through the Ages",
            cards = bySetCode.cardsOf("FCA")
        ),
        CollectionCategory(
            code = CollectionPartType.COMMANDER,
            name = "Final Fantasy Commander",
            cards = bySetCode.cardsOf("FIC", "CTFIC")
        ),
        CollectionCategory(
            code = CollectionPartType.ART_AND_SCENE,
            name = "Art & Scene Cards",
            cards = bySetCode.cardsOf("AFIN", "AFIC")
        ),
        CollectionCategory(
            code = CollectionPartType.PROMOS,
            name = "Promos",
            cards = bySetCode.cardsOf("PFIN", "PSS5", "RFIN", "FFBONUS")
        ),
        CollectionCategory(
            code = CollectionPartType.SECRET_LAIR,
            name = "Secret Lair",
            cards = bySetCode.cardsOf("FFSLD")
        ),
        CollectionCategory(
            code = CollectionPartType.PROMO_TOKENS,
            name = "Promo Tokens",
            cards = bySetCode.cardsOf("WFIN")
        )
    )

    return categories.filter { it.cards.isNotEmpty() }
}

private fun Map<String, SlimSet>.cardsOf(vararg setCodes: String): List<SlimCard> =
    setCodes.flatMap { code ->
        this[code.uppercase()]?.items.orEmpty()
    }

data class CollectionCategory(
    val code: CollectionPartType,
    val name: String,
    val cards: List<SlimCard>
)

data class SlimCard(
    val id: String,
    val name: String,
    val number: String,
    val setCode: String,
    val setName: String?,
    val type: String?,
    val rarity: String?,
    val layout: String?,
    val finishes: List<String>?,
    val promoTypes: List<String>?,
    val printedName: String?,
    val scryfallId: String?,
    val imageUrl: String?,
    val price: Double?,
    val sourceKind: String?,
    val group: String?
)

data class SlimSet(
    val setCode: String,
    val setName: String,
    @SerializedName(value = "items", alternate = ["cards"])
    val items: List<SlimCard>?
)

data class SlimRoot(
    val schemaVersion: Int?,
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
