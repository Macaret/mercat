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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartcodeapps.constructionprizewheel.R
import com.smartcodeapps.constructionprizewheel.domain.MAX_ACTIVE_PRIZES
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

    Row(
        modifier = modifier.fillMaxSize().padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(260.dp)) {
            val frame = Modifier.size(240.dp).clip(RoundedCornerShape(16.dp))
            if (image != null) {
                Image(bitmap = image, contentDescription = null, contentScale = ContentScale.Crop, modifier = frame)
            } else {
                Box(modifier = frame.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.no_image), style = MaterialTheme.typography.titleMedium)
                }
            }
            OutlinedButton(
                onClick = onChooseImage,
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth().height(56.dp).testTag(EditPrizeTestTags.CHOOSE_IMAGE)
            ) {
                Text(
                    text = stringResource(if (imagePath == null) R.string.choose_image_button else R.string.change_image_button),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            OutlinedButton(
                onClick = onTakePhoto,
                modifier = Modifier.padding(top = 8.dp).fillMaxWidth().height(56.dp).testTag(EditPrizeTestTags.TAKE_PHOTO)
            ) {
                Text(text = stringResource(R.string.take_photo_button), style = MaterialTheme.typography.titleMedium)
            }
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
                modifier = Modifier.testTag(EditPrizeTestTags.TITLE)
            )
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.field_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(EditPrizeTestTags.NAME)
            )
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.field_description)) },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth().testTag(EditPrizeTestTags.DESCRIPTION)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Switch(
                    checked = active,
                    onCheckedChange = onActiveChange,
                    enabled = canActivate,
                    modifier = Modifier.testTag(EditPrizeTestTags.ACTIVE)
                )
                Text(stringResource(R.string.active_label), style = MaterialTheme.typography.titleMedium)
            }
            if (!canActivate) {
                Text(
                    text = stringResource(R.string.active_limit_hint, MAX_ACTIVE_PRIZES),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag(EditPrizeTestTags.ACTIVE_HINT)
                )
            }
            OutlinedTextField(
                value = probability,
                onValueChange = onProbabilityChange,
                label = { Text(stringResource(R.string.field_probability)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(240.dp).testTag(EditPrizeTestTags.PROBABILITY)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.height(56.dp).testTag(EditPrizeTestTags.CANCEL)
                ) { Text(stringResource(R.string.cancel_button), style = MaterialTheme.typography.titleMedium) }
                Button(
                    onClick = onDone,
                    enabled = name.isNotBlank(),
                    modifier = Modifier.height(56.dp).testTag(EditPrizeTestTags.DONE)
                ) { Text(stringResource(R.string.done_button), style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}

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
