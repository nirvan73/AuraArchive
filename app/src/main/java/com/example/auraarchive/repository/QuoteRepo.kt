package com.example.auraarchive.repository

import android.util.Log
import com.example.auraarchive.network.QuoteApiService
import com.example.auraarchive.network.model.QuoteResponse
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class QuoteRepo @Inject constructor(
    private val quoteApi: QuoteApiService
) {
    suspend fun getRandomQuote(): QuoteResponse? {
        return try {
            val response = quoteApi.getRandomQuote()
            // response is a List, so we take the first item
            response.firstOrNull()
        } catch (e: Exception) {
            Log.e("QuoteRepo", "Parsing Error: ${e.message}")
            null
        }
    }
}