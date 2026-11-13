package com.smartcodeapps.constructionprizewheel.ui.result

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartcodeapps.constructionprizewheel.R
import com.smartcodeapps.constructionprizewheel.ui.common.MainButton
import com.smartcodeapps.constructionprizewheel.ui.common.rememberImageBitmap
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme

object ResultTestTags {
    const val CONTINUE_BUTTON = "continue_button"
    const val PRIZE_NAME = "prize_name"
    const val PRIZE_DESCRIPTION = "prize_description"
    const val PRIZE_IMAGE = "prize_image"
}

private val PRIZE_CIRCLE_SIZE = 255.dp
private val BANNER_MAX_WIDTH = 400.dp
private val GAP = 8.dp
/** congrats_banner.png is cropped to its visible area: 1400x384. */
private const val BANNER_ASPECT = 1400f / 384f
private val TEXT_COLOR = Color(0xFF1B2A1B)

@Composable
fun PrizeResultScreen(onContinue: () -> Unit, viewModel: ResultViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(onBack = onContinue)
    ResultContent(state = state, onContinue = onContinue)
}

@Composable
fun ResultContent(state: ResultUiState, onContinue: () -> Unit, modifier: Modifier = Modifier) {
    val prize = state.prize
    val image = rememberImageBitmap(prize?.imagePath)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Space above and below the centered prize circle. The banner must fit in the top half
        // (below the logo), so its width is limited by that space.
        val spaceAbovePrize: Dp = maxHeight / 2 - PRIZE_CIRCLE_SIZE / 2
        val bannerHeightLimit: Dp = spaceAbovePrize - GAP * 2
        val bannerWidth: Dp = minOf(BANNER_MAX_WIDTH, bannerHeightLimit * BANNER_ASPECT)

        Image(
            painter = painterResource(R.drawable.winner_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Banner centered in the space between the top of the screen and the prize.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(spaceAbovePrize)
        ) {
            Image(
                painter = painterResource(R.drawable.congrats_banner),
                contentDescription = stringResource(R.string.congrats_banner_description),
                contentScale = ContentScale.Fit,
                modifier = Modifier.width(bannerWidth)
            )
        }

        // Prize, centered on the whole screen.
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = prize?.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(PRIZE_CIRCLE_SIZE)
                    .clip(CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .testTag(ResultTestTags.PRIZE_IMAGE)
            )
        }

        // Name and description below the prize, in the space left above the button.
        if (prize != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = maxHeight / 2 + PRIZE_CIRCLE_SIZE / 2 + GAP)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = prize.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TEXT_COLOR,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(ResultTestTags.PRIZE_NAME)
                )
                if (!prize.description.isNullOrBlank()) {
                    Text(
                        text = prize.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TEXT_COLOR,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp).testTag(ResultTestTags.PRIZE_DESCRIPTION)
                    )
                }
            }
        }

        // Continue button pinned to the bottom.
        MainButton(
            text = stringResource(R.string.continue_button),
            onClick = onContinue,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .height(64.dp)
                .testTag(ResultTestTags.CONTINUE_BUTTON)
        )
    }
}

@Preview(widthDp = 1024, heightDp = 600, showBackground = true)
@Composable
private fun ResultContentPreview() {
    ConstructionPrizeWheelTheme {
        ResultContent(
            state = ResultUiState(
                loaded = true,
                prize = ResultPrize("Professional Hammer", "Heavy duty, 500g head", null)
            ),
            onContinue = {}
        )
    }
}
