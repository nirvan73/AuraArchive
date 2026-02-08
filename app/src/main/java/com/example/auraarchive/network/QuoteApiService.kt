package com.example.auraarchive.network

import com.example.auraarchive.network.model.QuoteResponse
import retrofit2.http.GET

interface QuoteApiService {
    @GET("quotes/random")
    suspend fun getRandomQuote(): List<QuoteResponse> // Returns a List
}