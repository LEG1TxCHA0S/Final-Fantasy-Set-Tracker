package com.chaos.finalfantasysettracker.data

import android.content.Context
import com.chaos.finalfantasysettracker.database.AppDatabase
import com.chaos.finalfantasysettracker.database.SeedDataInitializer
import com.chaos.finalfantasysettracker.repository.CollectionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val repository = CollectionRepository(
        dao = database.trackerDao(),
        assetMetadataDataSource = AssetCardMetadataDataSource(context)
    )

    init {
        appScope.launch {
            SeedDataInitializer(context, database.trackerDao()).seedIfEmpty()
        }
    }
}
