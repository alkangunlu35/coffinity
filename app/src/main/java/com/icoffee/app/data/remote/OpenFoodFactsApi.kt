package com.icoffee.app.data.remote

import com.icoffee.app.data.remote.model.OpenFoodFactsProductResponse
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApi {
    @GET("/api/v2/product/{barcode}")
    suspend fun getProductByBarcode(
        @Path("barcode") barcode: String
    ): OpenFoodFactsProductResponse
}

object OpenFoodFactsApiFactory {
    private const val BASE_URL = "https://world.openfoodfacts.org/"
    private const val USER_AGENT = "Coffinity/1.0 (alkan@coffinity.net)"

    private val userAgentInterceptor = Interceptor { chain ->
        val request = chain.request()
            .newBuilder()
            .header("User-Agent", USER_AGENT)
            .build()
        chain.proceed(request)
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .build()
    }

    val api: OpenFoodFactsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFoodFactsApi::class.java)
    }
}
