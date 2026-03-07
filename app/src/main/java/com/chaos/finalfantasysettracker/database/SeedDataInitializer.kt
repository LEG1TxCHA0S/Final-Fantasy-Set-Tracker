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
                    description = "Final Fantasy main set (foil targets + surge-only entries)",
                    displayOrder = 0
                ),
                CollectionPartEntity(
                    type = CollectionPartType.FIN_ART_SIGNED,
                    name = "FIN Signed Art Cards",
                    description = "Signed art cards only (no unsigned art cards)",
                    displayOrder = 1
                ),
                CollectionPartEntity(
                    type = CollectionPartType.FCA,
                    name = "FCA Through the Ages",
                    description = "Through the Ages cards (foil or non-foil)",
                    displayOrder = 2
                ),
                CollectionPartEntity(
                    type = CollectionPartType.TOKENS,
                    name = "Tokens",
                    description = "Final Fantasy token checklist (foil)",
                    displayOrder = 3
                ),
                CollectionPartEntity(
                    type = CollectionPartType.PRECONS,
                    name = "Commander Precons",
                    description = "Precon products in surge foil",
                    displayOrder = 4
                ),
                CollectionPartEntity(
                    type = CollectionPartType.PROMOS,
                    name = "Promos",
                    description = "Final Fantasy promos with source metadata",
                    displayOrder = 5
                )
            )
        )

        val items = buildList {
            addAll(finMainEntries(partIds[0]))
            addAll(finSignedArtEntries(partIds[1]))
            addAll(fcaEntries(partIds[2]))
            addAll(tokenEntries(partIds[3]))
            addAll(preconEntries(partIds[4]))
            addAll(promoEntries(partIds[5]))
        }

        dao.insertItems(
            items.mapIndexed { index, item ->
                // Mixed initial ownership for dashboard/search/sort validation.
                item.copy(owned = index % 4 == 0)
            }
        )
    }

    private fun finMainEntries(partId: Long): List<CollectibleItemEntity> {
        val foilChecklist = listOf(
            ChecklistSeed("Cloud, Ex-SOLDIER", "001"),
            ChecklistSeed("Tifa Lockhart", "005"),
            ChecklistSeed("Aerith Gainsborough", "013"),
            ChecklistSeed("Barret Wallace", "016"),
            ChecklistSeed("Red XIII", "025"),
            ChecklistSeed("Y'shtola Rhul", "036"),
            ChecklistSeed("Noctis, Prince of Lucis", "041"),
            ChecklistSeed("Lightning, Army of One", "052"),
            ChecklistSeed("Tidus, Zanarkand Hero", "067"),
            ChecklistSeed("Terra Branford", "074"),
            ChecklistSeed("Vivi Ornitier", "086"),
            ChecklistSeed("Sephiroth", "099")
        )

        val surgeOnlyChecklist = listOf(
            ChecklistSeed("Summon: Bahamut", "201"),
            ChecklistSeed("Summon: Knights of the Round", "204"),
            ChecklistSeed("Limit Break: Omnislash", "212"),
            ChecklistSeed("Limit Break: Ultima", "219")
        )

        return buildList {
            addAll(
                foilChecklist.map {
                    card(
                        partId = partId,
                        name = it.name,
                        setCode = "FIN",
                        collectorNumber = it.collectorNumber,
                        finishRequirement = FinishRequirement.FOIL_ONLY,
                        variantType = VariantType.STANDARD
                    )
                }
            )
            addAll(
                surgeOnlyChecklist.map {
                    card(
                        partId = partId,
                        name = it.name,
                        setCode = "FIN",
                        collectorNumber = it.collectorNumber,
                        finishRequirement = FinishRequirement.SURGE_FOIL_ONLY,
                        variantType = VariantType.SURGE
                    )
                }
            )
        }
    }

    private fun finSignedArtEntries(partId: Long): List<CollectibleItemEntity> {
        val signedArtChecklist = listOf(
            ChecklistSeed("Signed Art Card - Cloud", "A01"),
            ChecklistSeed("Signed Art Card - Tifa", "A02"),
            ChecklistSeed("Signed Art Card - Aerith", "A03"),
            ChecklistSeed("Signed Art Card - Sephiroth", "A04"),
            ChecklistSeed("Signed Art Card - Yuna", "A05"),
            ChecklistSeed("Signed Art Card - Tidus", "A06"),
            ChecklistSeed("Signed Art Card - Terra", "A07"),
            ChecklistSeed("Signed Art Card - Vivi", "A08")
        )

        return signedArtChecklist.map {
            CollectibleItemEntity(
                partId = partId,
                name = it.name,
                setCode = "FIN",
                itemType = ItemType.ART_CARD,
                finishRequirement = FinishRequirement.SIGNED_ONLY,
                variantType = VariantType.SIGNED,
                collectorNumber = it.collectorNumber,
                promoSource = null
            )
        }
    }

    private fun fcaEntries(partId: Long): List<CollectibleItemEntity> {
        val fcaChecklist = listOf(
            ChecklistSeed("Warrior of Light", "001"),
            ChecklistSeed("Cecil Harvey", "006"),
            ChecklistSeed("Kain Highwind", "009"),
            ChecklistSeed("Cloud Strife", "017"),
            ChecklistSeed("Squall Leonhart", "024"),
            ChecklistSeed("Zidane Tribal", "031"),
            ChecklistSeed("Tidus", "037"),
            ChecklistSeed("Yuna", "042"),
            ChecklistSeed("Lightning", "053"),
            ChecklistSeed("Clive Rosfield", "066")
        )

        return fcaChecklist.map {
            card(
                partId = partId,
                name = it.name,
                setCode = "FCA",
                collectorNumber = it.collectorNumber,
                finishRequirement = FinishRequirement.FOIL_OR_NONFOIL,
                variantType = VariantType.STANDARD
            )
        }
    }

    private fun tokenEntries(partId: Long): List<CollectibleItemEntity> {
        val tokenChecklist = listOf(
            ChecklistSeed("Hero Token 1/1", "T01"),
            ChecklistSeed("Wizard Token 1/1", "T02"),
            ChecklistSeed("Soldier Token 1/1", "T03"),
            ChecklistSeed("Chocobo Token 2/2", "T05"),
            ChecklistSeed("Moogle Token 1/1", "T08"),
            ChecklistSeed("Esper Token 4/4", "T12")
        )

        return tokenChecklist.map {
            CollectibleItemEntity(
                partId = partId,
                name = it.name,
                setCode = "FIN",
                itemType = ItemType.TOKEN,
                finishRequirement = FinishRequirement.FOIL_ONLY,
                variantType = VariantType.STANDARD,
                collectorNumber = it.collectorNumber,
                promoSource = null
            )
        }
    }

    private fun preconEntries(partId: Long): List<CollectibleItemEntity> {
        val productChecklist = listOf(
            "Scions & Summons",
            "Shinra Soldiers",
            "Limit Break Legends",
            "Crystals & Chaos"
        )

        return productChecklist.map { name ->
            CollectibleItemEntity(
                partId = partId,
                name = name,
                setCode = null,
                itemType = ItemType.PRECON,
                finishRequirement = FinishRequirement.SURGE_FOIL_ONLY,
                variantType = VariantType.PRODUCT,
                collectorNumber = null,
                promoSource = "Commander Deck"
            )
        }
    }

    private fun promoEntries(partId: Long): List<CollectibleItemEntity> {
        // Note: where official promo collector IDs vary by region/event, the source field is canonical
        // and collectorNumber can be updated later without changing structure.
        val promoChecklist = listOf(
            PromoSeed("Buy-a-Box Promo - Zidane", "P01", "Buy-a-Box", FinishRequirement.FOIL_OR_NONFOIL),
            PromoSeed("WPN Promo - Yuna", "P05", "WPN Store Promo", FinishRequirement.FOIL_ONLY),
            PromoSeed("Prerelease Promo - Cloud", "P09", "Prerelease", FinishRequirement.FOIL_ONLY),
            PromoSeed("Bundle Promo - Noctis", "P12", "Bundle", FinishRequirement.FOIL_ONLY),
            PromoSeed("Gift Bundle Promo - Terra", "P14", "Gift Bundle", FinishRequirement.FOIL_ONLY),
            PromoSeed("Festival Promo - Lightning", "P18", "MagicCon / Festival", FinishRequirement.FOIL_OR_NONFOIL),
            PromoSeed("Artist Signed Promo - Vivi", "P22", "Artist Event", FinishRequirement.SIGNED_ONLY)
        )

        return promoChecklist.map { seed ->
            CollectibleItemEntity(
                partId = partId,
                name = seed.name,
                setCode = "FIN",
                itemType = ItemType.PROMO,
                finishRequirement = seed.finishRequirement,
                variantType = VariantType.PROMO,
                collectorNumber = seed.collectorNumber,
                promoSource = seed.promoSource
            )
        }
    }

    private fun card(
        partId: Long,
        name: String,
        setCode: String,
        collectorNumber: String?,
        finishRequirement: FinishRequirement,
        variantType: VariantType
    ) = CollectibleItemEntity(
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

private data class ChecklistSeed(
    val name: String,
    val collectorNumber: String?
)

private data class PromoSeed(
    val name: String,
    val collectorNumber: String,
    val promoSource: String,
    val finishRequirement: FinishRequirement
)
