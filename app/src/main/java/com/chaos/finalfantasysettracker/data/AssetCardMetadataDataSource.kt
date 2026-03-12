package com.chaos.finalfantasysettracker.data

import android.content.Context
import com.google.gson.Gson

class AssetCardMetadataDataSource(context: Context) {
    private val cardById: Map<String, CardAssetMetadata> by lazy {
        val json = context.assets.open("final_fantasy_all_slim.json")
            .bufferedReader()
            .use { it.readText() }
        val root = Gson().fromJson(json, AssetRoot::class.java)
        root.sets.orEmpty()
            .flatMap { set -> set.items.orEmpty() }
            .associateBy(
                keySelector = { it.id.orEmpty() },
                valueTransform = {
                    CardAssetMetadata(
                        id = it.id,
                        setName = it.setName,
                        type = it.type,
                        rarity = it.rarity,
                        layout = it.layout,
                        finishes = it.finishes,
                        promoTypes = it.promoTypes,
                        printedName = it.printedName,
                        sourceKind = it.sourceKind,
                        group = it.group,
                        imageUrl = it.imageUrl,
                        scryfallId = it.scryfallId,
                        price = it.price
                    )
                }
            )
            .filterKeys { it.isNotBlank() }
    }

    fun getByChecklistId(checklistId: String): CardAssetMetadata? = cardById[checklistId]
}

data class CardAssetMetadata(
    val id: String?,
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
    val items: List<AssetItem>?
)

private data class AssetItem(
    val id: String?,
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
