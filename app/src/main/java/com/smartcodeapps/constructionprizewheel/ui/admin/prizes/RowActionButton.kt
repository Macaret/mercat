package com.smartcodeapps.constructionprizewheel.ui.admin.prizes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.smartcodeapps.constructionprizewheel.R

enum class RowActionKind { EDIT, DELETE }

private val EditBackground = Color(0xFFE3EEFF)
private val EditTint = Color(0xFF1E6FD9)
private val DeleteBackground = Color(0xFFFDE4E4)
private val DeleteTint = Color(0xFFE03030)

/** Rounded square icon button (pencil or trash), styled like the admin design. */
@Composable
fun RowActionButton(
    kind: RowActionKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(
        if (kind == RowActionKind.EDIT) R.string.edit_button else R.string.delete_button
    )
    val background = if (kind == RowActionKind.EDIT) EditBackground else DeleteBackground
    val tint = if (kind == RowActionKind.EDIT) EditTint else DeleteTint

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            if (kind == RowActionKind.EDIT) drawPencil(tint) else drawTrash(tint)
        }
    }
}

private fun DrawScope.drawPencil(color: Color) {
    val w = size.width
    drawLine(
        color = color,
        start = Offset(w * 0.18f, w * 0.82f),
        end = Offset(w * 0.8f, w * 0.2f),
        strokeWidth = w * 0.2f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(w * 0.1f, w * 0.9f),
        end = Offset(w * 0.2f, w * 0.8f),
        strokeWidth = w * 0.12f,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawTrash(color: Color) {
    val w = size.width
    val stroke = w * 0.1f
    // Lid
    drawLine(
        color = color,
        start = Offset(w * 0.12f, w * 0.26f),
        end = Offset(w * 0.88f, w * 0.26f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    // Handle
    drawLine(
        color = color,
        start = Offset(w * 0.38f, w * 0.14f),
        end = Offset(w * 0.62f, w * 0.14f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    // Body
    drawRoundRect(
        color = color,
        topLeft = Offset(w * 0.22f, w * 0.34f),
        size = Size(w * 0.56f, w * 0.56f),
        cornerRadius = CornerRadius(w * 0.08f),
        style = Stroke(width = stroke),
    )
}
