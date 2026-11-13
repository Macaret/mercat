package com.smartcodeapps.constructionprizewheel.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smartcodeapps.constructionprizewheel.data.Branding
import com.smartcodeapps.constructionprizewheel.domain.Prize
import com.smartcodeapps.constructionprizewheel.ui.admin.AdminAccessError
import com.smartcodeapps.constructionprizewheel.ui.admin.AdminAccessContent
import com.smartcodeapps.constructionprizewheel.ui.admin.AdminAccessMode
import com.smartcodeapps.constructionprizewheel.ui.admin.AdminAccessTestTags
import com.smartcodeapps.constructionprizewheel.ui.admin.AdminAccessUiState
import com.smartcodeapps.constructionprizewheel.ui.admin.prizes.PrizeEditorUiState
import com.smartcodeapps.constructionprizewheel.ui.admin.prizes.PrizeListContent
import com.smartcodeapps.constructionprizewheel.ui.admin.prizes.PrizeListTestTags
import com.smartcodeapps.constructionprizewheel.ui.result.ResultContent
import com.smartcodeapps.constructionprizewheel.ui.result.ResultPrize
import com.smartcodeapps.constructionprizewheel.ui.result.ResultTestTags
import com.smartcodeapps.constructionprizewheel.ui.result.ResultUiState
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme
import com.smartcodeapps.constructionprizewheel.ui.wheel.WheelContent
import com.smartcodeapps.constructionprizewheel.ui.wheel.WheelSegment
import com.smartcodeapps.constructionprizewheel.ui.wheel.WheelTestTags
import com.smartcodeapps.constructionprizewheel.ui.wheel.WheelUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Checks that key controls stay inside the screen at common landscape sizes, including a short one. */
class TabletLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private data class ScreenSize(val name: String, val width: Dp, val height: Dp)

    private val sizes = listOf(
        ScreenSize("small tablet", 800.dp, 480.dp),
        ScreenSize("standard tablet", 960.dp, 600.dp),
        ScreenSize("large tablet", 1280.dp, 800.dp),
        ScreenSize("extra large tablet", 1920.dp, 1200.dp)
    )
    private val shortScreen = ScreenSize("short screen", 800.dp, 360.dp)

    private var screenSize by mutableStateOf(sizes.first())
    private var screenContent by mutableStateOf<@Composable () -> Unit>({})
    private var started = false

    private fun show(size: ScreenSize, content: @Composable () -> Unit) {
        screenSize = size
        screenContent = content
        if (!started) {
            started = true
            composeRule.setContent {
                ConstructionPrizeWheelTheme {
                    Box(Modifier.requiredSize(screenSize.width, screenSize.height).testTag(CONTAINER)) {
                        screenContent()
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun assertInside(size: ScreenSize, tag: String) {
        val container = composeRule.onNodeWithTag(CONTAINER).getBoundsInRoot()
        val node = composeRule.onNodeWithTag(tag).getBoundsInRoot()
        val slack = 1.dp
        assertTrue(
            "${size.name}: '$tag' $node is outside the screen $container",
            node.left >= container.left - slack && node.top >= container.top - slack &&
                node.right <= container.right + slack && node.bottom <= container.bottom + slack
        )
    }

    @Test
    fun wheelScreenKeepsItsControlsOnScreen() {
        sizes.forEach { size ->
            show(size) {
                WheelContent(
                    state = WheelUiState(
                        loaded = true,
                        branding = Branding(mainTitle = "SPIN THE WHEEL AND WIN A GREAT PRIZE TODAY!"),
                        segments = List(8) { WheelSegment(it.toLong(), "Prize $it", null) }
                    ),
                    rotationDegrees = { 0f },
                    onSpin = {}
                )
            }
            assertInside(size, WheelTestTags.SPIN_BUTTON)
            assertInside(size, WheelTestTags.LOGO)
        }
    }

    @Test
    fun noPrizesMessageStaysOnScreen() {
        sizes.forEach { size ->
            show(size) { WheelContent(state = WheelUiState(loaded = true), rotationDegrees = { 0f }, onSpin = {}) }
            assertInside(size, WheelTestTags.NO_PRIZES_MESSAGE)
        }
    }

    @Test
    fun resultScreenKeepsTheContinueButtonOnScreen() {
        val longPrize = ResultPrize(
            name = "Professional Heavy Duty Cordless Power Drill Set",
            description = "Includes two batteries, a charger, a carrying case and a full set of drill bits.",
            imagePath = null
        )
        (sizes + shortScreen).forEach { size ->
            show(size) { ResultContent(state = ResultUiState(loaded = true, prize = longPrize), onContinue = {}) }
            assertInside(size, ResultTestTags.CONTINUE_BUTTON)
        }
    }

    @Test
    fun pinScreenShowsEveryKeyAndTheLoginButton() {
        (sizes + shortScreen).forEach { size ->
            show(size) {
                AdminAccessContent(
                    state = AdminAccessUiState(mode = AdminAccessMode.LOGIN, pin = "12", error = AdminAccessError.INCORRECT_PIN),
                    onDigit = {}, onBackspace = {}, onSubmit = {}, onCancel = {}
                )
            }
            (0..9).forEach { assertInside(size, AdminAccessTestTags.key(it)) }
            assertInside(size, AdminAccessTestTags.KEY_DELETE)
        }
    }

    @Test
    fun pinScreenLoginButtonIsReachableOnTabletSizes() {
        sizes.forEach { size ->
            show(size) {
                AdminAccessContent(
                    state = AdminAccessUiState(mode = AdminAccessMode.LOGIN, pin = "1234"),
                    onDigit = {}, onBackspace = {}, onSubmit = {}, onCancel = {}
                )
            }
            assertInside(size, AdminAccessTestTags.SUBMIT)
            assertInside(size, AdminAccessTestTags.CANCEL)
        }
    }

    @Test
    fun prizeListKeepsTheSaveBarOnScreenWithManyPrizes() {
        val prizes = List(12) { index ->
            Prize(
                id = index + 1L,
                name = "Prize ${index + 1}",
                description = null,
                imagePath = null,
                probability = if (index < 8) 10 else 0,
                active = index < 8,
                wheelOrder = if (index < 8) index + 1 else null
            )
        }
        sizes.forEach { size ->
            show(size) {
                PrizeListContent(
                    state = PrizeEditorUiState(
                        loaded = true, prizes = prizes, dirty = true, activeCount = 8, activeProbabilityTotal = 80
                    ),
                    onAddPrize = {}, onEditPrize = {}, onDeletePrize = {}, onRearrange = {},
                    onSave = {}, onDiscard = {}, onBack = {}
                )
            }
            assertInside(size, PrizeListTestTags.SAVE)
            assertInside(size, PrizeListTestTags.DISCARD)
            assertInside(size, PrizeListTestTags.ADD)
        }
    }

    private companion object {
        const val CONTAINER = "screen_container"
    }
}
