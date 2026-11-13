package com.smartcodeapps.constructionprizewheel.domain

import com.smartcodeapps.constructionprizewheel.data.PrizeRepository
import com.smartcodeapps.constructionprizewheel.data.SettingsRepository
import javax.inject.Inject

/** Returns prizes, branding and the PIN to factory state. Spin history is deliberately left alone. */
class RestoreConfigurationUseCase @Inject constructor(
    private val prizeRepository: PrizeRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke() {
        prizeRepository.clearAll()
        settingsRepository.resetAll()
    }
}
