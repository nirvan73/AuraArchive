package com.example.auraarchive.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auraarchive.module.HomeUiState
import com.example.auraarchive.repository.AuraRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResourceViewModel @Inject constructor(
    private val repo: AuraRepo
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadResources()
    }

    fun loadResources() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repo.fetchFeed().collect { allPosts ->
                    // Filter for only published content
                    val publishedDocs = allPosts.filter { it.status == "PUBLISHED" }
                    _uiState.update { it.copy(
                        posts = publishedDocs,
                        isLoading = false
                    ) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
}