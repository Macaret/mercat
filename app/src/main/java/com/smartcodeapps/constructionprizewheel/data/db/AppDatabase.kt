package com.smartcodeapps.constructionprizewheel.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PrizeEntity::class, SpinRecordEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prizeDao(): PrizeDao
    abstract fun spinRecordDao(): SpinRecordDao
}

/** Drops the removed redemptionStatus column from spin_records, keeping the existing history. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE spin_records_new (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "spunAtEpochMilli INTEGER NOT NULL, " +
                "prizeId INTEGER NOT NULL, " +
                "prizeName TEXT NOT NULL)"
        )
        db.execSQL(
            "INSERT INTO spin_records_new (id, spunAtEpochMilli, prizeId, prizeName) " +
                "SELECT id, spunAtEpochMilli, prizeId, prizeName FROM spin_records"
        )
        db.execSQL("DROP TABLE spin_records")
        db.execSQL("ALTER TABLE spin_records_new RENAME TO spin_records")
    }
}
