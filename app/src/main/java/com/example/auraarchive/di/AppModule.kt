package com.example.auraarchive.di

import com.example.auraarchive.network.AuraApiService
import com.example.auraarchive.network.QuoteApiService
import com.example.auraarchive.repository.QuoteRepo
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit


@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    private val BASE_URL = "https://auraarchive.onrender.com/"
    private const val QUOTE_BASE_URL = "https://api.quotable.io/"

    @Provides
    @Singleton
    fun provideQuoteApi(json: Json): QuoteApiService {
        return Retrofit.Builder()
            .baseUrl(QUOTE_BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(QuoteApiService::class.java)
    }
    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }
    }

    @Provides
    @Singleton
    fun provideAuraApi(json: Json): AuraApiService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(AuraApiService::class.java)
    }


    @Provides
    @Singleton
    fun provideQuoteRepo(api: QuoteApiService): QuoteRepo {
        return QuoteRepo(api)
    }
}