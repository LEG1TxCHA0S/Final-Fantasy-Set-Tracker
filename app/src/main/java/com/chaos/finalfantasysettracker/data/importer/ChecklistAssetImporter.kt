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
import org.json.JSONArray
import org.json.JSONObject

class ChecklistAssetImporter(
    private val context: Context,
    private val dao: TrackerDao
) {
    companion object {
        private const val TAG = "ChecklistAssetImporter"
    }

    private val checklistFiles = listOf("FIN", "FCA", "FIC", "AFIN", "AFIC", "PFIN", "PSS5", "RFIN", "WFIN")
        .map { "checklists/$it.json" }

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
        Log.d(TAG, "[DB_INSERT] importIfEmpty inserted AFIN=$afinInserted AFIC=$aficInserted")
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
        checklistFiles.map { parseMtgJsonFile(it) }.sortedBy { it.displayOrder }

    private fun logImportDebug(items: List<CollectibleItemEntity>) {
        items.groupBy { it.setCode ?: "UNKNOWN" }
            .toSortedMap()
            .forEach { (setCode, setItems) ->
                setItems.take(3).forEach { item ->
                    Log.d(
                        TAG,
                        "[IMPORT] set=$setCode name=${item.name}, collector=${item.collectorNumber}, scryfallId=${item.scryfallId}"
                    )
                }
            }

        val bahamut = items.firstOrNull {
            it.name.equals("Summon: Bahamut", ignoreCase = true) &&
                it.setCode.equals("FIN", ignoreCase = true) &&
                it.collectorNumber == "1"
        }
        Log.d(
            TAG,
            "[IMPORT_SANITY] Summon: Bahamut set=FIN collector=1 scryfallId=${bahamut?.scryfallId}"
        )

        val withScryfall = items.count { !it.scryfallId.isNullOrBlank() }
        Log.d(TAG, "[IMPORT] totals items=${items.size}, withScryfallId=$withScryfall")
    }

    private fun parseMtgJsonFile(path: String): ChecklistAssetPayload {
        val json = context.assets.open(path).bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val data = root.optJSONObject("data")
            ?: throw IllegalArgumentException("Invalid MTGJSON file $path: missing data object")

        val setCode = data.optString("code", "").trim().ifBlank {
            throw IllegalArgumentException("Invalid MTGJSON file $path: missing data.code")
        }
        val partType = CollectionPartType.valueOf(setCode)
        val partName = data.optString("name", partType.displayName()).ifBlank { partType.displayName() }
        val partDescription = "Imported from $setCode MTGJSON"
        val cardsCount = (data.optJSONArray("cards") ?: JSONArray()).length()
        val tokensCount = (data.optJSONArray("tokens") ?: JSONArray()).length()
        if (partType == CollectionPartType.AFIN || partType == CollectionPartType.AFIC) {
            Log.d(TAG, "[PARSE_FILE] path=$path set=$setCode cards=$cardsCount tokens=$tokensCount")
        }
        val entries = data.entryArrayFor(partType)

        return ChecklistAssetPayload(
            partType = partType,
            partName = partName,
            partDescription = partDescription,
            displayOrder = partType.defaultDisplayOrder(),
            items = entries.toItemPayloads(partType)
        )
    }

    private fun JSONObject.entryArrayFor(partType: CollectionPartType): JSONArray {
        val cards = optJSONArray("cards") ?: JSONArray()
        val tokens = optJSONArray("tokens") ?: JSONArray()

        return when (partType) {
            CollectionPartType.AFIN,
            CollectionPartType.AFIC,
            CollectionPartType.WFIN -> if (tokens.length() > 0) tokens else cards
            else -> if (cards.length() > 0) cards else tokens
        }
    }

    private fun JSONArray.toItemPayloads(partType: CollectionPartType): List<ChecklistItemPayload> {
        if (partType == CollectionPartType.AFIN || partType == CollectionPartType.AFIC) {
            Log.d(TAG, "[IMPORT_SELECT] ${partType.name} selectedBeforeDedupe=${length()}")
        }

        val mapped = (0 until length()).mapNotNull { index ->
            val obj = getJSONObject(index)
            if (!partType.shouldImportEntry(obj)) return@mapNotNull null

            val name = obj.optString("name", "").trim().ifBlank { "Unknown Card" }
            val collectorNumber = obj.optString("number", "").trim().ifBlank { null }
            val setCode = obj.optString("setCode", partType.name).trim().ifBlank { partType.name }
            val owned = obj.optBoolean("owned", false)

            val identifiers = obj.optJSONObject("identifiers")
            val scryfallId = identifiers?.optString("scryfallId", null)?.ifBlank { null }

            val imageUrlSmall = scryfallId?.toScryfallImageUrl("small")
            val imageUrlNormal = scryfallId?.toScryfallImageUrl("normal")
            val imageUrlLarge = scryfallId?.toScryfallImageUrl("large")

            val rarity = obj.optString("rarity", null)
            val manaCost = obj.optString("manaCost", null)
            val typeLine = obj.optString("type", null)
            val promoTypes = obj.optJSONArray("promoTypes")
            val promoSource = promoTypes?.joinToStringSafe(", ")

            ChecklistItemPayload(
                checklistId = "${setCode.lowercase()}-${collectorNumber ?: "idx$index"}-${name.lowercase().replace(' ', '-')}",
                name = name,
                setCode = setCode,
                collectorNumber = collectorNumber,
                itemType = partType.inferItemType(typeLine, name),
                finishRequirement = partType.defaultFinishRequirement(),
                variantType = partType.defaultVariantType(),
                promoSource = promoSource,
                owned = owned,
                scryfallId = scryfallId,
                imageUrlSmall = imageUrlSmall,
                imageUrlNormal = imageUrlNormal,
                imageUrlLarge = imageUrlLarge,
                priceUsd = null,
                priceUsdFoil = null,
                rarity = rarity,
                manaCost = manaCost,
                typeLine = typeLine
            )
        }

        if (partType == CollectionPartType.AFIN || partType == CollectionPartType.AFIC) {
            Log.d(TAG, "[DEDUPE] ${partType.name} afterDedupe=${mapped.size}")
        }

        return mapped
    }

    private fun String.toScryfallImageUrl(version: String): String =
        "https://api.scryfall.com/cards/$this?format=image&version=$version"

    private fun JSONArray.joinToStringSafe(separator: String): String {
        return (0 until length())
            .mapNotNull { idx -> optString(idx).takeIf { it.isNotBlank() } }
            .joinToString(separator)
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

    private fun CollectionPartType.shouldImportEntry(obj: JSONObject): Boolean {
        if (this != CollectionPartType.AFIN && this != CollectionPartType.AFIC) return true

        val layout = obj.optString("layout", "")
        if (!layout.equals("art_series", ignoreCase = true)) return true

        val side = obj.optString("side", "a")
        return side.equals("a", ignoreCase = true)
    }

    private fun CollectionPartType.inferItemType(typeLine: String?, name: String): ItemType = when {
        this == CollectionPartType.FIC -> ItemType.PRECON
        this == CollectionPartType.AFIN -> ItemType.ART_CARD
        this == CollectionPartType.WFIN || typeLine.orEmpty().contains("token", ignoreCase = true) -> ItemType.TOKEN
        this == CollectionPartType.PFIN || this == CollectionPartType.PSS5 || this == CollectionPartType.RFIN -> ItemType.PROMO
        name.contains("art", ignoreCase = true) -> ItemType.ART_CARD
        else -> ItemType.CARD
    }
}

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
