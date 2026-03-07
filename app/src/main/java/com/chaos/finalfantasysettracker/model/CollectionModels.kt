package com.chaos.finalfantasysettracker.model

enum class OwnershipRule {
    FOIL_ONLY,
    FOIL_OR_NONFOIL,
    SURGE_FOIL_ONLY,
    SIGNED_OR_UNSIGNED,
    SIGNED_ONLY
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
    val partId: Long,
    val name: String,
    val details: String,
    val ownershipRule: OwnershipRule,
    val ownedNormal: Boolean,
    val ownedFoil: Boolean,
    val ownedSurgeFoil: Boolean,
    val ownedSigned: Boolean
) {
    val isOwned: Boolean
        get() = when (ownershipRule) {
            OwnershipRule.FOIL_ONLY -> ownedFoil
            OwnershipRule.FOIL_OR_NONFOIL -> ownedFoil || ownedNormal
            OwnershipRule.SURGE_FOIL_ONLY -> ownedSurgeFoil
            OwnershipRule.SIGNED_OR_UNSIGNED -> ownedSigned || ownedNormal
            OwnershipRule.SIGNED_ONLY -> ownedSigned
        }

    val ruleLabel: String
        get() = when (ownershipRule) {
            OwnershipRule.FOIL_ONLY -> "Foil required"
            OwnershipRule.FOIL_OR_NONFOIL -> "Foil or non-foil"
            OwnershipRule.SURGE_FOIL_ONLY -> "Surge foil required"
            OwnershipRule.SIGNED_OR_UNSIGNED -> "Signed or regular"
            OwnershipRule.SIGNED_ONLY -> "Signed required"
        }
}

enum class ItemSortOption(val label: String) {
    NAME_ASC("Name A-Z"),
    NAME_DESC("Name Z-A"),
    OWNED_FIRST("Owned first"),
    MISSING_FIRST("Missing first")
}
