package com.example.auraarchive.module

import com.example.auraarchive.network.model.AuraPost

data class HomeUiState(
    val posts:List<AuraPost> = emptyList(),
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val quote: String = "Loading inspiration...",
    val author: String = ""
)