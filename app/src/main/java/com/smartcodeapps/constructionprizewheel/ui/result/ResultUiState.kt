package com.smartcodeapps.constructionprizewheel.ui.result

import com.smartcodeapps.constructionprizewheel.data.Branding
import com.smartcodeapps.constructionprizewheel.domain.Prize

data class ResultPrize(val name: String, val description: String?, val imagePath: String?)

data class ResultUiState(
    val loaded: Boolean = false,
    val congratsTitle: String = Branding.DEFAULT_CONGRATS_TITLE,
    val congratsMessage: String = Branding.DEFAULT_CONGRATS_MESSAGE,
    val prize: ResultPrize? = null
)

fun buildResultUiState(prizes: List<Prize>, prizeId: Long, branding: Branding): ResultUiState = ResultUiState(
    loaded = true,
    congratsTitle = branding.congratsTitle,
    congratsMessage = branding.congratsMessage,
    prize = prizes.firstOrNull { it.id == prizeId }?.let { ResultPrize(it.name, it.description, it.imagePath) }
)
