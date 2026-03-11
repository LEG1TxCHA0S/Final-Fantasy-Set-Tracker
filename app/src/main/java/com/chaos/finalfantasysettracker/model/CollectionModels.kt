package com.chaos.finalfantasysettracker.model

enum class CollectionPartType {
    MAIN_SET,
    THROUGH_THE_AGES,
    COMMANDER,
    ART_AND_SCENE,
    PROMOS,
    SECRET_LAIR,
    PROMO_TOKENS
}

enum class ItemType {
    CARD,
    ART_CARD,
    TOKEN,
    PRECON,
    PROMO
}

enum class FinishRequirement {
    FOIL_ONLY,
    SURGE_FOIL_ONLY,
    FOIL_OR_NONFOIL,
    SIGNED_ONLY
}

enum class VariantType {
    STANDARD,
    SIGNED,
    SURGE,
    PRODUCT,
    PROMO
}

data class CollectionPartProgress(
    val id: Long,
    val name: String,
    val description: String,
    val ownedCount: Int,
    val totalCount: Int
) {
    val completionPercentage: Float = if (totalCount == 0) 0f else ownedCount.toFloat() / totalCount
}

data class CollectionOverview(
    val ownedCount: Int,
    val totalCount: Int
) {
    val completionPercentage: Float = if (totalCount == 0) 0f else ownedCount.toFloat() / totalCount
}

data class CollectibleItemStatus(
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
) {
    val isOwned: Boolean = owned

    val ruleLabel: String
        get() = when (finishRequirement) {
            FinishRequirement.FOIL_ONLY -> "Foil required"
            FinishRequirement.SURGE_FOIL_ONLY -> "Surge foil required"
            FinishRequirement.FOIL_OR_NONFOIL -> "Foil or non-foil"
            FinishRequirement.SIGNED_ONLY -> "Signed required"
        }

    val details: String
        get() {
            val tokens = buildList {
                setCode?.let { add(it) }
                collectorNumber?.let { add("#$it") }
                if (itemType == ItemType.PROMO) {
                    promoSource?.let { add(it) }
                }
            }
            return tokens.joinToString(" • ")
        }

    fun resolvedImageUrl(version: ScryfallImageVersion = ScryfallImageVersion.SMALL): String? {
        val directUrl = when (version) {
            ScryfallImageVersion.SMALL -> imageUrlSmall
            ScryfallImageVersion.NORMAL -> imageUrlNormal
            ScryfallImageVersion.LARGE -> imageUrlLarge
        }?.takeIf { it.isNotBlank() }
        if (directUrl != null) return directUrl

        val id = scryfallId?.trim().orEmpty()
        if (id.length < 2) return null

        val sizeSegment = when (version) {
            ScryfallImageVersion.SMALL -> "small"
            ScryfallImageVersion.NORMAL -> "normal"
            ScryfallImageVersion.LARGE -> "large"
        }

        val a = id[0].lowercaseChar()
        val b = id[1].lowercaseChar()
        return "https://cards.scryfall.io/$sizeSegment/front/$a/$b/$id.jpg"
    }
}

enum class ScryfallImageVersion(val value: String) {
    SMALL("small"),
    NORMAL("normal"),
    LARGE("large")
}

enum class OwnershipFilter(val label: String) {
    ALL("All"),
    OWNED("Owned"),
    MISSING("Missing")
}

enum class ItemSortOption(val label: String) {
    COLLECTOR_NUMBER("Collector number"),
    NAME_ASC("Name A-Z"),
    NAME_DESC("Name Z-A"),
    OWNED_FIRST("Owned first"),
    MISSING_FIRST("Missing first")
}
