package com.smartcodeapps.constructionprizewheel.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PrizeEntity::class, SpinRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prizeDao(): PrizeDao
    abstract fun spinRecordDao(): SpinRecordDao
}
