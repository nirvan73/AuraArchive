package com.example.auraarchive.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuraPost(
    val status: String,
    @SerialName("created_at") val id: String,
    val title: String? = null,
    val summary: String? = null,
    @SerialName("content") val content: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("external_links") val links: List<AuraLink> = emptyList(),
    val error: String? = null
)

@Serializable
data class AuraLink(
    val title: String,
    val url: String,
    val description: String
)

@Serializable
data class UploadResult(
    val id: String,
    val message: String
)