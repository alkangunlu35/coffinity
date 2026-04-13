package com.icoffee.app.data.remote.importer

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

data class BrandImportRequestDto(
    val url: String
)

data class BrandImportResponseDto(
    val sourceUrl: String? = null,
    val detectedBrandName: String? = null,
    val detectedDescription: String? = null,
    val detectedLogoUrl: String? = null,
    val detectedCoverImageUrl: String? = null,
    val detectedCountry: String? = null,
    val detectedCity: String? = null,
    val detectedWebsite: String? = null,
    val detectedInstagram: String? = null,
    val extractionWarnings: List<String>? = null,
    val extractionConfidenceNotes: List<String>? = null
)

interface BrandImportApi {
    @POST
    suspend fun importBrandFromUrl(
        @Url endpoint: String,
        @Header("Authorization") authorization: String,
        @Body request: BrandImportRequestDto
    ): BrandImportResponseDto
}

object BrandImportApiFactory {
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    val api: BrandImportApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://example.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BrandImportApi::class.java)
    }
}
