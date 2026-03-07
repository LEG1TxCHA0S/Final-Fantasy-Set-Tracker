package com.chaos.finalfantasysettracker.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chaos.finalfantasysettracker.model.CollectionPartType
import com.chaos.finalfantasysettracker.model.FinishRequirement
import com.chaos.finalfantasysettracker.model.ItemType
import com.chaos.finalfantasysettracker.model.VariantType

@Entity(tableName = "collection_parts")
data class CollectionPartEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: CollectionPartType,
    val name: String,
    val description: String,
    val displayOrder: Int
)

@Entity(
    tableName = "collectible_items",
    foreignKeys = [
        ForeignKey(
            entity = CollectionPartEntity::class,
            parentColumns = ["id"],
            childColumns = ["partId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["partId"]), Index(value = ["checklistId"], unique = true)]
)
data class CollectibleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val partId: Long,
    val checklistId: String,
    val name: String,
    val setCode: String?,
    val itemType: ItemType,
    val finishRequirement: FinishRequirement,
    val variantType: VariantType,
    val collectorNumber: String?,
    val promoSource: String?,
    val owned: Boolean = false,
    // Scryfall-ready nullable enrichment fields
    val scryfallId: String? = null,
    val imageUrlSmall: String? = null,
    val imageUrlNormal: String? = null,
    val imageUrlLarge: String? = null,
    val priceUsd: String? = null,
    val priceUsdFoil: String? = null,
    val rarity: String? = null,
    val manaCost: String? = null,
    val typeLine: String? = null
)
