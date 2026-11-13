package com.smartcodeapps.constructionprizewheel.ui.admin.prizes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartcodeapps.constructionprizewheel.R
import com.smartcodeapps.constructionprizewheel.domain.MAX_ACTIVE_PRIZES
import com.smartcodeapps.constructionprizewheel.domain.Prize
import com.smartcodeapps.constructionprizewheel.ui.admin.UnsavedChangesDialog
import com.smartcodeapps.constructionprizewheel.ui.common.BluePill
import com.smartcodeapps.constructionprizewheel.ui.common.GlossyPillButton
import com.smartcodeapps.constructionprizewheel.ui.common.rememberImageBitmap
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

object PrizeListTestTags {
    const val ADD = "prize_add"
    const val BACK = "prize_back"
    const val SUMMARY = "prize_summary"
    const val BUST_WARNING = "prize_bust_warning"
    const val ERRORS = "prize_errors"
    const val EMPTY = "prize_empty"
    const val ACTIVE_HEADER = "prize_active_header"
    const val INACTIVE_HEADER = "prize_inactive_header"
    const val ACTIVE_HINT = "prize_active_hint"
    const val INACTIVE_HINT = "prize_inactive_hint"
    fun row(id: Long) = "prize_row_$id"
    fun handle(id: Long) = "prize_handle_$id"
    fun edit(id: Long) = "prize_edit_$id"
    fun delete(id: Long) = "prize_delete_$id"
    fun probability(id: Long) = "prize_probability_$id"
}

private const val HEADER_ACTIVE = Long.MIN_VALUE
private const val HEADER_INACTIVE = Long.MIN_VALUE + 1

/** The list shown while dragging: [active header, active ids..., inactive header, inactive ids...]. */
internal fun buildArrangement(prizes: List<Prize>): List<Long> =
    listOf(HEADER_ACTIVE) + prizes.filter { it.active }.map { it.id } +
        HEADER_INACTIVE + prizes.filterNot { it.active }.map { it.id }

internal fun activeIdsOf(arrangement: List<Long>): List<Long> =
    arrangement.subList(1, arrangement.indexOf(HEADER_INACTIVE).coerceAtLeast(1))

/** Returns the arrangement after dragging [fromKey] over [toKey], or null when that move is not allowed. */
internal fun moveInArrangement(arrangement: List<Long>, fromKey: Long, toKey: Long): List<Long>? {
    if (fromKey == HEADER_ACTIVE || fromKey == HEADER_INACTIVE || toKey == HEADER_ACTIVE) return null
    val from = arrangement.indexOf(fromKey)
    val to = arrangement.indexOf(toKey)
    if (from < 0 || to < 0 || from == to) return null
    return arrangement.toMutableList().apply { add(to, removeAt(from)) }
}

@Composable
fun PrizeListScreen(
    editor: PrizeEditorViewModel,
    onAddPrize: () -> Unit,
    onEditPrize: (Long) -> Unit,
    onBack: () -> Unit
) {
    val state by editor.uiState.collectAsStateWithLifecycle()
    var confirmLeave by remember { mutableStateOf(false) }
    val requestBack = { if (state.dirty) confirmLeave = true else onBack() }

    // Every change is saved automatically. If the configuration is invalid (for example the
    // probabilities don't total 100%), the errors are shown and saving is retried on the next change.
    LaunchedEffect(state.prizes, state.dirty) {
        if (state.dirty && !state.saving && state.errors.isEmpty()) editor.save()
    }

    BackHandler(onBack = requestBack)
    PrizeListContent(
        state = state,
        onAddPrize = onAddPrize,
        onEditPrize = onEditPrize,
        onDeletePrize = editor::deletePrize,
        onRearrange = editor::rearrange,
        onBack = requestBack,
        onNoticeShown = editor::clearPushedOutNotice,
        onProbabilityChange = editor::setProbability
    )

    if (confirmLeave) {
        UnsavedChangesDialog(
            onDiscard = {
                confirmLeave = false
                editor.discard()
                onBack()
            },
            onKeepEditing = { confirmLeave = false }
        )
    }
}

