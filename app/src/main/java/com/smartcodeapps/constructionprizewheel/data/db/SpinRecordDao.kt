package com.smartcodeapps.constructionprizewheel.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SpinRecordDao {

    @Query("SELECT * FROM spin_records ORDER BY spunAtEpochMilli DESC, id DESC")
    fun observeAll(): Flow<List<SpinRecordEntity>>

    @Insert
    suspend fun insert(record: SpinRecordEntity): Long

    @Query("DELETE FROM spin_records")
    suspend fun deleteAll()
}
