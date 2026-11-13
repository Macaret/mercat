package com.smartcodeapps.constructionprizewheel.ui.admin.prizes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartcodeapps.constructionprizewheel.R
import com.smartcodeapps.constructionprizewheel.domain.MAX_ACTIVE_PRIZES
import com.smartcodeapps.constructionprizewheel.ui.admin.ContentPanelColor
import com.smartcodeapps.constructionprizewheel.ui.admin.SidebarColor
import com.smartcodeapps.constructionprizewheel.ui.common.BluePill
import com.smartcodeapps.constructionprizewheel.ui.common.GlossyPillButton
import com.smartcodeapps.constructionprizewheel.ui.common.GreenPill
import com.smartcodeapps.constructionprizewheel.ui.common.NavyPill
import com.smartcodeapps.constructionprizewheel.ui.common.rememberImageBitmap
import com.smartcodeapps.constructionprizewheel.ui.theme.ConstructionPrizeWheelTheme
import kotlinx.coroutines.launch
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

const val NEW_PRIZE_ID = 0L

object EditPrizeTestTags {
    const val NAME = "edit_name"
    const val DESCRIPTION = "edit_description"
    const val PROBABILITY = "edit_probability"
    const val ACTIVE = "edit_active"
    const val CHOOSE_IMAGE = "edit_choose_image"
    const val TAKE_PHOTO = "edit_take_photo"
    const val IMAGE_ERROR ="edit_image_error"
    const val ACTIVE_HINT = "edit_active_hint"
    const val DONE = "edit_done"
    const val CANCEL = "edit_cancel"
    const val TITLE = "edit_title"
}

@Composable
fun EditPrizeScreen(editor: PrizeEditorViewModel, prizeId: Long, onFinished: () -> Unit) {
    val state by editor.uiState.collectAsStateWithLifecycle()
    val existing = remember(prizeId, state.loaded) { editor.draftPrize(prizeId) }
    val isNew = prizeId == NEW_PRIZE_ID

    var name by rememberSaveable(prizeId) { mutableStateOf(existing?.name.orEmpty()) }
    var description by rememberSaveable(prizeId) { mutableStateOf(existing?.description.orEmpty()) }
    var imagePath by rememberSaveable(prizeId) { mutableStateOf(existing?.imagePath) }
    var probability by rememberSaveable(prizeId) { mutableStateOf(existing?.probability?.toString().orEmpty()) }
    var active by rememberSaveable(prizeId) { mutableStateOf(existing?.active ?: false) }
    var imageError by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    imagePath = editor.importImage(uri)
                    imageError = false
                } catch (_: IOException) {
                    imageError = true
                }
            }
        }
    }

    val context = LocalContext.current
    var pendingPhoto by rememberSaveable { mutableStateOf<Uri?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingPhoto
        pendingPhoto = null
        if (saved && uri != null) {
            scope.launch {
                try {
                    imagePath = editor.importImage(uri)
                    imageError = false
                } catch (_: IOException) {
                    imageError = true
                }
            }
        }
    }

    EditPrizeContent(
        isNew = isNew,
        name = name,
        onNameChange = { name = it },
        description = description,
        onDescriptionChange = { description = it },
        imagePath = imagePath,
        imageError = imageError,
        onChooseImage = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onTakePhoto = {
            val dir = File(context.cacheDir, "camera").apply { mkdirs() }
            val file = File.createTempFile("prize_", ".jpg", dir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingPhoto = uri
            camera.launch(uri)
        },
        probability = probability,
        onProbabilityChange = { probability = sanitizeProbability(it) },
        active = active,
        onActiveChange = { active = it },
        canActivate = active || existing?.active == true || state.canActivateMore,
        onDone = {
            val form = PrizeForm(
                name = name.trim(),
                description = description.trim().ifEmpty { null },
                imagePath = imagePath,
                probability = probability.toIntOrNull() ?: 0,
                active = active
            )
            if (isNew) editor.addPrize(form) else editor.updatePrize(prizeId, form)
            onFinished()
        },
        onCancel = onFinished
    )
}

fun sanitizeProbability(input: String): String = input.filter(Char::isDigit).take(3)

