package com.chaos.finalfantasysettracker.data.importer

import android.content.Context
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
    suspend fun importIfEmpty() {
        if (dao.getPartCount() > 0) return

        val files = listOf(
            "checklists/fin_main.json",
            "checklists/fin_signed_art.json",
            "checklists/fca.json",
            "checklists/tokens.json",
            "checklists/precons.json",
            "checklists/promos.json"
        )

        val payloads = files.map { parseChecklistFile(it) }.sortedBy { it.displayOrder }
        val partIds = dao.insertParts(payloads.map { it.toPartEntity() })

        val items = buildList {
            payloads.forEachIndexed { index, payload ->
                val partId = partIds[index]
                addAll(payload.items.map { it.toEntity(partId) })
            }
        }
        dao.insertItems(items)
    }

    private fun parseChecklistFile(path: String): ChecklistAssetPayload {
        val jsonString = context.assets.open(path).bufferedReader().use { it.readText() }
        val root = JSONObject(jsonString)

        val partTypeRaw = root.optString("partType", "").trim()
        require(partTypeRaw.isNotBlank()) {
            "Invalid checklist file $path: missing required field partType"
        }
        val partType = parsePartType(partTypeRaw, path)

        val partName = root.optString("partName", "").trim()
        require(partName.isNotBlank()) {
            "Invalid checklist file $path: missing required field partName"
        }

        val partDescription = root.optString("partDescription", "").ifBlank { partName }
        val displayOrder = if (root.has("displayOrder")) {
            root.optInt("displayOrder", partType.defaultDisplayOrder())
        } else {
            partType.defaultDisplayOrder()
        }

        val itemsArray = root.optJSONArray("items")
            ?: throw IllegalArgumentException("Invalid checklist file $path: missing required field items")

        return ChecklistAssetPayload(
            partType = partType,
            partName = partName,
            partDescription = partDescription,
            displayOrder = displayOrder,
            items = itemsArray.toItemPayloads(path)
        )
    }

    private fun JSONArray.toItemPayloads(path: String): List<ChecklistItemPayload> {
        val invalidEntries = mutableListOf<String>()
        val validEntries = mutableListOf<ChecklistItemPayload>()

        (0 until length()).forEach { index ->
            val obj = getJSONObject(index)
            val result = parseItemPayload(obj, path, index)
            if (result.error != null) {
                invalidEntries += result.error
            } else {
                validEntries += result.item!!
            }
        }

        if (invalidEntries.isNotEmpty()) {
            throw IllegalArgumentException(
                buildString {
                    append("Checklist validation failed for $path:\n")
                    invalidEntries.forEach { append("- $it\n") }
                }.trimEnd()
            )
        }

        return validEntries
    }

    private fun parseItemPayload(obj: JSONObject, path: String, index: Int): ItemParseResult {
        val id = obj.optString("id", "").trim()
        val name = obj.optString("name", "").trim()
        val itemTypeRaw = obj.optString("itemType", "").trim()
        val finishRequirementRaw = obj.optString("finishRequirement", "").trim()
        val variantTypeRaw = obj.optString("variantType", "").trim()

        val missingRequired = mutableListOf<String>()
        if (id.isBlank()) missingRequired += "id"
        if (name.isBlank()) missingRequired += "name"
        if (itemTypeRaw.isBlank()) missingRequired += "itemType"
        if (finishRequirementRaw.isBlank()) missingRequired += "finishRequirement"
        if (variantTypeRaw.isBlank()) missingRequired += "variantType"
        if (!obj.has("owned")) missingRequired += "owned"

        val entryLabel = if (id.isNotBlank()) id else "index=$index"

        if (missingRequired.isNotEmpty()) {
            return ItemParseResult(
                error = "Invalid checklist entry $entryLabel: missing ${missingRequired.joinToString(", ")}"
            )
        }

        val itemType = parseItemType(itemTypeRaw, entryLabel)
        val finishRequirement = parseFinishRequirement(finishRequirementRaw, entryLabel)
        val variantType = parseVariantType(variantTypeRaw, entryLabel)
        val owned = obj.optBoolean("owned", false)

        val setCode = obj.optNullableString("setCode")
        val collectorNumber = obj.optNullableString("collectorNumber")

        if (itemType in IDENTITY_REQUIRES_SET_AND_COLLECTOR) {
            if (setCode.isNullOrBlank()) {
                return ItemParseResult(error = "Invalid checklist entry $entryLabel: missing setCode")
            }
            if (collectorNumber.isNullOrBlank()) {
                return ItemParseResult(error = "Invalid checklist entry $entryLabel: missing collectorNumber")
            }
        }

        return ItemParseResult(
            item = ChecklistItemPayload(
                checklistId = id,
                name = name,
                setCode = setCode,
                collectorNumber = collectorNumber,
                itemType = itemType,
                finishRequirement = finishRequirement,
                variantType = variantType,
                promoSource = obj.optNullableString("promoSource"),
                owned = owned,
                scryfallId = obj.optNullableString("scryfallId"),
                imageUrlSmall = obj.optNullableString("imageUrlSmall"),
                imageUrlNormal = obj.optNullableString("imageUrlNormal"),
                imageUrlLarge = obj.optNullableString("imageUrlLarge"),
                priceUsd = obj.optNullableString("priceUsd"),
                priceUsdFoil = obj.optNullableString("priceUsdFoil"),
                rarity = obj.optNullableString("rarity"),
                manaCost = obj.optNullableString("manaCost"),
                typeLine = obj.optNullableString("typeLine")
            )
        )
    }


    private fun parsePartType(value: String, path: String): CollectionPartType =
        runCatching { CollectionPartType.valueOf(value) }.getOrElse {
            throw IllegalArgumentException("Invalid checklist file $path: unknown partType '$value'")
        }

    private fun parseItemType(value: String, entryLabel: String): ItemType =
        runCatching { ItemType.valueOf(value) }.getOrElse {
            throw IllegalArgumentException("Invalid checklist entry $entryLabel: unknown itemType '$value'")
        }

    private fun parseFinishRequirement(value: String, entryLabel: String): FinishRequirement =
        runCatching { FinishRequirement.valueOf(value) }.getOrElse {
            throw IllegalArgumentException("Invalid checklist entry $entryLabel: unknown finishRequirement '$value'")
        }

    private fun parseVariantType(value: String, entryLabel: String): VariantType =
        runCatching { VariantType.valueOf(value) }.getOrElse {
            throw IllegalArgumentException("Invalid checklist entry $entryLabel: unknown variantType '$value'")
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

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).ifBlank { null }
    }

    private fun CollectionPartType.defaultDisplayOrder(): Int = when (this) {
        CollectionPartType.FIN_MAIN -> 1
        CollectionPartType.FIN_ART_SIGNED -> 2
        CollectionPartType.FCA -> 3
        CollectionPartType.TOKENS -> 4
        CollectionPartType.PRECONS -> 5
        CollectionPartType.PROMOS -> 6
    }

    companion object {
        private val IDENTITY_REQUIRES_SET_AND_COLLECTOR = setOf(
            ItemType.CARD,
            ItemType.ART_CARD,
            ItemType.TOKEN,
            ItemType.PROMO
        )
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

private data class ItemParseResult(
    val item: ChecklistItemPayload? = null,
    val error: String? = null
)
