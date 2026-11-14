package com.smartcodeapps.constructionprizewheel.ui.admin.prizes

import android.net.Uri
import com.smartcodeapps.constructionprizewheel.data.PrizeRepository
import com.smartcodeapps.constructionprizewheel.data.SaveResult
import com.smartcodeapps.constructionprizewheel.data.image.ImageStore
import com.smartcodeapps.constructionprizewheel.domain.ConfigError
import com.smartcodeapps.constructionprizewheel.domain.Prize
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PrizeEditorViewModelTest {

    private fun prize(id: Long, name: String, probability: Int = 0, active: Boolean = false, order: Int? = null) =
        Prize(id, name, null, "/img/$id.jpg", probability, active, if (active) order else null)

    private val saved = listOf(
        prize(1, "Hammer", 60, active = true, order = 1),
        prize(2, "Gloves", 40, active = true, order = 2),
        prize(3, "Drill")
    )

    private val stored = MutableStateFlow(saved)
    private val repository = mockk<PrizeRepository>()
    private val imageStore = mockk<ImageStore>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { repository.observePrizes() } returns stored
        coEvery { repository.saveAll(any()) } coAnswers {
            stored.value = firstArg<List<Prize>>().mapIndexed { index, p -> if (p.id < 0) p.copy(id = 100L + index) else p }
            SaveResult.Success
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = PrizeEditorViewModel(repository, imageStore)

    private fun form(name: String, probability: Int = 0, active: Boolean = false, image: String? = "/img/new.jpg") =
        PrizeForm(name, null, image, probability, active)

    @Test
    fun `loads saved prizes in admin order and starts clean`() {
        val state = viewModel().uiState.value
        assertTrue(state.loaded)
        assertFalse(state.dirty)
        assertEquals(listOf("Hammer", "Gloves", "Drill"), state.prizes.map { it.name })
        assertEquals(2, state.activeCount)
        assertEquals(100, state.activeProbabilityTotal)
        assertTrue(state.canActivateMore)
    }

    @Test
    fun `adding a prize marks the draft dirty with a temporary id`() {
        val vm = viewModel()
        vm.addPrize(form("Tape"))

        val state = vm.uiState.value
        assertTrue(state.dirty)
        assertTrue(state.prizes.any { it.name == "Tape" && it.id < 0 })
    }

    @Test
    fun `nothing is written to the repository until save`() {
        val vm = viewModel()
        vm.addPrize(form("Tape"))
        vm.deletePrize(3)
        vm.updatePrize(1, form("Big Hammer", 60, true))

        coVerify(exactly = 0) { repository.saveAll(any()) }
    }

    @Test
    fun `updating a prize changes its fields in the draft`() {
        val vm = viewModel()
        vm.updatePrize(3, form("Power Drill", 0, false))
        assertEquals("Power Drill", vm.draftPrize(3)?.name)
        assertTrue(vm.uiState.value.dirty)
    }

    @Test
    fun `deleting a prize removes it from the draft`() {
        val vm = viewModel()
        vm.deletePrize(3)
        assertNull(vm.draftPrize(3))
        assertEquals(2, vm.uiState.value.prizes.size)
    }

    @Test
    fun `discard restores the saved prizes`() {
        val vm = viewModel()
        vm.addPrize(form("Tape"))
        vm.deletePrize(1)
        vm.discard()

        val state = vm.uiState.value
        assertFalse(state.dirty)
        assertEquals(listOf("Hammer", "Gloves", "Drill"), state.prizes.map { it.name })
        assertNotNull(vm.draftPrize(3))
    }

    @Test
    fun `successful save writes the whole draft and reloads with real ids`() = runTest {
        val vm = viewModel()
        vm.addPrize(form("Tape"))
        vm.save()

        coVerify(exactly = 1) { repository.saveAll(match { list -> list.map { it.name } == listOf("Hammer", "Gloves", "Drill", "Tape") }) }
        val state = vm.uiState.value
        assertFalse(state.dirty)
        assertFalse(state.saving)
        assertTrue(state.prizes.none { it.id < 0 })
    }

    @Test
    fun `invalid save keeps the draft dirty and exposes every error`() = runTest {
        val errors = listOf(ConfigError.ProbabilityTotalOver100(160), ConfigError.ActiveMissingImage(1))
        coEvery { repository.saveAll(any()) } returns SaveResult.Invalid(errors)

        val vm = viewModel()
        vm.deletePrize(2)
        vm.save()

        val state = vm.uiState.value
        assertEquals(errors, state.errors)
        assertTrue(state.dirty)
        assertFalse(state.saving)
        assertEquals(stored.value, saved)
    }

    @Test
    fun `the next edit clears the previous save errors`() = runTest {
        coEvery { repository.saveAll(any()) } returns SaveResult.Invalid(listOf(ConfigError.ProbabilityTotalOver100(160)))
        val vm = viewModel()
        vm.deletePrize(2)
        vm.save()
        assertTrue(vm.uiState.value.errors.isNotEmpty())

        vm.updatePrize(1, form("Hammer", 100, true, "/img/1.jpg"))
        assertTrue(vm.uiState.value.errors.isEmpty())
    }

    @Test
    fun `discard clears save errors`() = runTest {
        coEvery { repository.saveAll(any()) } returns SaveResult.Invalid(listOf(ConfigError.ProbabilityTotalOver100(160)))
        val vm = viewModel()
        vm.deletePrize(2)
        vm.save()
        vm.discard()
        assertTrue(vm.uiState.value.errors.isEmpty())
    }

    @Test
    fun `activation limit is reported when eight prizes are active`() {
        stored.value = (1L..8L).map { prize(it, "P$it", 10, active = true, order = it.toInt()) } + prize(9, "Spare")
        val state = viewModel().uiState.value
        assertEquals(8, state.activeCount)
        assertFalse(state.canActivateMore)
    }

    private fun wheelIds(vm: PrizeEditorViewModel) = vm.uiState.value.prizes.filter { it.active }.map { it.id }

    @Test
    fun `editing a prize in the form can activate it and it joins the end of the wheel`() {
        val vm = viewModel()
        vm.updatePrize(3, form("Drill", 0, true, "/img/3.jpg"))
        assertEquals(listOf(1L, 2L, 3L), wheelIds(vm))
    }

    @Test
    fun `rearranging the active list reorders the wheel and marks the draft dirty`() {
        val vm = viewModel()
        vm.rearrange(listOf(2L, 1L))
        assertEquals(listOf(2L, 1L), wheelIds(vm))
        assertTrue(vm.uiState.value.dirty)
    }

    @Test
    fun `dragging an inactive prize into the active list activates it`() {
        val vm = viewModel()
        vm.rearrange(listOf(1L, 3L, 2L))
        assertEquals(listOf(1L, 3L, 2L), wheelIds(vm))
        assertEquals(3, vm.uiState.value.activeCount)
    }

    @Test
    fun `dragging an active prize out of the active list deactivates it`() {
        val vm = viewModel()
        vm.rearrange(listOf(1L))
        assertEquals(1, vm.uiState.value.activeCount)
        assertEquals(60, vm.uiState.value.activeProbabilityTotal)
        assertFalse(vm.draftPrize(2)!!.active)
    }

    @Test
    fun `an unchanged arrangement does not mark the draft dirty`() {
        val vm = viewModel()
        vm.rearrange(listOf(1L, 2L))
        assertFalse(vm.uiState.value.dirty)
    }

    @Test
    fun `pushing a prize out of a full wheel reports its name`() {
        stored.value = (1L..8L).map { prize(it, "P$it", 10, active = true, order = it.toInt()) } + prize(9, "Spare")
        val vm = viewModel()
        vm.rearrange(listOf(1L, 9L, 2L, 3L, 4L, 5L, 6L, 7L, 8L))

        val state = vm.uiState.value
        assertEquals("P8", state.pushedOutPrizeName)
        assertEquals(8, state.activeCount)
        assertEquals(9L, state.prizes.filter { it.active }[1].id)
    }

    @Test
    fun `the pushed out notice can be dismissed`() {
        stored.value = (1L..8L).map { prize(it, "P$it", 10, active = true, order = it.toInt()) } + prize(9, "Spare")
        val vm = viewModel()
        vm.rearrange(listOf(9L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L))
        vm.clearPushedOutNotice()
        assertNull(vm.uiState.value.pushedOutPrizeName)
    }

    @Test
    fun `discard also clears the pushed out notice`() {
        stored.value = (1L..8L).map { prize(it, "P$it", 10, active = true, order = it.toInt()) } + prize(9, "Spare")
        val vm = viewModel()
        vm.rearrange(listOf(9L, 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L))
        vm.discard()
        assertNull(vm.uiState.value.pushedOutPrizeName)
        assertFalse(vm.uiState.value.dirty)
    }

    @Test
    fun `rearranging clears earlier save errors`() = runTest {
        coEvery { repository.saveAll(any()) } returns SaveResult.Invalid(listOf(ConfigError.ProbabilityTotalOver100(160)))
        val vm = viewModel()
        vm.deletePrize(2)
        vm.save()
        assertTrue(vm.uiState.value.errors.isNotEmpty())

        // Putting Drill on the wheel is a real change, so the old errors no longer apply.
        vm.rearrange(listOf(1L, 3L))
        assertTrue(vm.uiState.value.errors.isEmpty())
    }

    @Test
    fun `a rearrange that changes nothing keeps the save errors`() = runTest {
        coEvery { repository.saveAll(any()) } returns SaveResult.Invalid(listOf(ConfigError.ProbabilityTotalOver100(160)))
        val vm = viewModel()
        vm.deletePrize(2)
        vm.save()

        vm.rearrange(listOf(1L))
        assertTrue(vm.uiState.value.errors.isNotEmpty())
    }

    @Test
    fun `imported images are delegated to the image store`() = runTest {
        val uri = mockk<Uri>()
        coEvery { imageStore.savePrizeImage(uri) } returns "/img/picked.jpg"
        assertEquals("/img/picked.jpg", viewModel().importImage(uri))
    }

    @Test
    fun `setting a probability changes only that prize and marks the draft dirty`() {
        val vm = viewModel()
        vm.setProbability(1, 30)

        val state = vm.uiState.value
        val hammer = state.prizes.first { it.id == 1L }
        assertEquals(30, hammer.probability)
        assertEquals("Hammer", hammer.name)
        assertTrue(hammer.active)
        assertEquals(1, hammer.wheelOrder)
        assertEquals(40, state.prizes.first { it.id == 2L }.probability)
        assertEquals(70, state.activeProbabilityTotal)
        assertEquals(30, state.bustProbability)
        assertTrue(state.dirty)
    }

    @Test
    fun `setting the same probability keeps the draft clean`() {
        val vm = viewModel()
        vm.setProbability(1, 60)
        assertFalse(vm.uiState.value.dirty)
    }

    @Test
    fun `setting a probability on an inactive prize keeps it inactive`() {
        val vm = viewModel()
        vm.setProbability(3, 25)

        val drill = vm.uiState.value.prizes.first { it.id == 3L }
        assertEquals(25, drill.probability)
        assertFalse(drill.active)
        assertEquals(100, vm.uiState.value.activeProbabilityTotal)
    }

    @Test
    fun `setting the probability of an unknown prize does nothing`() {
        val vm = viewModel()
        vm.setProbability(99, 10)
        assertFalse(vm.uiState.value.dirty)
    }

    @Test
    fun `setting a probability clears earlier save errors`() = runTest {
        coEvery { repository.saveAll(any()) } returns SaveResult.Invalid(listOf(ConfigError.ProbabilityTotalOver100(160)))
        val vm = viewModel()
        vm.setProbability(1, 100)
        vm.save()
        assertTrue(vm.uiState.value.errors.isNotEmpty())

        vm.setProbability(1, 50)
        assertTrue(vm.uiState.value.errors.isEmpty())
    }

    @Test
    fun `no prizes means a 100 percent bust chance`() {
        val state = PrizeEditorUiState(loaded = true, activeCount = 0, activeProbabilityTotal = 0)
        assertEquals(8, state.bustSlots)
        assertEquals(100, state.bustProbability)
        assertTrue(state.totalValid)
        assertFalse(state.bustSlotsUnreachable)
    }

    @Test
    fun `busts take what the prizes leave`() {
        val state = PrizeEditorUiState(loaded = true, activeCount = 3, activeProbabilityTotal = 40)
        assertEquals(5, state.bustSlots)
        assertEquals(60, state.bustProbability)
        assertTrue(state.totalValid)
        assertFalse(state.bustSlotsUnreachable)
    }

    @Test
    fun `prizes totalling 100 with bust slots on the wheel are valid but warned about`() {
        val state = PrizeEditorUiState(loaded = true, activeCount = 2, activeProbabilityTotal = 100)
        assertEquals(0, state.bustProbability)
        assertTrue(state.totalValid)
        assertTrue(state.bustSlotsUnreachable)
    }

    @Test
    fun `a full wheel must total exactly 100`() {
        assertFalse(PrizeEditorUiState(activeCount = 8, activeProbabilityTotal = 90).totalValid)
        assertEquals(0, PrizeEditorUiState(activeCount = 8, activeProbabilityTotal = 90).bustProbability)
        assertTrue(PrizeEditorUiState(activeCount = 8, activeProbabilityTotal = 100).totalValid)
        assertFalse(PrizeEditorUiState(activeCount = 8, activeProbabilityTotal = 100).bustSlotsUnreachable)
    }

    @Test
    fun `a total above 100 is invalid`() {
        val state = PrizeEditorUiState(activeCount = 2, activeProbabilityTotal = 110)
        assertFalse(state.totalValid)
        assertEquals(0, state.bustProbability)
    }
}
