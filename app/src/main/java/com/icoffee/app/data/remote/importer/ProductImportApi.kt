package com.icoffee.app.data.remote.importer

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

data class ProductImportRequestDto(
    val url: String
)

data class ProductImportResponseDto(
    val sourceUrl: String? = null,
    val detectedBrandName: String? = null,
    val detectedProductName: String? = null,
    val detectedDescription: String? = null,
    val detectedImageUrl: String? = null,
    val detectedOrigin: String? = null,
    val detectedRoastLevel: String? = null,
    val detectedProcess: String? = null,
    val detectedTastingNotes: List<String>? = null,
    val barcode: String? = null,
    val extractionWarnings: List<String>? = null,
    val extractionConfidenceNotes: List<String>? = null
)

interface ProductImportApi {
    @POST
    suspend fun importProductFromUrl(
        @Url endpoint: String,
        @Body request: ProductImportRequestDto
    ): ProductImportResponseDto
}

object ProductImportApiFactory {
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    val api: ProductImportApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://example.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ProductImportApi::class.java)
    }
}
