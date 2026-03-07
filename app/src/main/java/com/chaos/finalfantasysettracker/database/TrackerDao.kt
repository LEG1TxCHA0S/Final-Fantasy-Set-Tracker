package com.chaos.finalfantasysettracker.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chaos.finalfantasysettracker.model.FinishRequirement
import com.chaos.finalfantasysettracker.model.ItemType
import com.chaos.finalfantasysettracker.model.VariantType
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParts(parts: List<CollectionPartEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<CollectibleItemEntity>): List<Long>

    @Query("UPDATE collectible_items SET owned = :owned WHERE id = :itemId")
    suspend fun setOwned(itemId: Long, owned: Boolean)

    @Query("SELECT COUNT(*) FROM collection_parts")
    suspend fun getPartCount(): Int

    @Query(
        """
        SELECT p.id, p.name, p.description,
        SUM(CASE WHEN IFNULL(i.owned, 0) = 1 THEN 1 ELSE 0 END) as ownedCount,
        COUNT(i.id) as totalCount
        FROM collection_parts p
        LEFT JOIN collectible_items i ON i.partId = p.id
        GROUP BY p.id
        ORDER BY p.displayOrder ASC
        """
    )
    fun observePartProgress(): Flow<List<PartProgressRow>>

    @Query(
        """
        SELECT
        SUM(CASE WHEN IFNULL(i.owned, 0) = 1 THEN 1 ELSE 0 END) as ownedCount,
        COUNT(i.id) as totalCount
        FROM collectible_items i
        """
    )
    fun observeOverviewProgress(): Flow<OverviewProgressRow>

    @Query(
        """
        SELECT i.id, i.partId, i.name, i.setCode, i.itemType, i.finishRequirement, i.variantType,
               i.collectorNumber, i.promoSource, i.owned
        FROM collectible_items i
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
    val setCode: String?,
    val itemType: ItemType,
    val finishRequirement: FinishRequirement,
    val variantType: VariantType,
    val collectorNumber: String?,
    val promoSource: String?,
    val owned: Boolean
)
