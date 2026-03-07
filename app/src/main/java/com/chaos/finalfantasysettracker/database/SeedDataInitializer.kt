package com.chaos.finalfantasysettracker.database

import com.chaos.finalfantasysettracker.model.OwnershipRule

class SeedDataInitializer(private val dao: TrackerDao) {
    suspend fun seedIfEmpty() {
        if (dao.getPartCount() > 0) return

        val partIds = dao.insertParts(
            listOf(
                CollectionPartEntity(name = "FIN Main Set", description = "FIN base set targets in foil or surge foil-only printings", displayOrder = 0),
                CollectionPartEntity(name = "FIN Art Cards", description = "Art cards including signed variants", displayOrder = 1),
                CollectionPartEntity(name = "FCA Through the Ages", description = "FCA cards counted as foil or non-foil", displayOrder = 2),
                CollectionPartEntity(name = "Tokens", description = "Set tokens tracked as foil-only", displayOrder = 3),
                CollectionPartEntity(name = "Commander Precons", description = "Commander precon cards in surge foil", displayOrder = 4),
                CollectionPartEntity(name = "Promos", description = "All Final Fantasy related promo targets", displayOrder = 5)
            )
        )

        val items = mutableListOf<CollectibleItemEntity>()
        items += buildItems(partIds[0], OwnershipRule.FOIL_ONLY, "Cloud, Midgar Mercenary", "Tifa, Martial Artist", "Sephiroth, Fallen Hero", "Aerith, Planet's Voice")
        items += buildItems(partIds[0], OwnershipRule.SURGE_FOIL_ONLY, "Summon: Bahamut", "Limit Break: Omni-Slash")

        items += buildItems(partIds[1], OwnershipRule.SIGNED_OR_UNSIGNED, "Art Card - Cloud", "Art Card - Tifa", "Art Card - Sephiroth", "Art Card - Terra")

        items += buildItems(partIds[2], OwnershipRule.FOIL_OR_NONFOIL, "The Crystal Awakens", "Lightning, Savior", "Buster Sword", "Cecil, Dark Knight")

        items += buildItems(partIds[3], OwnershipRule.FOIL_ONLY, "Hero Token 1/1", "Chocobo Token 2/2", "Esper Token 4/4")

        items += buildItems(partIds[4], OwnershipRule.SURGE_FOIL_ONLY, "Revival Trance (Precon)", "Shinra Arsenal (Precon)", "Crystal Communion (Precon)")

        items += buildItems(partIds[5], OwnershipRule.FOIL_OR_NONFOIL, "Buy-a-Box Promo - Zidane")
        items += buildItems(partIds[5], OwnershipRule.FOIL_ONLY, "WPN Promo - Yuna")
        items += buildItems(partIds[5], OwnershipRule.SIGNED_ONLY, "Artist Signed Promo - Vivi")

        val itemIds = dao.insertItems(items)
        // Seed representative ownership statuses for dashboard behavior.
        itemIds.take(6).forEachIndexed { index, id ->
            dao.upsertOwnership(
                when (index) {
                    0 -> OwnershipEntity(itemId = id, ownedFoil = true)
                    1 -> OwnershipEntity(itemId = id, ownedFoil = true)
                    2 -> OwnershipEntity(itemId = id, ownedFoil = false)
                    3 -> OwnershipEntity(itemId = id, ownedFoil = true)
                    4 -> OwnershipEntity(itemId = id, ownedSurgeFoil = true)
                    else -> OwnershipEntity(itemId = id, ownedSurgeFoil = false)
                }
            )
        }
    }

    private fun buildItems(partId: Long, rule: OwnershipRule, vararg names: String): List<CollectibleItemEntity> {
        return names.map { name ->
            CollectibleItemEntity(partId = partId, name = name, details = "Target: ${rule.name.lowercase().replace('_', ' ')}", ownershipRule = rule)
        }
    }
}