@Composable
fun EditPrizeContent(
    isNew: Boolean,
    name: String,
    onNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    imagePath: String?,
    imageError: Boolean,
    onChooseImage: () -> Unit,
    probability: String,
    onProbabilityChange: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    canActivate: Boolean,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    onTakePhoto: () -> Unit = {}
) {
    val image = rememberImageBitmap(imagePath)

    // Same frame as the admin layout: navy background around a light rounded panel.
    Box(modifier = modifier.fillMaxSize().background(SidebarColor).padding(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(ContentPanelColor)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(260.dp)) {
                val frame = Modifier.size(240.dp).clip(RoundedCornerShape(16.dp))
                if (image != null) {
                    Image(bitmap = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = frame)
                } else {
                    Box(modifier = frame.background(ImagePlaceholderColor), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.no_image),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextColor
                        )
                    }
                }
                GlossyPillButton(
                    text = stringResource(
                        if (imagePath == null) R.string.choose_image_button else R.string.change_image_button
                    ),
                    onClick = onChooseImage,
                    palette = BluePill,
                    textSize = 16.sp,
                    modifier = Modifier.padding(top = 16.dp).fillMaxWidth().height(56.dp)
                        .testTag(EditPrizeTestTags.CHOOSE_IMAGE)
                )
                GlossyPillButton(
                    text = stringResource(R.string.take_photo_button),
                    onClick = onTakePhoto,
                    palette = NavyPill,
                    textSize = 16.sp,
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth().height(56.dp)
                        .testTag(EditPrizeTestTags.TAKE_PHOTO)
                )
                if (imageError) {
                    Text(
                        text = stringResource(R.string.image_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp).testTag(EditPrizeTestTags.IMAGE_ERROR)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(if (isNew) R.string.new_prize_title else R.string.edit_prize_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextColor,
                    modifier = Modifier.testTag(EditPrizeTestTags.TITLE)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.field_name)) },
                    singleLine = true,
                    shape = FieldShape,
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag(EditPrizeTestTags.NAME)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text(stringResource(R.string.field_description)) },
                    minLines = 2,
                    maxLines = 3,
                    shape = FieldShape,
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth().testTag(EditPrizeTestTags.DESCRIPTION)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Switch(
                        checked = active,
                        onCheckedChange = onActiveChange,
                        enabled = canActivate,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AccentBlue,
                            checkedBorderColor = AccentBlue,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFB4BCCB),
                            uncheckedBorderColor = Color(0xFFB4BCCB),
                            disabledCheckedThumbColor = Color.White,
                            disabledCheckedTrackColor = AccentBlue.copy(alpha = 0.4f),
                            disabledUncheckedThumbColor = Color.White,
                            disabledUncheckedTrackColor = Color(0xFFB4BCCB).copy(alpha = 0.5f),
                            disabledUncheckedBorderColor = Color.Transparent,
                        ),
                        modifier = Modifier.testTag(EditPrizeTestTags.ACTIVE)
                    )
                    Text(
                        text = stringResource(R.string.active_label),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextColor
                    )
                }
                if (!canActivate) {
                    Text(
                        text = stringResource(R.string.active_limit_hint, MAX_ACTIVE_PRIZES),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextColor,
                        modifier = Modifier.testTag(EditPrizeTestTags.ACTIVE_HINT)
                    )
                }
                OutlinedTextField(
                    value = probability,
                    onValueChange = onProbabilityChange,
                    label = { Text(stringResource(R.string.field_probability)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = FieldShape,
                    colors = fieldColors(),
                    modifier = Modifier.width(240.dp).testTag(EditPrizeTestTags.PROBABILITY)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                    GlossyPillButton(
                        text = stringResource(R.string.cancel_button),
                        onClick = onCancel,
                        palette = NavyPill,
                        textSize = 16.sp,
                        modifier = Modifier.height(56.dp).testTag(EditPrizeTestTags.CANCEL)
                    )
                    GlossyPillButton(
                        text = stringResource(R.string.done_button),
                        onClick = onDone,
                        enabled = name.isNotBlank(),
                        palette = GreenPill,
                        textSize = 16.sp,
                        modifier = Modifier.height(56.dp).testTag(EditPrizeTestTags.DONE)
                    )
                }
            }
        }
    }
}

private val AccentBlue = Color(0xFF1E6FD9)
private val TextColor = Color(0xFF13233D)
private val ImagePlaceholderColor = Color(0xFFE3E7EE)
private val FieldShape = RoundedCornerShape(12.dp)

/** White fields with a blue focus accent, so they sit on the light admin panel instead of the purple theme. */
@Composable
internal fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextColor,
    unfocusedTextColor = TextColor,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedBorderColor = AccentBlue,
    unfocusedBorderColor = Color(0xFFB4BCCB),
    focusedLabelColor = AccentBlue,
    unfocusedLabelColor = Color(0xFF5B6678),
    cursorColor = AccentBlue,
)

@Preview(widthDp = 1024, heightDp = 600, showBackground = true)
@Composable
private fun EditPrizePreview() {
    ConstructionPrizeWheelTheme {
        EditPrizeContent(
            isNew = false, name = "Professional Hammer", onNameChange = {}, description = "Heavy duty",
            onDescriptionChange = {}, imagePath = null, imageError = false, onChooseImage = {},
            probability = "25", onProbabilityChange = {}, active = true, onActiveChange = {},
            canActivate = true, onDone = {}, onCancel = {}
        )
    }
}
