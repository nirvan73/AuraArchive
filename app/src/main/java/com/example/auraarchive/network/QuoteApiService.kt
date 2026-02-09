package com.example.auraarchive.network

import com.example.auraarchive.network.model.QuoteResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface QuoteApiService {
    @GET("random")
    suspend fun getRandomQuote(
        @Query("tags") tags: String = "technology,famous-quotes"
    ): QuoteResponse
}