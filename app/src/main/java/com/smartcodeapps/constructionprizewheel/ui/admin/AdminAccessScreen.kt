package com.smartcodeapps.constructionprizewheel.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartcodeapps.constructionprizewheel.R
import com.smartcodeapps.constructionprizewheel.security.PinPolicy
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme

object AdminAccessTestTags {
    const val PIN_DISPLAY = "pin_display"
    const val SUBMIT = "pin_submit"
    const val CANCEL = "pin_cancel"
    const val ERROR = "pin_error"
    const val TITLE = "pin_title"
    const val KEY_DELETE = "key_delete"
    fun key(digit: Int) = "key_$digit"
}

/** Dark translucent layer over the background, as in the pinLayout design. */
private val ScreenOverlay = Color(0xBF0B1B33)
/** Opaque base so the shadow doesn't show through the translucent key; approximates the overlaid background. */
private val KeyBase = Color(0xFF3C485A)
private val KeyFill = Color.White.copy(alpha = 0.12f)
private val KeyBorder = Color.White.copy(alpha = 0.7f)
private val ErrorColor = Color(0xFFFF8A80)

private val KEY_SPACING = 12.dp
private val MAX_KEY_SIZE = 68.dp
private val MIN_KEY_SIZE = 44.dp
/** Height reserved for logo, title, PIN, error, submit and back, so the keypad fits. */
private val RESERVED_HEIGHT = 280.dp
private const val KEYPAD_ROWS = 4

fun pinDisplay(length: Int): String =
    "●".repeat(length.coerceIn(0, PinPolicy.LENGTH)) + "○".repeat((PinPolicy.LENGTH - length).coerceAtLeast(0))

@Composable
fun AdminAccessScreen(
    onAuthenticated: () -> Unit,
    onCancel: () -> Unit,
    viewModel: AdminAccessViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.authenticated) {
        if (state.authenticated) onAuthenticated()
    }
    BackHandler(onBack = onCancel)

    AdminAccessContent(
        state = state,
        onDigit = viewModel::onDigit,
        onBackspace = viewModel::onBackspace,
        onSubmit = viewModel::onSubmit,
        onCancel = onCancel
    )
}

@Composable
fun AdminAccessContent(
    state: AdminAccessUiState,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.wheel_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Logo sits under the overlay, so it is dimmed like the background.
        Image(
            painter = painterResource(R.drawable.mercat_logo),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp).height(72.dp)
        )
        Box(modifier = Modifier.fillMaxSize().background(ScreenOverlay))

        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val keySize = ((maxHeight - RESERVED_HEIGHT - KEY_SPACING * (KEYPAD_ROWS - 1)) / KEYPAD_ROWS)
                .coerceIn(MIN_KEY_SIZE, MAX_KEY_SIZE)

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            ) {
                Spacer(Modifier.height(96.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LockIcon(Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(titleFor(state.mode)),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag(AdminAccessTestTags.TITLE)
                    )
                }
                Text(
                    text = pinDisplay(state.pin.length),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 8.dp).testTag(AdminAccessTestTags.PIN_DISPLAY)
                )
                Text(
                    text = state.error?.let { stringResource(messageFor(it)) } ?: " ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ErrorColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.height(24.dp).testTag(AdminAccessTestTags.ERROR)
                )
                Keypad(keySize = keySize, onDigit = onDigit, onBackspace = onBackspace)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onCancel, modifier = Modifier.testTag(AdminAccessTestTags.CANCEL)) {
                    Text(
                        text = stringResource(R.string.back_button),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun LockIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = size.width * 0.12f
        // Shackle
        drawArc(
            color = Color.White,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(size.width * 0.25f, 0f),
            size = Size(size.width * 0.5f, size.height * 0.6f),
            style = Stroke(width = stroke)
        )
        // Body
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(0f, size.height * 0.45f),
            size = Size(size.width, size.height * 0.55f),
            cornerRadius = CornerRadius(size.width * 0.15f)
        )
    }
}

@Composable
private fun Keypad(keySize: Dp, onDigit: (Int) -> Unit, onBackspace: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(KEY_SPACING), horizontalAlignment = Alignment.CenterHorizontally) {
        listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(KEY_SPACING)) {
                row.forEach { digit ->
                    KeypadKey(digit.toString(), AdminAccessTestTags.key(digit), keySize) { onDigit(digit) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(KEY_SPACING)) {
            Spacer(Modifier.size(keySize))
            KeypadKey("0", AdminAccessTestTags.key(0), keySize) { onDigit(0) }
            KeypadKey(stringResource(R.string.delete_key), AdminAccessTestTags.KEY_DELETE, keySize, onBackspace)
        }
    }
}

@Composable
private fun KeypadKey(label: String, tag: String, size: Dp, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(KeyBase)
            .background(KeyFill)
            .border(width = 1.5.dp, color = KeyBorder, shape = CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(tag)
    ) {
        Text(text = label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

private fun titleFor(mode: AdminAccessMode): Int = when (mode) {
    AdminAccessMode.SETUP_ENTER -> R.string.admin_setup_title
    AdminAccessMode.SETUP_CONFIRM -> R.string.admin_confirm_title
    else -> R.string.admin_access_title
}

private fun submitLabelFor(mode: AdminAccessMode): Int = when (mode) {
    AdminAccessMode.SETUP_ENTER -> R.string.next_button
    AdminAccessMode.SETUP_CONFIRM -> R.string.save_pin_button
    else -> R.string.login_button
}

private fun messageFor(error: AdminAccessError): Int = when (error) {
    AdminAccessError.INCORRECT_PIN -> R.string.error_incorrect_pin
    AdminAccessError.INVALID_LENGTH -> R.string.error_invalid_pin_length
    AdminAccessError.MISMATCH -> R.string.error_pin_mismatch
}

@Preview(widthDp = 1024, heightDp = 600, showBackground = true)
@Composable
private fun AdminAccessPreview() {
    ConstructionPrizeWheelTheme {
        AdminAccessContent(
            state = AdminAccessUiState(mode = AdminAccessMode.LOGIN, pin = "123"),
            onDigit = {}, onBackspace = {}, onSubmit = {}, onCancel = {}
        )
    }
}

@Preview(widthDp = 800, heightDp = 360, showBackground = true)
@Composable
private fun AdminAccessSmallPreview() {
    ConstructionPrizeWheelTheme {
        AdminAccessContent(
            state = AdminAccessUiState(mode = AdminAccessMode.LOGIN, pin = "12"),
            onDigit = {}, onBackspace = {}, onSubmit = {}, onCancel = {}
        )
    }
}
