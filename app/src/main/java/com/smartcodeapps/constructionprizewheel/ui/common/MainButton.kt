package com.smartcodeapps.constructionprizewheel.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** The red spin/continue button from the design. */
@Composable
fun MainButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    GlossyPillButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        palette = RedPill,
    )
}
