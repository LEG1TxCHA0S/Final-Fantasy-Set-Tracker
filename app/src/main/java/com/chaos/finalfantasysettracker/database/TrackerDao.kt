package com.chaos.finalfantasysettracker.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chaos.finalfantasysettracker.model.OwnershipRule
import kotlinx.coroutines.flow.Flow

private const val OWNED_CASE = """
CASE i.ownershipRule
  WHEN 'FOIL_ONLY' THEN IFNULL(o.ownedFoil, 0) = 1
  WHEN 'FOIL_OR_NONFOIL' THEN IFNULL(o.ownedFoil, 0) = 1 OR IFNULL(o.ownedNormal, 0) = 1
  WHEN 'SURGE_FOIL_ONLY' THEN IFNULL(o.ownedSurgeFoil, 0) = 1
  WHEN 'SIGNED_OR_UNSIGNED' THEN IFNULL(o.ownedSigned, 0) = 1 OR IFNULL(o.ownedNormal, 0) = 1
  WHEN 'SIGNED_ONLY' THEN IFNULL(o.ownedSigned, 0) = 1
  ELSE 0
END
"""

@Dao
interface TrackerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParts(parts: List<CollectionPartEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<CollectibleItemEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOwnership(ownership: OwnershipEntity)

    @Query("SELECT COUNT(*) FROM collection_parts")
    suspend fun getPartCount(): Int

    @Query(
        """
        SELECT p.id, p.name, p.description,
        SUM(CASE WHEN $OWNED_CASE THEN 1 ELSE 0 END) as ownedCount,
        COUNT(i.id) as totalCount
        FROM collection_parts p
        LEFT JOIN collectible_items i ON i.partId = p.id
        LEFT JOIN ownership o ON o.itemId = i.id
        GROUP BY p.id
        ORDER BY p.displayOrder ASC
        """
    )
    fun observePartProgress(): Flow<List<PartProgressRow>>

    @Query(
        """
        SELECT
        SUM(CASE WHEN $OWNED_CASE THEN 1 ELSE 0 END) as ownedCount,
        COUNT(i.id) as totalCount
        FROM collectible_items i
        LEFT JOIN ownership o ON o.itemId = i.id
        """
    )
    fun observeOverviewProgress(): Flow<OverviewProgressRow>

    @Query(
        """
        SELECT i.id, i.partId, i.name, i.details, i.ownershipRule,
               IFNULL(o.ownedNormal, 0) as ownedNormal,
               IFNULL(o.ownedFoil, 0) as ownedFoil,
               IFNULL(o.ownedSurgeFoil, 0) as ownedSurgeFoil,
               IFNULL(o.ownedSigned, 0) as ownedSigned
        FROM collectible_items i
        LEFT JOIN ownership o ON o.itemId = i.id
        WHERE i.partId = :partId
        """
    )
    fun observeItemsForPart(partId: Long): Flow<List<ItemStatusRow>>
}

data class PartProgressRow(
    val id: Long,
    val name: String,
    val description: String,
    val ownedCount: Int,
    val totalCount: Int
)

data class OverviewProgressRow(
    val ownedCount: Int,
    val totalCount: Int
)

data class ItemStatusRow(
    val id: Long,
    val partId: Long,
    val name: String,
    val details: String,
    val ownershipRule: OwnershipRule,
    val ownedNormal: Boolean,
    val ownedFoil: Boolean,
    val ownedSurgeFoil: Boolean,
    val ownedSigned: Boolean
)