@Composable
fun PrizeListContent(
    state: PrizeEditorUiState,
    onAddPrize: () -> Unit,
    onEditPrize: (Long) -> Unit,
    onDeletePrize: (Long) -> Unit,
    onRearrange: (List<Long>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNoticeShown: () -> Unit = {},
    onProbabilityChange: (id: Long, probability: Int) -> Unit = { _, _ -> }
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val notice = state.pushedOutPrizeName?.let { stringResource(R.string.pushed_out_notice, it, MAX_ACTIVE_PRIZES) }
    LaunchedEffect(notice) {
        if (notice != null) {
            snackbarHostState.showSnackbar(notice)
            onNoticeShown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(start = 12.dp, top = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.prizes_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                GlossyPillButton(
                    text = stringResource(R.string.add_prize_button),
                    onClick = onAddPrize,
                    palette = BluePill,
                    textSize = 16.sp,
                    modifier = Modifier.height(56.dp).testTag(PrizeListTestTags.ADD)
                )
            }

            Text(
                text = stringResource(
                    R.string.prize_summary,
                    state.activeCount,
                    MAX_ACTIVE_PRIZES,
                    state.activeProbabilityTotal,
                    state.bustProbability
                ),
                style = MaterialTheme.typography.titleMedium,
                color = if (state.totalValid) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(PrizeListTestTags.SUMMARY)
            )
            if (state.bustSlotsUnreachable) {
                Text(
                    text = stringResource(R.string.bust_unreachable_warning, state.bustSlots),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.testTag(PrizeListTestTags.BUST_WARNING)
                )
            }

            if (state.errors.isNotEmpty()) ErrorsCard(state)

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (state.loaded && state.prizes.isEmpty()) {
                    Text(
                        text = stringResource(R.string.prize_list_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center).testTag(PrizeListTestTags.EMPTY)
                    )
                } else {
                    PrizeSections(
                        prizes = state.prizes,
                        onEditPrize = onEditPrize,
                        onDeletePrize = onDeletePrize,
                        onRearrange = onRearrange,
                        onProbabilityChange = onProbabilityChange
                    )
                }
            }

        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun PrizeSections(
    prizes: List<Prize>,
    onEditPrize: (Long) -> Unit,
    onDeletePrize: (Long) -> Unit,
    onRearrange: (List<Long>) -> Unit,
    onProbabilityChange: (id: Long, probability: Int) -> Unit
) {
    var resetKey by remember { mutableIntStateOf(0) }
    val arrangement = remember(prizes, resetKey) { buildArrangement(prizes).toMutableStateList() }
    val prizesById = remember(prizes) { prizes.associateBy { it.id } }
    val lazyListState = rememberLazyListState()

    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val moved = moveInArrangement(arrangement, from.key as Long, to.key as Long)
        if (moved != null) {
            arrangement.clear()
            arrangement.addAll(moved)
        }
    }

    val inactiveHeaderIndex = arrangement.indexOf(HEADER_INACTIVE)
    val liveActiveCount = inactiveHeaderIndex - 1
    val liveInactiveCount = arrangement.size - inactiveHeaderIndex - 1

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(arrangement, key = { it }) { key ->
            when (key) {
                HEADER_ACTIVE -> SectionHeader(
                    title = stringResource(R.string.wheel_on_wheel_header, liveActiveCount, MAX_ACTIVE_PRIZES),
                    hint = stringResource(R.string.prizes_active_hint).takeIf { liveActiveCount == 0 },
                    headerTag = PrizeListTestTags.ACTIVE_HEADER,
                    hintTag = PrizeListTestTags.ACTIVE_HINT,
                    emphasized = true
                )
                HEADER_INACTIVE -> ReorderableItem(reorderState, key) {
                    SectionHeader(
                        title = stringResource(R.string.wheel_off_wheel_header),
                        hint = stringResource(R.string.prizes_inactive_hint).takeIf { liveInactiveCount == 0 },
                        headerTag = PrizeListTestTags.INACTIVE_HEADER,
                        hintTag = PrizeListTestTags.INACTIVE_HINT,
                        emphasized = false,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                else -> {
                    val prize = prizesById[key] ?: return@items
                    ReorderableItem(reorderState, key) { isDragging ->
                        PrizeRow(
                            prize = prize,
                            inactiveSection = arrangement.indexOf(key) > inactiveHeaderIndex,
                            isDragging = isDragging,
                            onEdit = { onEditPrize(prize.id) },
                            onDelete = { onDeletePrize(prize.id) },
                            onProbabilityChange = { onProbabilityChange(prize.id, it) },
                            onDragStopped = {
                                onRearrange(activeIdsOf(arrangement))
                                resetKey++
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    hint: String?,
    headerTag: String,
    hintTag: String,
    emphasized: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 3.dp,
            color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp).testTag(headerTag)
        )
        if (hint != null) {
            Text(
                text = hint,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp).testTag(hintTag)
            )
        }
    }
}

@Composable
private fun ReorderableCollectionItemScope.PrizeRow(
    prize: Prize,
    inactiveSection: Boolean,
    isDragging: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onProbabilityChange: (Int) -> Unit,
    onDragStopped: () -> Unit
) {
    val image = rememberImageBitmap(prize.imagePath)
    val elevation by animateDpAsState(if (isDragging) 10.dp else 0.dp, label = "dragElevation")
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        modifier = Modifier.fillMaxWidth().testTag(PrizeListTestTags.row(prize.id))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DragHandle(
                modifier = Modifier
                    .draggableHandle(onDragStopped = onDragStopped)
                    .testTag(PrizeListTestTags.handle(prize.id))
            )
            val thumbnail = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
            if (image != null) {
                Image(bitmap = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = thumbnail)
            } else {
                Box(
                    modifier = thumbnail.background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.no_image), style = MaterialTheme.typography.labelSmall)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = prize.name.ifBlank { "—" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(if (prize.active) R.string.prize_row_active else R.string.prize_row_inactive),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            ProbabilityField(
                value = prize.probability,
                description = stringResource(R.string.probability_field_description, prize.name.ifBlank { "—" }),
                onCommit = onProbabilityChange,
                modifier = Modifier.testTag(PrizeListTestTags.probability(prize.id))
            )
            RowActionButton(
                kind = RowActionKind.EDIT,
                onClick = onEdit,
                modifier = Modifier.testTag(PrizeListTestTags.edit(prize.id))
            )
            RowActionButton(
                kind = RowActionKind.DELETE,
                onClick = onDelete,
                modifier = Modifier.padding(end = 12.dp).testTag(PrizeListTestTags.delete(prize.id))
            )
        }
    }
}

/**
 * Inline percentage editor. The text is kept locally while typing and committed (clamped to 0..100)
 * when the keyboard's Done is pressed or the field loses focus, so the list's auto-save never sees
 * half-typed values.
 */
@Composable
private fun ProbabilityField(
    value: Int,
    description: String,
    onCommit: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    fun commit() {
        val parsed = (text.toIntOrNull() ?: 0).coerceIn(0, 100)
        text = parsed.toString()
        if (parsed != value) onCommit(parsed)
    }

    OutlinedTextField(
        value = text,
        onValueChange = { text = sanitizeProbability(it) },
        singleLine = true,
        suffix = { Text("%") },
        textStyle = MaterialTheme.typography.titleMedium,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = {
            commit()
            focusManager.clearFocus()
        }),
        shape = RoundedCornerShape(12.dp),
        colors = fieldColors(),
        modifier = modifier
            .width(112.dp)
            .semantics { contentDescription = description }
            .onFocusChanged {
                if (focused && !it.isFocused) commit()
                focused = it.isFocused
            }
    )
}

@Composable
private fun DragHandle(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.drag_handle_description)
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier.size(56.dp).semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(width = 26.dp, height = 20.dp)) {
            val stroke = 3.dp.toPx()
            listOf(0.1f, 0.5f, 0.9f).forEach { fraction ->
                val y = size.height * fraction
                drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = stroke, cap = StrokeCap.Round)
            }
        }
    }
}

@Preview(widthDp = 1024, heightDp = 600, showBackground = true)
@Composable
private fun PrizeListPreview() {
    ConstructionPrizeWheelTheme {
        PrizeListContent(
            state = PrizeEditorUiState(
                loaded = true,
                prizes = listOf(
                    Prize(1, "Hammer", null, null, 60, true, 1),
                    Prize(2, "Gloves", null, null, 40, true, 2),
                    Prize(3, "Drill", null, null, 0, false, null)
                ),
                dirty = true,
                activeCount = 2,
                activeProbabilityTotal = 100
            ),
            onAddPrize = {}, onEditPrize = {}, onDeletePrize = {}, onRearrange = {},
            onBack = {}
        )
    }
}
