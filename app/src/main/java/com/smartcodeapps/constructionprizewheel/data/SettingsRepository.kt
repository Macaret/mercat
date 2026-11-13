package com.smartcodeapps.constructionprizewheel.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.smartcodeapps.constructionprizewheel.data.image.ImageStore
import com.smartcodeapps.constructionprizewheel.security.PinHasher
import com.smartcodeapps.constructionprizewheel.security.PinPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

data class Branding(
    val logoPath: String? = null,
    val mainTitle: String = DEFAULT_MAIN_TITLE,
    val congratsTitle: String = DEFAULT_CONGRATS_TITLE,
    val congratsMessage: String = DEFAULT_CONGRATS_MESSAGE
) {
    companion object {
        const val DEFAULT_MAIN_TITLE = "ÎNVÂRTE ROATA ȘI CÂȘTIGĂ!"
        const val DEFAULT_CONGRATS_TITLE = "FELICITĂRI!"
        const val DEFAULT_CONGRATS_MESSAGE = "AI CÂȘTIGAT!"
    }
}

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val imageStore: ImageStore
) {
    val branding: Flow<Branding> = dataStore.data.map { prefs ->
        Branding(
            logoPath = prefs[LOGO_PATH],
            mainTitle = prefs[MAIN_TITLE] ?: Branding.DEFAULT_MAIN_TITLE,
            congratsTitle = prefs[CONGRATS_TITLE] ?: Branding.DEFAULT_CONGRATS_TITLE,
            congratsMessage = prefs[CONGRATS_MESSAGE] ?: Branding.DEFAULT_CONGRATS_MESSAGE
        )
    }

    val hasPin: Flow<Boolean> = dataStore.data.map { it[PIN_HASH] != null }

    suspend fun updateBranding(branding: Branding) {
        dataStore.edit { prefs ->
            if (branding.logoPath == null) prefs.remove(LOGO_PATH) else prefs[LOGO_PATH] = branding.logoPath
            prefs[MAIN_TITLE] = branding.mainTitle
            prefs[CONGRATS_TITLE] = branding.congratsTitle
            prefs[CONGRATS_MESSAGE] = branding.congratsMessage
        }
        imageStore.deleteLogosExcept(branding.logoPath)
    }

    suspend fun setPin(pin: String) {
        require(PinPolicy.isValid(pin)) { "PIN must be exactly ${PinPolicy.LENGTH} digits" }
        val salt = PinHasher.newSalt()
        val hash = PinHasher.hash(pin, salt)
        dataStore.edit { prefs ->
            prefs[PIN_SALT] = Base64.getEncoder().encodeToString(salt)
            prefs[PIN_HASH] = Base64.getEncoder().encodeToString(hash)
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val prefs = dataStore.data.first()
        val salt = prefs[PIN_SALT] ?: return false
        val hash = prefs[PIN_HASH] ?: return false
        return PinHasher.matches(pin, Base64.getDecoder().decode(salt), Base64.getDecoder().decode(hash))
    }

    suspend fun resetAll() {
        dataStore.edit { it.clear() }
        imageStore.deleteLogo()
    }

    private companion object {
        val LOGO_PATH = stringPreferencesKey("logo_path")
        val MAIN_TITLE = stringPreferencesKey("main_title")
        val CONGRATS_TITLE = stringPreferencesKey("congrats_title")
        val CONGRATS_MESSAGE = stringPreferencesKey("congrats_message")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_HASH = stringPreferencesKey("pin_hash")
    }
}
