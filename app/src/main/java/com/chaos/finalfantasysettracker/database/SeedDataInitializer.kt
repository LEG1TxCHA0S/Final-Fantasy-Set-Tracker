package com.chaos.finalfantasysettracker.database

import android.content.Context
import com.chaos.finalfantasysettracker.data.importer.ChecklistAssetImporter

class SeedDataInitializer(
    private val context: Context,
    private val dao: TrackerDao
) {
    suspend fun seedIfEmpty() {
        ChecklistAssetImporter(context, dao).importIfEmpty()
        enforcePromoAndPreconDataRules()
    }

    private suspend fun enforcePromoAndPreconDataRules() {
        // Ensure date-stamped promo variants are never tracked.
        dao.deleteDateStampedPromos()

        // Ensure precon products always resolve to explicit product image URLs.
        dao.updateImageUrlByChecklistId(
            checklistId = "precon-revival-trance",
            imageUrl = "https://media.wizards.com/2025/images/daily/en_cZ8SeUkVcW.webp"
        )
        dao.updateImageUrlByChecklistId(
            checklistId = "precon-limit-break",
            imageUrl = "https://media.wizards.com/2025/images/daily/en_mvpDWrYJFH.webp"
        )
        dao.updateImageUrlByChecklistId(
            checklistId = "precon-counter-blitz",
            imageUrl = "https://media.wizards.com/2025/images/daily/en_xR2tr3W5CE.webp"
        )
        dao.updateImageUrlByChecklistId(
            checklistId = "precon-scions-spellcraft",
            imageUrl = "https://media.wizards.com/2025/images/daily/en_IiHW87Fdi1.webp"
        )
    }
}
