package com.smartcodeapps.constructionprizewheel.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartcodeapps.constructionprizewheel.domain.Prize

@Entity(tableName = "prizes")
data class PrizeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String?,
    val imagePath: String?,
    val probability: Int,
    val active: Boolean,
    val wheelOrder: Int?
)

fun PrizeEntity.toDomain() = Prize(id, name, description, imagePath, probability, active, wheelOrder)

fun Prize.toEntity() = PrizeEntity(id, name, description, imagePath, probability, active, wheelOrder)
