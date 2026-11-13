package com.smartcodeapps.constructionprizewheel.ui.admin.prizes

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcodeapps.constructionprizewheel.data.PrizeRepository
import com.smartcodeapps.constructionprizewheel.data.SaveResult
import com.smartcodeapps.constructionprizewheel.data.image.ImageStore
import com.smartcodeapps.constructionprizewheel.domain.ConfigError
import com.smartcodeapps.constructionprizewheel.domain.Prize
import com.smartcodeapps.constructionprizewheel.domain.PrizeDraft
import com.smartcodeapps.constructionprizewheel.domain.WHEEL_SLOTS
import com.smartcodeapps.constructionprizewheel.domain.forAdminList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PrizeForm(
    val name: String,
    val description: String?,
    val imagePath: String?,
    val probability: Int,
    val active: Boolean
)

data class PrizeEditorUiState(
    val loaded: Boolean = false,
    val prizes: List<Prize> = emptyList(),
    val dirty: Boolean = false,
    val errors: List<ConfigError> = emptyList(),
    val activeCount: Int = 0,
    val activeProbabilityTotal: Int = 0,
    val canActivateMore: Boolean = true,
    val saving: Boolean = false,
    val pushedOutPrizeName: String? = null
) {
    val bustSlots: Int get() = WHEEL_SLOTS - activeCount

    /** Chance of landing on a bust; the bust slots share it equally. */
    val bustProbability: Int get() = if (bustSlots > 0) (100 - activeProbabilityTotal).coerceAtLeast(0) else 0

    val totalValid: Boolean
        get() = activeProbabilityTotal <= 100 && (activeCount < WHEEL_SLOTS || activeProbabilityTotal == 100)

    /** Valid, but the bust slots are drawn on the wheel with no chance of being landed on. */
    val bustSlotsUnreachable: Boolean get() = bustSlots in 1 until WHEEL_SLOTS && activeProbabilityTotal == 100
}

private data class EditorState(
    val original: List<Prize>? = null,
    val draft: PrizeDraft? = null,
    val errors: List<ConfigError> = emptyList(),
    val saving: Boolean = false,
    val pushedOutPrizeName: String? = null
)

private fun EditorState.toUiState(): PrizeEditorUiState {
    val draft = draft ?: return PrizeEditorUiState()
    val active = draft.prizes.filter { it.active }
    return PrizeEditorUiState(
        loaded = true,
        prizes = draft.prizes.forAdminList(),
        dirty = draft.prizes != original,
        errors = errors,
        activeCount = active.size,
        activeProbabilityTotal = active.sumOf { it.probability },
        canActivateMore = draft.canActivateMore,
        saving = saving,
        pushedOutPrizeName = pushedOutPrizeName
    )
}

@HiltViewModel
class PrizeEditorViewModel @Inject constructor(
    private val prizeRepository: PrizeRepository,
    private val imageStore: ImageStore
) : ViewModel() {

    private val state = MutableStateFlow(EditorState())

    val uiState: StateFlow<PrizeEditorUiState> = state
        .map { it.toUiState() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, PrizeEditorUiState())

    init {
        viewModelScope.launch { loadSaved() }
    }

    fun draftPrize(id: Long): Prize? = state.value.draft?.prizes?.firstOrNull { it.id == id }

    fun addPrize(form: PrizeForm) = edit { it.add(form.name, form.description, form.imagePath, form.probability, form.active) }

    fun updatePrize(id: Long, form: PrizeForm) =
        edit { it.update(id, form.name, form.description, form.imagePath, form.probability, form.active) }

    /** Changes only the probability of one prize, e.g. from the inline field in the list. */
    fun setProbability(id: Long, probability: Int) {
        val prize = draftPrize(id) ?: return
        if (prize.probability == probability) return
        updatePrize(
            id,
            PrizeForm(prize.name, prize.description, prize.imagePath, probability, prize.active)
        )
    }

    fun deletePrize(id: Long) = edit { it.delete(id) }

    fun rearrange(activeIds: List<Long>) {
        state.update { current ->
            val draft = current.draft ?: return@update current
            val result = draft.rearrange(activeIds)
            if (result.draft === draft && result.pushedOut.isEmpty()) return@update current
            current.copy(
                draft = result.draft,
                errors = emptyList(),
                pushedOutPrizeName = result.pushedOut.firstOrNull()?.name ?: current.pushedOutPrizeName
            )
        }
    }

    fun clearPushedOutNotice() {
        state.update { it.copy(pushedOutPrizeName = null) }
    }

    fun discard() {
        state.update { current ->
            val original = current.original ?: return@update current
            current.copy(draft = PrizeDraft(original), errors = emptyList(), pushedOutPrizeName = null)
        }
    }

    fun save() {
        val current = state.value
        val draft = current.draft ?: return
        if (current.saving) return
        state.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val result = prizeRepository.saveAll(draft.prizes)) {
                SaveResult.Success -> loadSaved()
                is SaveResult.Invalid -> state.update { it.copy(errors = result.errors, saving = false) }
            }
        }
    }

    suspend fun importImage(source: Uri): String = imageStore.savePrizeImage(source)

    private suspend fun loadSaved() {
        val saved = prizeRepository.observePrizes().first()
        state.value = EditorState(original = saved, draft = PrizeDraft(saved))
    }

    private fun edit(change: (PrizeDraft) -> PrizeDraft) {
        state.update { current ->
            val draft = current.draft ?: return@update current
            current.copy(draft = change(draft), errors = emptyList())
        }
    }
}
