package com.smartcodeapps.constructionprizewheel.domain

import java.time.Instant

data class SpinRecord(
    val id: Long,
    val spunAt: Instant,
    val prizeId: Long,
    val prizeName: String
)
