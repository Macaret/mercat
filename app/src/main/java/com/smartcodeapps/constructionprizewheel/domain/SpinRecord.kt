package com.smartcodeapps.constructionprizewheel.domain

import java.time.Instant

enum class RedemptionStatus { NOT_REDEEMED, REDEEMED }

data class SpinRecord(
    val id: Long,
    val spunAt: Instant,
    val prizeId: Long,
    val prizeName: String,
    val redemptionStatus: RedemptionStatus = RedemptionStatus.NOT_REDEEMED
)
