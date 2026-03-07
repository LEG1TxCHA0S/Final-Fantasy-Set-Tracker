package com.chaos.finalfantasysettracker.database

import android.content.Context
import com.chaos.finalfantasysettracker.data.importer.ChecklistAssetImporter

class SeedDataInitializer(
    private val context: Context,
    private val dao: TrackerDao
) {
    suspend fun seedIfEmpty() {
        ChecklistAssetImporter(context, dao).importIfEmpty()
    }
}
