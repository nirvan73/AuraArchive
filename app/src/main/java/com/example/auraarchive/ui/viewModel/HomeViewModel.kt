package com.example.auraarchive.ui.viewModel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auraarchive.module.HomeUiState
import com.example.auraarchive.repository.AuraRepo
import com.example.auraarchive.repository.QuoteRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: AuraRepo,
    private val quoteRepo: QuoteRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigateToDraft = MutableSharedFlow<String>(replay = 1)
    val navigateToDraft = _navigateToDraft.asSharedFlow()

    init {
        loadFeed()
        fetchInspiration()
    }

     fun fetchInspiration() {
        viewModelScope.launch {
            val response = quoteRepo.getRandomQuote()
            if (response != null) {
                _uiState.update { it.copy(
                    quote = response.quote,
                    author = response.author
                ) }
            }
        }
    }

    fun loadFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repo.fetchFeed().collect { result ->
                    _uiState.update { it.copy(posts = result, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun uploadAudio(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repo.uploadAudioFile(uri)
            Log.e("DEBUG: Upload Result ID: ","${result?.id}")
            if (result != null) {
                _uiState.update { it.copy(isLoading = false) }
                _navigateToDraft.emit(result.id)
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun startPolling(sessionId: String) {
        viewModelScope.launch {
            repo.pollForDraft(sessionId).collect { draft ->
                // ... logic to update list ...
                if (draft?.status == "REVIEW_PENDING") {
                    _uiState.update { it.copy(isLoading = false) }
                    _navigateToDraft.emit(draft.id)
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}