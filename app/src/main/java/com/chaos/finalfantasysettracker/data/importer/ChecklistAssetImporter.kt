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
        val itemsArray = root.getJSONArray("items")
        val partType = CollectionPartType.valueOf(root.getString("partType"))
        val partName = root.getString("partName")
        val partDescription = root.optString("partDescription", "").ifBlank { partName }

        return ChecklistAssetPayload(
            partType = partType,
            partName = partName,
            partDescription = partDescription,
            displayOrder = root.getInt("displayOrder"),
            items = itemsArray.toItemPayloads()
        )
    }

    private fun JSONArray.toItemPayloads(): List<ChecklistItemPayload> =
        (0 until length()).map { index ->
            val obj = getJSONObject(index)
            ChecklistItemPayload(
                checklistId = obj.getString("id"),
                name = obj.getString("name"),
                setCode = obj.optNullableString("setCode"),
                collectorNumber = obj.optNullableString("collectorNumber"),
                itemType = ItemType.valueOf(obj.getString("itemType")),
                finishRequirement = FinishRequirement.valueOf(obj.getString("finishRequirement")),
                variantType = VariantType.valueOf(obj.getString("variantType")),
                promoSource = obj.optNullableString("promoSource"),
                owned = obj.optBoolean("owned", false)
            )
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
        owned = owned
    )

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).ifBlank { null }
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
    val owned: Boolean
)
