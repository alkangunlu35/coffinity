// FILE: app/src/main/java/com/icoffee/app/data/importer/ProductUrlImportRepository.kt
// FULL REPLACEMENT

package com.icoffee.app.data.importer

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.icoffee.app.data.model.importer.ProductImportFailureReason
import com.icoffee.app.data.model.importer.ProductImportPreview
import com.icoffee.app.data.model.importer.ProductImportPreviewResult
import com.icoffee.app.data.remote.importer.ProductImportApi
import com.icoffee.app.data.remote.importer.ProductImportApiFactory
import com.icoffee.app.data.remote.importer.ProductImportRequestDto
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.net.URI
import java.util.concurrent.TimeUnit

class ProductUrlImportRepository(
    private val api: ProductImportApi = ProductImportApiFactory.api
) {

    suspend fun importFromUrl(rawUrl: String): ProductImportPreviewResult {
        val normalizedUrl = normalizeUrl(rawUrl)
            ?: return ProductImportPreviewResult.Failure(ProductImportFailureReason.INVALID_URL)

        val endpoint = resolveImportEndpoint()
            ?: return ProductImportPreviewResult.Failure(ProductImportFailureReason.UNKNOWN)

        val token = getIdToken()
            ?: return ProductImportPreviewResult.Failure(ProductImportFailureReason.UNKNOWN)

        val authedApi = createAuthedApi(token)

        return try {
            val response = authedApi.importProductFromUrl(
                endpoint = endpoint,
                request = ProductImportRequestDto(url = normalizedUrl)
            )

            val preview = ProductImportPreview(
                sourceUrl = response.sourceUrl?.trim().orEmpty().ifBlank { normalizedUrl },
                detectedBrandName = response.detectedBrandName?.cleanOrNull(),
                detectedProductName = response.detectedProductName?.cleanOrNull(),
                detectedDescription = response.detectedDescription?.cleanOrNull(),
                detectedImageUrl = response.detectedImageUrl?.cleanOrNull(),
                detectedOrigin = response.detectedOrigin?.cleanOrNull(),
                detectedRoastLevel = response.detectedRoastLevel?.cleanOrNull(),
                detectedProcess = response.detectedProcess?.cleanOrNull(),
                detectedTastingNotes = response.detectedTastingNotes.orEmpty().mapNotNull { it.cleanOrNull() },
                barcode = response.barcode?.cleanOrNull(),
                extractionWarnings = response.extractionWarnings.orEmpty().mapNotNull { it.cleanOrNull() },
                extractionConfidenceNotes = response.extractionConfidenceNotes.orEmpty()
                    .mapNotNull { it.cleanOrNull() }
            )

            val hasCoreContent = preview.detectedProductName != null ||
                preview.detectedDescription != null ||
                preview.detectedImageUrl != null ||
                preview.detectedOrigin != null ||
                preview.detectedRoastLevel != null ||
                preview.detectedProcess != null ||
                preview.detectedTastingNotes.isNotEmpty() ||
                preview.barcode != null

            if (!hasCoreContent) {
                ProductImportPreviewResult.Failure(ProductImportFailureReason.NO_DATA)
            } else {
                ProductImportPreviewResult.Success(preview)
            }
        } catch (http: HttpException) {
            when (http.code()) {
                400 -> ProductImportPreviewResult.Failure(ProductImportFailureReason.INVALID_URL)
                401, 403 -> ProductImportPreviewResult.Failure(ProductImportFailureReason.UNKNOWN)
                404, 422 -> ProductImportPreviewResult.Failure(ProductImportFailureReason.NO_DATA)
                408, 429, 500, 502, 503, 504 ->
                    ProductImportPreviewResult.Failure(ProductImportFailureReason.UNREACHABLE)
                else -> ProductImportPreviewResult.Failure(ProductImportFailureReason.UNKNOWN)
            }
        } catch (_: IOException) {
            ProductImportPreviewResult.Failure(ProductImportFailureReason.UNREACHABLE)
        } catch (_: Exception) {
            ProductImportPreviewResult.Failure(ProductImportFailureReason.UNKNOWN)
        }
    }

    private suspend fun getIdToken(): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return try {
            user.getIdToken(true).await().token
        } catch (_: Exception) {
            null
        }
    }

    private fun createAuthedApi(token: String): ProductImportApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request: Request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl("https://example.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ProductImportApi::class.java)
    }

    private fun resolveImportEndpoint(): String? {
        val projectId = runCatching { FirebaseApp.getInstance().options.projectId }
            .getOrNull()
            ?.trim()
            .orEmpty()
        if (projectId.isBlank()) return null
        return "https://us-central1-$projectId.cloudfunctions.net/importProductFromUrl"
    }

    private fun normalizeUrl(rawUrl: String): String? {
        val value = rawUrl.trim()
        if (value.isBlank()) return null
        val candidate = if (value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true)
        ) {
            value
        } else {
            "https://$value"
        }

        val uri = runCatching { URI(candidate) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase().orEmpty()
        val host = uri.host?.lowercase().orEmpty()
        if (scheme !in setOf("http", "https")) return null
        if (host.isBlank()) return null
        if (host == "localhost" || host.startsWith("127.") || host.endsWith(".local")) return null

        return uri.toString()
    }

    private fun String.cleanOrNull(): String? =
        trim().takeIf { it.isNotBlank() }
}