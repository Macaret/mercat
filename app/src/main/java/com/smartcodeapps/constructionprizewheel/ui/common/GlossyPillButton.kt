package com.smartcodeapps.constructionprizewheel.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Colors for one glossy pill button: body gradient, pressed gradient and the raised rim. */
data class PillPalette(
    val top: Color,
    val bottom: Color,
    val pressedTop: Color,
    val pressedBottom: Color,
    val rimTop: Color,
    val rimBottom: Color,
)

val RedPill = PillPalette(
    top = Color(0xFFF0302A),
    bottom = Color(0xFFCF0805),
    pressedTop = Color(0xFFD72620),
    pressedBottom = Color(0xFFAD0502),
    rimTop = Color(0xFFFF5A50),
    rimBottom = Color(0xFFA80000),
)

val BluePill = PillPalette(
    top = Color(0xFF3A86F0),
    bottom = Color(0xFF1456C4),
    pressedTop = Color(0xFF2F76D8),
    pressedBottom = Color(0xFF0E47A8),
    rimTop = Color(0xFF7DB4FF),
    rimBottom = Color(0xFF0B3C94),
)

val NavyPill = PillPalette(
    top = Color(0xFF3A5478),
    bottom = Color(0xFF24395A),
    pressedTop = Color(0xFF30486A),
    pressedBottom = Color(0xFF1B2D4A),
    rimTop = Color(0xFF5B7BA6),
    rimBottom = Color(0xFF13233D),
)

/**
 * Glossy pill button: gradient body, raised rim, soft gloss and a white label.
 * Used for the main red button and for the admin menu.
 */
@Composable
fun GlossyPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    palette: PillPalette = RedPill,
    textSize: TextUnit = 22.sp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val fill = Brush.verticalGradient(
        colors = if (pressed) listOf(palette.pressedTop, palette.pressedBottom) else listOf(palette.top, palette.bottom),
    )

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 64.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .shadow(elevation = 6.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(palette.rimTop, palette.rimBottom)))
            .padding(2.dp)
            .clip(CircleShape)
            .background(fill)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Soft gloss over the upper half
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.18f),
                        0.5f to Color.Transparent,
                    ),
                ),
        )
        Text(
            text = text,
            color = Color.White,
            fontSize = textSize,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge.copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.35f),
                    offset = Offset(0f, 3f),
                    blurRadius = 4f,
                ),
            ),
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
        )
    }
}
