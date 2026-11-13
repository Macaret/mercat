package com.smartcodeapps.constructionprizewheel.ui.wheel

import android.graphics.BitmapFactory
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartcodeapps.constructionprizewheel.R
import com.smartcodeapps.constructionprizewheel.ui.common.MainButton
import com.smartcodeapps.constructionprizewheel.ui.common.rememberImageBitmap
import com.smartcodeapps.constructionprizewheel.ui.common.rememberShineSchedule
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WheelTestTags {
    const val SPIN_BUTTON = "spin_button"
    const val LOGO = "logo"
}

@Composable
fun WheelScreen(
    onNavigateToResult: (prizeId: Long) -> Unit,
    onNavigateToAdmin: () -> Unit,
    viewModel: WheelViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val rotation = remember { Animatable(state.restingRotation) }

    LaunchedEffect(state.resultPrizeId) {
        state.resultPrizeId?.let { prizeId ->
            onNavigateToResult(prizeId)
            viewModel.onResultShown()
        }
    }

    LaunchedEffect(state.spinPlan) {
        val plan = state.spinPlan
        if (plan == null) {
            rotation.snapTo(state.restingRotation)
        } else {
            rotation.animateTo(
                targetValue = plan.targetRotation,
                animationSpec = tween(durationMillis = SpinPlanner.DURATION_MS, easing = LinearOutSlowInEasing)
            )
            viewModel.onSpinFinished()
        }
    }

    WheelContent(
        state = state,
        rotationDegrees = { rotation.value },
        onSpin = viewModel::onSpin,
        onAdminRequested = onNavigateToAdmin
    )
}

@Composable
fun WheelContent(
    state: WheelUiState,
    rotationDegrees: () -> Float,
    onSpin: () -> Unit,
    modifier: Modifier = Modifier,
    onAdminRequested: () -> Unit = {}
) {
    val images = rememberSegmentImages(state.segments)
    val shine = rememberShineSchedule()
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.wheel_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Logo(
                path = state.branding.logoPath,
                modifier = Modifier
                    .height(LOGO_HEIGHT)
                    .testTag(WheelTestTags.LOGO)
                    .adminLongPress(enabled = !state.spinning, onTriggered = onAdminRequested)
            )
            Spacer(Modifier.height(12.dp))
            WheelCanvas(
                segments = state.segments,
                images = images,
                rotationDegrees = rotationDegrees,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.height(16.dp))
            MainButton(
                text = stringResource(R.string.spin_button),
                onClick = onSpin,
                enabled = state.canSpin,
                // No streak while the wheel is spinning or the button is disabled.
                shine = if (state.canSpin) ({ shine.button }) else null,
                modifier = Modifier.height(SPIN_BUTTON_HEIGHT).testTag(WheelTestTags.SPIN_BUTTON)
            )
        }
    }
}

@Composable
private fun Logo(path: String?, modifier: Modifier = Modifier) {
    val bitmap = rememberImageBitmap(path)
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, contentScale = ContentScale.Fit, modifier = modifier)
    } else {
        Image(
            painter = painterResource(R.drawable.mercat_logo),
            contentScale = ContentScale.Fit,
            contentDescription = null,
            modifier = modifier
        )
    }
}

@Composable
private fun rememberSegmentImages(segments: List<WheelSegment>): Map<Long, ImageBitmap> {
    val images by produceState<Map<Long, ImageBitmap>>(initialValue = emptyMap(), segments) {
        value = withContext(Dispatchers.IO) {
            segments.mapNotNull { segment ->
                val prizeId = segment.prizeId ?: return@mapNotNull null
                val bitmap = segment.imagePath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
                bitmap?.let { prizeId to it }
            }.toMap()
        }
    }
    return images
}

private val LOGO_HEIGHT = 110.dp

/** Matches the design image's proportions (711x186 at 320dp wide is ~84dp); never above 100dp. */
private val SPIN_BUTTON_HEIGHT = 64.dp

@Preview(widthDp = 1024, heightDp = 600, showBackground = true)
@Composable
private fun WheelContentPreview() {
    ConstructionPrizeWheelTheme {
        WheelContent(
            state = WheelUiState(
                loaded = true,
                segments = List(8) { if (it % 3 == 1) WheelSegment.Bust else WheelSegment(it.toLong(), "Prize $it", null) }
            ),
            rotationDegrees = { 0f },
            onSpin = {}
        )
    }
}
