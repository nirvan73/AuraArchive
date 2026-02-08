package com.example.auraarchive.network

import com.example.auraarchive.network.model.AuraPost
import com.example.auraarchive.network.model.UploadResult
import okhttp3.MultipartBody
import retrofit2.http.*

interface AuraApiService {
    @GET("api/feed")
    suspend fun getFeed(): List<AuraPost>

    @GET("api/drafts")
    suspend fun getDrafts(): List<AuraPost>

    @Multipart
    @POST("api/upload")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part
    ): UploadResult

    @PUT("api/save/{id}")
    suspend fun updateDraft(
        @Path("id") id: String,
        @Body updates: Map<String, String>
    ): Map<String, String>

    @POST("api/publish/{id}")
    suspend fun publish(
        @Path("id") id: String
    ): Map<String, String>
}