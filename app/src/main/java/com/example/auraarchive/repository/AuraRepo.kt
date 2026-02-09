package com.example.auraarchive.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.auraarchive.network.AuraApiService
import com.example.auraarchive.network.model.AuraPost
import com.example.auraarchive.network.model.UploadResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuraRepo @Inject constructor(
    private val api: AuraApiService,
    @ApplicationContext private val context: Context
) {
    fun fetchFeed(): Flow<List<AuraPost>> = flow {
        emit(api.getFeed())
    }

    fun pollForDraft(sessionId: String): Flow<AuraPost?> = flow {
        var attempts = 0
        while (attempts < 40) {
            val drafts = try {
                api.getDrafts()
            } catch (e: Exception) {
                emptyList()
            }
            Log.d("AuraRepo", "Searching for sessionId: $sessionId")
            drafts.forEach { Log.d("AuraRepo", "Checking against Draft ID: ${it.id}") }

            val current = drafts.find { it.id == sessionId }
            emit(current)

            if (current != null && (current.status == "REVIEW_PENDING" || current.status == "FAILED")) {
                return@flow
            }

            delay(5000)
            attempts++
        }
    }

    suspend fun getDraftsList(): List<AuraPost> {
        return try {
            api.getDrafts()
        } catch (e: Exception) {
            Log.e("AuraRepo", "Error fetching drafts: ${e.message}")
            emptyList()
        }
    }

    suspend fun getDraftById(postId: String): AuraPost? {
        return getDraftsList().find { it.id == postId }
    }


    suspend fun uploadAudioFile(uri: Uri): UploadResult? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val requestFile = bytes.toRequestBody("audio/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", "upload.mp3", requestFile)
            api.uploadFile(body)
        } catch (e: Exception) { null }
    }

    suspend fun updateDraft(id: String, title: String, summary: String, content: String) {
        val updateMap = mapOf(
            "title" to title,
            "summary" to summary,
            "blog_markdown" to content
        )
        api.updateDraft(id, updateMap)
    }

    suspend fun publishPost(id: String) {
        api.publish(id)
    }
}