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

    @Query(
        """
        UPDATE collectible_items
        SET scryfallId = :scryfallId,
            imageUrlSmall = :imageUrlSmall,
            imageUrlNormal = :imageUrlNormal,
            imageUrlLarge = :imageUrlLarge,
            priceUsd = :priceUsd,
            priceUsdFoil = :priceUsdFoil,
            rarity = :rarity,
            manaCost = :manaCost,
            typeLine = :typeLine
        WHERE id = :itemId
        """
    )
    suspend fun updateScryfallMetadata(
        itemId: Long,
        scryfallId: String?,
        imageUrlSmall: String?,
        imageUrlNormal: String?,
        imageUrlLarge: String?,
        priceUsd: String?,
        priceUsdFoil: String?,
        rarity: String?,
        manaCost: String?,
        typeLine: String?
    )


    @Query(
        """
        SELECT id, name, scryfallId, imageUrlSmall, imageUrlNormal, imageUrlLarge
        FROM collectible_items
        ORDER BY id ASC
        LIMIT :limit
        """
    )
    suspend fun getImageDebugRows(limit: Int): List<ItemImageDebugRow>

    @Query(
        """
        UPDATE collectible_items
        SET scryfallId = COALESCE(:scryfallId, scryfallId),
            imageUrlSmall = COALESCE(:imageUrlSmall, imageUrlSmall),
            imageUrlNormal = COALESCE(:imageUrlNormal, imageUrlNormal),
            imageUrlLarge = COALESCE(:imageUrlLarge, imageUrlLarge)
        WHERE checklistId = :checklistId
        """
    )
    suspend fun updateScryfallMetadataByChecklistId(
        checklistId: String,
        scryfallId: String?,
        imageUrlSmall: String?,
        imageUrlNormal: String?,
        imageUrlLarge: String?
    )

    @Query("SELECT COUNT(*) FROM collection_parts")
    suspend fun getPartCount(): Int

    @Query(
        """
        DELETE FROM collectible_items
        WHERE itemType = 'PROMO'
          AND (
            LOWER(IFNULL(promoSource, '')) IN ('date-stamped', 'date stamped')
            OR LOWER(name) LIKE '%date-stamped%'
            OR LOWER(name) LIKE '%date stamped%'
          )
        """
    )
    suspend fun deleteDateStampedPromos()

    @Query("UPDATE collectible_items SET imageUrlNormal = :imageUrl WHERE checklistId = :checklistId")
    suspend fun updateImageUrlByChecklistId(checklistId: String, imageUrl: String)

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
        SELECT i.id, i.checklistId, i.partId, i.name, i.setCode, i.itemType, i.finishRequirement, i.variantType,
               i.collectorNumber, i.promoSource, i.owned,
               i.scryfallId, i.imageUrlSmall, i.imageUrlNormal, i.imageUrlLarge,
               i.priceUsd, i.priceUsdFoil, i.rarity, i.manaCost, i.typeLine
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
    val checklistId: String,
    val partId: Long,
    val name: String,
    val setCode: String?,
    val itemType: ItemType,
    val finishRequirement: FinishRequirement,
    val variantType: VariantType,
    val collectorNumber: String?,
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


data class ItemImageDebugRow(
    val id: Long,
    val name: String,
    val scryfallId: String?,
    val imageUrlSmall: String?,
    val imageUrlNormal: String?,
    val imageUrlLarge: String?
)
