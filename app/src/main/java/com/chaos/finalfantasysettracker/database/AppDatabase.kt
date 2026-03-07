package com.chaos.finalfantasysettracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.chaos.finalfantasysettracker.model.OwnershipRule

@Database(
    entities = [CollectionPartEntity::class, CollectibleItemEntity::class, OwnershipEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackerDao(): TrackerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "final_fantasy_tracker.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class RoomConverters {
    @TypeConverter
    fun ruleToString(rule: OwnershipRule): String = rule.name

    @TypeConverter
    fun stringToRule(value: String): OwnershipRule = OwnershipRule.valueOf(value)
}
