package com.smartcodeapps.constructionprizewheel.domain

const val MAX_ACTIVE_PRIZES = 8

data class Prize(
    val id: Long,
    val name: String,
    val description: String?,
    val imagePath: String?,
    val probability: Int,
    val active: Boolean,
    val wheelOrder: Int?
)

fun List<Prize>.wheelPrizes(): List<Prize> =
    filter { it.active }.sortedBy { it.wheelOrder ?: Int.MAX_VALUE }
