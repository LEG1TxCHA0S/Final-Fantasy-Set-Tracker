package com.chaos.finalfantasysettracker.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

class AssetCardMetadataDataSource(context: Context) {
    private data class AssetIndex(
        val byId: Map<String, CardAssetMetadata>,
        val bySetAndNumber: Map<String, CardAssetMetadata>
    )

    private val index: AssetIndex by lazy {
        val json = context.assets.open("final_fantasy_all_slim.json")
            .bufferedReader()
            .use { it.readText() }
        val root = Gson().fromJson(json, AssetRoot::class.java)
        val cards = root.sets.orEmpty().flatMap { set -> set.items.orEmpty() }

        val byId = cards
            .map { it.toMetadata() }
            .filter { !it.id.isNullOrBlank() }
            .associateBy { it.id!! }

        val bySetAndNumber = cards
            .map { it.toMetadata() }
            .filter { !it.setCode.isNullOrBlank() && !it.number.isNullOrBlank() }
            .associateBy { keyFor(it.setCode!!, it.number!!) }

        Log.d(TAG, "[ASSET_META] indexed byId=${byId.size}, bySetAndNumber=${bySetAndNumber.size}")
        AssetIndex(byId = byId, bySetAndNumber = bySetAndNumber)
    }

    fun getByChecklistId(checklistId: String): CardAssetMetadata? =
        index.byId[checklistId]

    fun getBySetAndCollectorNumber(setCode: String?, collectorNumber: String?): CardAssetMetadata? {
        if (setCode.isNullOrBlank() || collectorNumber.isNullOrBlank()) return null
        return index.bySetAndNumber[keyFor(setCode, collectorNumber)]
    }

    private fun AssetItem.toMetadata() = CardAssetMetadata(
        id = id,
        setCode = setCode,
        number = number,
        setName = setName,
        type = type,
        rarity = rarity,
        layout = layout,
        finishes = finishes,
        promoTypes = promoTypes,
        printedName = printedName,
        sourceKind = sourceKind,
        group = group,
        imageUrl = imageUrl,
        scryfallId = scryfallId,
        price = price
    )

    private fun keyFor(setCode: String, collectorNumber: String): String =
        "${setCode.trim().uppercase()}::${collectorNumber.trim()}"
}

data class CardAssetMetadata(
    val id: String?,
    val setCode: String?,
    val number: String?,
    val setName: String?,
    val type: String?,
    val rarity: String?,
    val layout: String?,
    val finishes: List<String>?,
    val promoTypes: List<String>?,
    val printedName: String?,
    val sourceKind: String?,
    val group: String?,
    val imageUrl: String?,
    val scryfallId: String?,
    val price: Double?
)

private data class AssetRoot(
    val sets: List<AssetSet>?
)

private data class AssetSet(
    @SerializedName(value = "items", alternate = ["cards"])
    val items: List<AssetItem>?
)

private data class AssetItem(
    val id: String?,
    val setCode: String?,
    val number: String?,
    val setName: String?,
    val type: String?,
    val rarity: String?,
    val layout: String?,
    val finishes: List<String>?,
    val promoTypes: List<String>?,
    val printedName: String?,
    val sourceKind: String?,
    val group: String?,
    val imageUrl: String?,
    val scryfallId: String?,
    val price: Double?
)

private const val TAG = "AssetCardMetadata"
