package com.smartcodeapps.constructionprizewheel.security

import com.smartcodeapps.constructionprizewheel.data.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminAuthentication @Inject constructor(private val settings: SettingsRepository) {

    suspend fun hasPin(): Boolean = settings.hasPin.first()

    suspend fun verify(pin: String): Boolean = settings.verifyPin(pin)

    suspend fun setPin(pin: String): Boolean {
        if (!PinPolicy.isValid(pin)) return false
        settings.setPin(pin)
        return true
    }
}
