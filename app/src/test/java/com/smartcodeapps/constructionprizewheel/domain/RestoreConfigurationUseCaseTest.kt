package com.smartcodeapps.constructionprizewheel.domain

import com.smartcodeapps.constructionprizewheel.data.PrizeRepository
import com.smartcodeapps.constructionprizewheel.data.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RestoreConfigurationUseCaseTest {

    private val prizes = mockk<PrizeRepository>()
    private val settings = mockk<SettingsRepository>()

    @Test
    fun `restoring clears the prizes and resets the settings and the pin`() = runTest {
        coEvery { prizes.clearAll() } returns Unit
        coEvery { settings.resetAll() } returns Unit

        RestoreConfigurationUseCase(prizes, settings)()

        coVerifyOrder {
            prizes.clearAll()
            settings.resetAll()
        }
    }
}
