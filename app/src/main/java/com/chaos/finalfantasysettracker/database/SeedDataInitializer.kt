package com.chaos.finalfantasysettracker.database

import android.content.Context
import android.util.Log
import com.chaos.finalfantasysettracker.data.importer.ChecklistAssetImporter

class SeedDataInitializer(
    private val context: Context,
    private val dao: TrackerDao
) {
    suspend fun seedIfEmpty() {
        val importer = ChecklistAssetImporter(context, dao)
        importer.importIfEmpty()
        importer.repairAfinAficIfNeeded()
        importer.repairWfinIfNeeded()
        importer.backfillImageMetadata()

        val afinCount = dao.getItemCountByPartType(com.chaos.finalfantasysettracker.model.CollectionPartType.AFIN)
        val aficCount = dao.getItemCountByPartType(com.chaos.finalfantasysettracker.model.CollectionPartType.AFIC)
        val wfinCount = dao.getItemCountByPartType(com.chaos.finalfantasysettracker.model.CollectionPartType.WFIN)
        Log.d(TAG, "[DB_READBACK] Room counts AFIN=$afinCount AFIC=$aficCount WFIN=$wfinCount")

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
