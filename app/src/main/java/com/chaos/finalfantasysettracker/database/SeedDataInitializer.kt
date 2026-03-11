package com.chaos.finalfantasysettracker.database

import android.content.Context
import android.util.Log
import com.chaos.finalfantasysettracker.data.importer.ChecklistAssetImporter
import com.chaos.finalfantasysettracker.model.CollectionPartType

class SeedDataInitializer(
    private val context: Context,
    private val dao: TrackerDao
) {
    suspend fun seedIfEmpty() {
        val importer = ChecklistAssetImporter(context, dao)
        importer.importIfEmpty()
        importer.backfillImageMetadata()

        val partCounts = CollectionPartType.entries.joinToString { partType ->
            "$partType=${dao.getItemCountByPartType(partType)}"
        }
        Log.d(TAG, "[DB_READBACK] Room counts $partCounts")

        val rows = dao.getImageDebugRows(limit = 8)
        rows.forEach { row ->
            Log.d(
                TAG,
                "DB image row id=${row.id}, name=${row.name}, scryfallId=${row.scryfallId}, imageSmall=${row.imageUrlSmall}"
            )
        }
    }
}

private const val TAG = "SeedDataInitializer"
