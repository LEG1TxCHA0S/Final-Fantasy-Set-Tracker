package com.chaos.finalfantasysettracker.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.chaos.finalfantasysettracker.model.OwnershipRule

@Entity(tableName = "collection_parts")
data class CollectionPartEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    indices = [Index(value = ["partId"])]
)
data class CollectibleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val partId: Long,
    val name: String,
    val details: String,
    val ownershipRule: OwnershipRule
)

@Entity(
    tableName = "ownership",
    foreignKeys = [
        ForeignKey(
            entity = CollectibleItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["itemId"])]
)
data class OwnershipEntity(
    @PrimaryKey val itemId: Long,
    val ownedNormal: Boolean = false,
    val ownedFoil: Boolean = false,
    val ownedSurgeFoil: Boolean = false,
    val ownedSigned: Boolean = false,
    val updatedAtUtc: Long = System.currentTimeMillis()
)
