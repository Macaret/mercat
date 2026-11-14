package com.smartcodeapps.constructionprizewheel.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartcodeapps.constructionprizewheel.domain.SpinRecord
import java.time.Instant

@Entity(tableName = "spin_records")
data class SpinRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spunAtEpochMilli: Long,
    val prizeId: Long,
    val prizeName: String
)

fun SpinRecordEntity.toDomain() = SpinRecord(
    id = id,
    spunAt = Instant.ofEpochMilli(spunAtEpochMilli),
    prizeId = prizeId,
    prizeName = prizeName
)

fun SpinRecord.toEntity() = SpinRecordEntity(
    id = id,
    spunAtEpochMilli = spunAt.toEpochMilli(),
    prizeId = prizeId,
    prizeName = prizeName
)
