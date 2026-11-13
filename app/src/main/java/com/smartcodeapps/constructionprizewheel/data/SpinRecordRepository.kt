package com.smartcodeapps.constructionprizewheel.data

import com.smartcodeapps.constructionprizewheel.data.db.SpinRecordDao
import com.smartcodeapps.constructionprizewheel.data.db.SpinRecordEntity
import com.smartcodeapps.constructionprizewheel.data.db.toDomain
import com.smartcodeapps.constructionprizewheel.domain.Prize
import com.smartcodeapps.constructionprizewheel.domain.RedemptionStatus
import com.smartcodeapps.constructionprizewheel.domain.SpinRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpinRecordRepository @Inject constructor(
    private val dao: SpinRecordDao,
    private val clock: Clock
) {
    fun observeHistory(): Flow<List<SpinRecord>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun recordSpin(winner: Prize): Long = dao.insert(
        SpinRecordEntity(
            spunAtEpochMilli = clock.millis(),
            prizeId = winner.id,
            prizeName = winner.name,
            redemptionStatus = RedemptionStatus.NOT_REDEEMED.name
        )
    )

    suspend fun setRedemptionStatus(recordId: Long, status: RedemptionStatus) =
        dao.setRedemptionStatus(recordId, status.name)

    suspend fun resetHistory() = dao.deleteAll()
}
