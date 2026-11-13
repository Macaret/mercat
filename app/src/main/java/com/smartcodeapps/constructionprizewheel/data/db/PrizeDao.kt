package com.smartcodeapps.constructionprizewheel.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PrizeDao {

    @Query("SELECT * FROM prizes ORDER BY id")
    abstract fun observeAll(): Flow<List<PrizeEntity>>

    @Upsert
    protected abstract suspend fun upsertAll(prizes: List<PrizeEntity>)

    @Query("DELETE FROM prizes WHERE id NOT IN (:keepIds)")
    protected abstract suspend fun deleteAllExcept(keepIds: List<Long>)

    @Query("DELETE FROM prizes")
    abstract suspend fun deleteAll()

    @Transaction
    open suspend fun replaceAll(prizes: List<PrizeEntity>) {
        deleteAllExcept(prizes.filter { it.id != 0L }.map { it.id })
        upsertAll(prizes)
    }
}
