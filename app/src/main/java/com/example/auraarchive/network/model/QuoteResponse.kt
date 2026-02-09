package com.example.auraarchive.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuoteResponse(
    @SerialName("content")
    val quote: String,
    @SerialName("author")
    val author: String,
    val tags: List<String> = emptyList()
)