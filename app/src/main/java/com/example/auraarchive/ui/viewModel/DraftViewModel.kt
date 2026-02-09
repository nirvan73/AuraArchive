package com.example.auraarchive.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auraarchive.module.DraftUiState
import com.example.auraarchive.repository.AuraRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DraftViewModel @Inject constructor(
    private val repo: AuraRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(DraftUiState())
    val uiState: StateFlow<DraftUiState> = _uiState.asStateFlow()

    fun loadDraft(postId: String) {
        viewModelScope.launch {
            if (_uiState.value.post == null) {
                _uiState.update { it.copy(isLoading = true) }
            }

            repo.pollForDraft(postId).collect { draft ->
                if (draft != null) {
                    _uiState.update { state ->
                        val shouldUpdateText = !state.isEditing

                        state.copy(
                            post = draft,
                            isLoading = false,
                            editedTitle = if (shouldUpdateText) (draft.title ?: state.editedTitle) else state.editedTitle,
                            editedSummary = if (shouldUpdateText) (draft.summary ?: state.editedSummary) else state.editedSummary,
                            editedContent = if (shouldUpdateText) (draft.content ?: state.editedContent) else state.editedContent,
                            errorMessage = if (draft.status == "FAILED") draft.error else null
                        )
                    }
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onTextFieldChange(title: String? = null, summary: String? = null, content: String? = null) {
        _uiState.update { state ->
            state.copy(
                editedTitle = title ?: state.editedTitle,
                editedSummary = summary ?: state.editedSummary,
                editedContent = content ?: state.editedContent
            )
        }
    }

    fun toggleEditMode() {
        _uiState.update { it.copy(isEditing = !it.isEditing) }
    }

    fun fetchAllDrafts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val response = repo.getDraftsList()
                _uiState.update { it.copy(
                    drafts = response.filter { it.status == "REVIEW_PENDING" },
                    isLoading = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun saveDraftUpdates() {
        val state = _uiState.value
        val currentPost = state.post ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                repo.updateDraft(currentPost.id, state.editedTitle, state.editedSummary, state.editedContent)
                _uiState.update { it.copy(
                    isSaving = false,
                    isEditing = false,
                    post = currentPost.copy(
                        title = state.editedTitle,
                        summary = state.editedSummary,
                        content = state.editedContent
                    )
                )}
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, errorMessage = e.message) }
            }
        }
    }

    fun publishDraft() {
        val currentPost = _uiState.value.post ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isPublishing = true) }
            try {
                repo.publishPost(currentPost.id)
                _uiState.update { it.copy(isPublishing = false, wasPublishedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isPublishing = false, errorMessage = e.message) }
            }
        }
    }
}