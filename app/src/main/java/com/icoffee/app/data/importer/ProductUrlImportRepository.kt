package com.icoffee.app.data.importer

import com.google.firebase.FirebaseApp
import com.icoffee.app.data.model.importer.ProductImportFailureReason
import com.icoffee.app.data.model.importer.ProductImportPreview
import com.icoffee.app.data.model.importer.ProductImportPreviewResult
import com.icoffee.app.data.remote.importer.ProductImportApi
import com.icoffee.app.data.remote.importer.ProductImportApiFactory
import com.icoffee.app.data.remote.importer.ProductImportRequestDto
import retrofit2.HttpException
import java.io.IOException
import java.net.URI

class ProductUrlImportRepository(
    private val api: ProductImportApi = ProductImportApiFactory.api
) {

    suspend fun importFromUrl(rawUrl: String): ProductImportPreviewResult {
        val normalizedUrl = normalizeUrl(rawUrl)
            ?: return ProductImportPreviewResult.Failure(ProductImportFailureReason.INVALID_URL)

        val endpoint = resolveImportEndpoint()
            ?: return ProductImportPreviewResult.Failure(ProductImportFailureReason.UNKNOWN)

        return try {
            val response = api.importProductFromUrl(
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
