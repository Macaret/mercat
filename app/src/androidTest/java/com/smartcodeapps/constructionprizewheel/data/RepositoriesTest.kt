package com.smartcodeapps.constructionprizewheel.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.smartcodeapps.constructionprizewheel.data.db.AppDatabase
import com.smartcodeapps.constructionprizewheel.data.image.ImageStore
import com.smartcodeapps.constructionprizewheel.domain.ConfigError
import com.smartcodeapps.constructionprizewheel.domain.Prize
import com.smartcodeapps.constructionprizewheel.domain.RedemptionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Clock

@RunWith(AndroidJUnit4::class)
class RepositoriesTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var imageStore: ImageStore
    private lateinit var prizes: PrizeRepository
    private lateinit var spins: SpinRecordRepository
    private lateinit var settings: SettingsRepository
    private lateinit var settingsFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        imageStore = ImageStore(context, Dispatchers.IO)
        prizes = PrizeRepository(db.prizeDao(), imageStore)
        spins = SpinRecordRepository(db.spinRecordDao(), Clock.systemUTC())
        settingsFile = File.createTempFile("settings", ".preferences_pb", context.cacheDir).also { it.delete() }
        settings = SettingsRepository(PreferenceDataStoreFactory.create { settingsFile }, imageStore)
    }

    @After
    fun tearDown() {
        db.close()
        settingsFile.delete()
    }

    private fun prize(name: String, probability: Int, id: Long = 0, order: Int = 0) =
        Prize(id, name, null, "/img/$name.jpg", probability, true, order)

    @Test
    fun validConfigurationIsSavedAndObserved() = runBlocking {
        val result = prizes.saveAll(listOf(prize("A", 60), prize("B", 40, order = 1)))
        assertEquals(SaveResult.Success, result)

        val saved = prizes.observePrizes().first()
        assertEquals(listOf("A", "B"), saved.map { it.name })
        assertEquals(listOf(60, 40), saved.map { it.probability })
    }

    @Test
    fun invalidConfigurationIsRejectedAndNothingIsSaved() = runBlocking {
        val result = prizes.saveAll(listOf(prize("A", 60), prize("B", 50, order = 1)))
        assertEquals(SaveResult.Invalid(listOf(ConfigError.ProbabilityTotalOver100(110))), result)
        assertTrue(prizes.observePrizes().first().isEmpty())
    }

    @Test
    fun prizesMissingFromTheSavedListAreDeleted() = runBlocking {
        prizes.saveAll(listOf(prize("A", 50), prize("B", 50, order = 1)))
        val existing = prizes.observePrizes().first()

        prizes.saveAll(listOf(existing.first().copy(probability = 100)))

        val remaining = prizes.observePrizes().first()
        assertEquals(listOf("A"), remaining.map { it.name })
    }

    private fun imageFile(name: String): File {
        val dir = File(context.filesDir, "prize_images").also { it.mkdirs() }
        return File(dir, name).also { it.writeText("x") }
    }

    @Test
    fun temporaryNegativeIdsAreInsertedAsNewPrizes() = runBlocking {
        val result = prizes.saveAll(listOf(prize("A", 50, id = -1), prize("B", 50, id = -2, order = 1)))
        assertEquals(SaveResult.Success, result)

        val ids = prizes.observePrizes().first().map { it.id }
        assertEquals(2, ids.size)
        assertTrue(ids.all { it > 0 })
    }

    @Test
    fun deletedPrizeImageFileIsRemovedFromStorage() = runBlocking {
        val kept = imageFile("kept.jpg")
        val removed = imageFile("removed.jpg")
        prizes.saveAll(
            listOf(
                Prize(0, "Kept", null, kept.absolutePath, 50, true, 0),
                Prize(0, "Removed", null, removed.absolutePath, 50, true, 1)
            )
        )
        assertTrue(kept.exists() && removed.exists())

        val keptPrize = prizes.observePrizes().first().first { it.name == "Kept" }
        prizes.saveAll(listOf(keptPrize.copy(probability = 100)))

        assertTrue(kept.exists())
        assertFalse(removed.exists())
        kept.delete()
    }

    @Test
    fun imagesNotUsedByAnyPrizeAreCleanedUpOnSave() = runBlocking {
        val orphan = imageFile("orphan.jpg")
        prizes.saveAll(listOf(prize("A", 100)))
        assertFalse(orphan.exists())
    }

    @Test
    fun clearAllRemovesEveryPrizeImage() = runBlocking {
        val file = imageFile("one.jpg")
        prizes.saveAll(listOf(Prize(0, "A", null, file.absolutePath, 100, true, 0)))
        prizes.clearAll()
        assertFalse(file.exists())
    }

    @Test
    fun spinRecordsSurvivePrizeDeletion() = runBlocking {
        prizes.saveAll(listOf(prize("A", 100)))
        val winner = prizes.observePrizes().first().single()
        spins.recordSpin(winner)

        prizes.clearAll()

        val history = spins.observeHistory().first()
        assertEquals(1, history.size)
        assertEquals("A", history.single().prizeName)
        assertEquals(winner.id, history.single().prizeId)
    }

    @Test
    fun redemptionStatusCanBeToggledBothWays() = runBlocking {
        prizes.saveAll(listOf(prize("A", 100)))
        val id = spins.recordSpin(prizes.observePrizes().first().single())
        assertEquals(RedemptionStatus.NOT_REDEEMED, spins.observeHistory().first().single().redemptionStatus)

        spins.setRedemptionStatus(id, RedemptionStatus.REDEEMED)
        assertEquals(RedemptionStatus.REDEEMED, spins.observeHistory().first().single().redemptionStatus)

        spins.setRedemptionStatus(id, RedemptionStatus.NOT_REDEEMED)
        assertEquals(RedemptionStatus.NOT_REDEEMED, spins.observeHistory().first().single().redemptionStatus)
    }

    @Test
    fun resetHistoryDeletesAllRecords() = runBlocking {
        prizes.saveAll(listOf(prize("A", 100)))
        val winner = prizes.observePrizes().first().single()
        spins.recordSpin(winner)
        spins.recordSpin(winner)

        spins.resetHistory()

        assertTrue(spins.observeHistory().first().isEmpty())
    }

    @Test
    fun pinIsVerifiedAndNeverStoredInPlainText() = runBlocking {
        assertFalse(settings.hasPin.first())
        settings.setPin("4821")

        assertTrue(settings.hasPin.first())
        assertTrue(settings.verifyPin("4821"))
        assertFalse(settings.verifyPin("0000"))
        assertFalse(settingsFile.readBytes().toString(Charsets.ISO_8859_1).contains("4821"))
    }

    @Test
    fun resetAllClearsPinAndBranding() = runBlocking {
        settings.setPin("4821")
        settings.updateBranding(Branding(mainTitle = "HELLO"))

        settings.resetAll()

        assertFalse(settings.hasPin.first())
        assertEquals(Branding(), settings.branding.first())
    }

    private fun logoFile(name: String): File {
        val dir = File(context.filesDir, "logo").also { it.mkdirs() }
        return File(dir, name).also { it.writeText("x") }
    }

    @Test
    fun savingBrandingKeepsTheReferencedLogoAndDeletesOlderOnes() = runBlocking {
        val old = logoFile("old.png")
        val current = logoFile("current.png")

        settings.updateBranding(Branding(logoPath = current.absolutePath))

        assertTrue(current.exists())
        assertFalse(old.exists())
        current.delete()
    }

    @Test
    fun usingTheDefaultLogoDeletesEveryLogoFile() = runBlocking {
        val file = logoFile("gone.png")
        settings.updateBranding(Branding(logoPath = null))
        assertFalse(file.exists())
    }

    @Test
    fun resetAllDeletesTheLogoFile() = runBlocking {
        val file = logoFile("reset.png")
        settings.updateBranding(Branding(logoPath = file.absolutePath))
        settings.resetAll()
        assertFalse(file.exists())
    }

    @Test
    fun brandingDefaultsApplyOnFirstRun() = runBlocking {
        assertEquals(Branding(), settings.branding.first())
    }
}
