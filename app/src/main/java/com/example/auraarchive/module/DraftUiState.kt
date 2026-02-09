package com.example.auraarchive.module

import com.example.auraarchive.network.model.AuraPost


data class DraftUiState(
    val post: AuraPost? = null,
    val drafts: List<AuraPost> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isPublishing: Boolean = false,
    val isEditing: Boolean = false,
    val errorMessage: String? = null,
    val wasPublishedSuccessfully: Boolean = false,
    val editedTitle: String = "",
    val editedSummary: String = "",
    val editedContent: String = ""
)