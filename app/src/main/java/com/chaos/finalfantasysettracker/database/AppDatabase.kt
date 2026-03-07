package com.chaos.finalfantasysettracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.chaos.finalfantasysettracker.model.CollectionPartType
import com.chaos.finalfantasysettracker.model.FinishRequirement
import com.chaos.finalfantasysettracker.model.ItemType
import com.chaos.finalfantasysettracker.model.VariantType

@Database(
    entities = [CollectionPartEntity::class, CollectibleItemEntity::class],
    version = 3,
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
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}

class RoomConverters {
    @TypeConverter
    fun partTypeToString(value: CollectionPartType): String = value.name

    @TypeConverter
    fun stringToPartType(value: String): CollectionPartType = CollectionPartType.valueOf(value)

    @TypeConverter
    fun itemTypeToString(value: ItemType): String = value.name

    @TypeConverter
    fun stringToItemType(value: String): ItemType = ItemType.valueOf(value)

    @TypeConverter
    fun finishRequirementToString(value: FinishRequirement): String = value.name

    @TypeConverter
    fun stringToFinishRequirement(value: String): FinishRequirement = FinishRequirement.valueOf(value)

    @TypeConverter
    fun variantTypeToString(value: VariantType): String = value.name

    @TypeConverter
    fun stringToVariantType(value: String): VariantType = VariantType.valueOf(value)
}
