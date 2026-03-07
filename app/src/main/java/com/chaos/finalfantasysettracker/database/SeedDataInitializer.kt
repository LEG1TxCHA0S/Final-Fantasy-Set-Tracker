package com.chaos.finalfantasysettracker.database

import com.chaos.finalfantasysettracker.model.CollectionPartType
import com.chaos.finalfantasysettracker.model.FinishRequirement
import com.chaos.finalfantasysettracker.model.ItemType
import com.chaos.finalfantasysettracker.model.VariantType

class SeedDataInitializer(private val dao: TrackerDao) {
    suspend fun seedIfEmpty() {
        if (dao.getPartCount() > 0) return

        val partIds = dao.insertParts(
            listOf(
                CollectionPartEntity(
                    type = CollectionPartType.FIN_MAIN,
                    name = "FIN Main Set",
                    description = "Final Fantasy main set (FIN): foil-only + surge entries",
                    displayOrder = 0
                ),
                CollectionPartEntity(
                    type = CollectionPartType.FIN_ART_SIGNED,
                    name = "FIN Signed Art Cards",
                    description = "Signed art card targets only",
                    displayOrder = 1
                ),
                CollectionPartEntity(
                    type = CollectionPartType.FCA,
                    name = "FCA Through the Ages",
                    description = "FCA cards where foil or non-foil both count",
                    displayOrder = 2
                ),
                CollectionPartEntity(
                    type = CollectionPartType.TOKENS,
                    name = "Tokens",
                    description = "Foil token checklist",
                    displayOrder = 3
                ),
                CollectionPartEntity(
                    type = CollectionPartType.PRECONS,
                    name = "Commander Precons",
                    description = "Product-level precons in surge foil",
                    displayOrder = 4
                ),
                CollectionPartEntity(
                    type = CollectionPartType.PROMOS,
                    name = "Promos",
                    description = "Promo checklist with source and subtype support",
                    displayOrder = 5
                )
            )
        )

        val items = buildList {
            // FIN Main Set
            addAll(
                cards(
                    partId = partIds[0],
                    setCode = "FIN",
                    finishRequirement = FinishRequirement.FOIL_ONLY,
                    variantType = VariantType.STANDARD,
                    entries = listOf(
                        "Cloud, Midgar Mercenary" to "001",
                        "Tifa, Martial Artist" to "024",
                        "Aerith, Planet's Voice" to "057"
                    )
                )
            )
            addAll(
                cards(
                    partId = partIds[0],
                    setCode = "FIN",
                    finishRequirement = FinishRequirement.SURGE_FOIL_ONLY,
                    variantType = VariantType.SURGE,
                    entries = listOf(
                        "Summon: Bahamut" to "201",
                        "Limit Break: Omni-Slash" to "222"
                    )
                )
            )

            // FIN Signed Art Cards only
            addAll(
                artCards(
                    partId = partIds[1],
                    setCode = "FIN",
                    entries = listOf(
                        "Signed Art - Cloud",
                        "Signed Art - Tifa",
                        "Signed Art - Sephiroth",
                        "Signed Art - Terra"
                    )
                )
            )

            // FCA Through the Ages
            addAll(
                cards(
                    partId = partIds[2],
                    setCode = "FCA",
                    finishRequirement = FinishRequirement.FOIL_OR_NONFOIL,
                    variantType = VariantType.STANDARD,
                    entries = listOf(
                        "The Crystal Awakens" to "007",
                        "Lightning, Savior" to "014",
                        "Buster Sword" to "042",
                        "Cecil, Dark Knight" to "066"
                    )
                )
            )

            // Tokens
            addAll(
                tokens(
                    partId = partIds[3],
                    setCode = "FIN",
                    entries = listOf(
                        "Hero Token 1/1" to "T01",
                        "Chocobo Token 2/2" to "T06",
                        "Esper Token 4/4" to "T12"
                    )
                )
            )

            // Precons as product-level collectibles
            addAll(
                precons(
                    partId = partIds[4],
                    entries = listOf(
                        "Revival Trance",
                        "Shinra Arsenal",
                        "Crystal Communion"
                    )
                )
            )

            // Promos with source/subtype
            addAll(
                promos(
                    partId = partIds[5],
                    setCode = "FIN",
                    entries = listOf(
                        PromoSeed("Buy-a-Box Promo - Zidane", "P01", "Buy-a-Box", FinishRequirement.FOIL_OR_NONFOIL),
                        PromoSeed("WPN Promo - Yuna", "P07", "WPN", FinishRequirement.FOIL_ONLY),
                        PromoSeed("Artist Signed Promo - Vivi", "P19", "Convention", FinishRequirement.SIGNED_ONLY)
                    )
                )
            )
        }

        dao.insertItems(
            items.mapIndexed { index, item ->
                item.copy(owned = index % 3 == 0)
            }
        )
    }

    private fun cards(
        partId: Long,
        setCode: String,
        finishRequirement: FinishRequirement,
        variantType: VariantType,
        entries: List<Pair<String, String>>
    ): List<CollectibleItemEntity> =
        entries.map { (name, collectorNumber) ->
            CollectibleItemEntity(
                partId = partId,
                name = name,
                setCode = setCode,
                itemType = ItemType.CARD,
                finishRequirement = finishRequirement,
                variantType = variantType,
                collectorNumber = collectorNumber,
                promoSource = null
            )
        }

    private fun artCards(
        partId: Long,
        setCode: String,
        entries: List<String>
    ): List<CollectibleItemEntity> =
        entries.mapIndexed { index, name ->
            CollectibleItemEntity(
                partId = partId,
                name = name,
                setCode = setCode,
                itemType = ItemType.ART_CARD,
                finishRequirement = FinishRequirement.SIGNED_ONLY,
                variantType = VariantType.SIGNED,
                collectorNumber = "A${index + 1}",
                promoSource = null
            )
        }

    private fun tokens(
        partId: Long,
        setCode: String,
        entries: List<Pair<String, String>>
    ): List<CollectibleItemEntity> =
        entries.map { (name, collectorNumber) ->
            CollectibleItemEntity(
                partId = partId,
                name = name,
                setCode = setCode,
                itemType = ItemType.TOKEN,
                finishRequirement = FinishRequirement.FOIL_ONLY,
                variantType = VariantType.STANDARD,
                collectorNumber = collectorNumber,
                promoSource = null
            )
        }

    private fun precons(partId: Long, entries: List<String>): List<CollectibleItemEntity> =
        entries.map { name ->
            CollectibleItemEntity(
                partId = partId,
                name = name,
                setCode = null,
                itemType = ItemType.PRECON,
                finishRequirement = FinishRequirement.SURGE_FOIL_ONLY,
                variantType = VariantType.PRODUCT,
                collectorNumber = null,
                promoSource = null
            )
        }

    private fun promos(
        partId: Long,
        setCode: String,
        entries: List<PromoSeed>
    ): List<CollectibleItemEntity> =
        entries.map { seed ->
            CollectibleItemEntity(
                partId = partId,
                name = seed.name,
                setCode = setCode,
                itemType = ItemType.PROMO,
                finishRequirement = seed.finishRequirement,
                variantType = VariantType.PROMO,
                collectorNumber = seed.collectorNumber,
                promoSource = seed.promoSource
            )
        }
}

private data class PromoSeed(
    val name: String,
    val collectorNumber: String,
    val promoSource: String,
    val finishRequirement: FinishRequirement
)
