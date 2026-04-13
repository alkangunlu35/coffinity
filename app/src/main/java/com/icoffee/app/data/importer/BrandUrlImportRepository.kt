package com.icoffee.app.data.importer

import com.google.firebase.FirebaseApp
import com.icoffee.app.data.auth.FirebaseAuthRepository
import com.icoffee.app.data.model.importer.BrandImportFailureReason
import com.icoffee.app.data.model.importer.BrandImportPreview
import com.icoffee.app.data.model.importer.BrandImportPreviewResult
import com.icoffee.app.data.remote.importer.BrandImportApi
import com.icoffee.app.data.remote.importer.BrandImportApiFactory
import com.icoffee.app.data.remote.importer.BrandImportRequestDto
import retrofit2.HttpException
import java.io.IOException
import java.net.URI
import kotlinx.coroutines.tasks.await

class BrandUrlImportRepository(
    private val api: BrandImportApi = BrandImportApiFactory.api
) {

    suspend fun importFromUrl(rawUrl: String): BrandImportPreviewResult {
        val normalizedUrl = normalizeUrl(rawUrl)
            ?: return BrandImportPreviewResult.Failure(BrandImportFailureReason.INVALID_URL)

        val endpoint = resolveImportEndpoint()
            ?: return BrandImportPreviewResult.Failure(BrandImportFailureReason.UNKNOWN)

        val idToken = runCatching {
            FirebaseAuthRepository.currentUser?.getIdToken(false)?.await()?.token
        }.getOrNull()?.trim().orEmpty()
        if (idToken.isBlank()) {
            return BrandImportPreviewResult.Failure(BrandImportFailureReason.UNAUTHORIZED)
        }

        return try {
            val response = api.importBrandFromUrl(
                endpoint = endpoint,
                authorization = "Bearer $idToken",
                request = BrandImportRequestDto(url = normalizedUrl)
            )

            val preview = BrandImportPreview(
                sourceUrl = response.sourceUrl?.trim().orEmpty().ifBlank { normalizedUrl },
                detectedBrandName = response.detectedBrandName.cleanOrNull(),
                detectedDescription = response.detectedDescription.cleanOrNull(),
                detectedLogoUrl = response.detectedLogoUrl.cleanOrNull(),
                detectedCoverImageUrl = response.detectedCoverImageUrl.cleanOrNull(),
                detectedCountry = response.detectedCountry.cleanOrNull(),
                detectedCity = response.detectedCity.cleanOrNull(),
                detectedWebsite = response.detectedWebsite.cleanOrNull(),
                detectedInstagram = response.detectedInstagram.cleanOrNull(),
                extractionWarnings = response.extractionWarnings.orEmpty().mapNotNull { it.cleanOrNull() },
                extractionConfidenceNotes = response.extractionConfidenceNotes.orEmpty()
                    .mapNotNull { it.cleanOrNull() }
            )

            val hasCoreContent = preview.detectedBrandName != null ||
                preview.detectedDescription != null ||
                preview.detectedLogoUrl != null ||
                preview.detectedCoverImageUrl != null ||
                preview.detectedWebsite != null ||
                preview.detectedInstagram != null

            if (!hasCoreContent) {
                BrandImportPreviewResult.Failure(BrandImportFailureReason.NO_DATA)
            } else {
                BrandImportPreviewResult.Success(preview)
            }
        } catch (http: HttpException) {
            when (http.code()) {
                400 -> BrandImportPreviewResult.Failure(BrandImportFailureReason.INVALID_URL)
                401, 403 -> BrandImportPreviewResult.Failure(BrandImportFailureReason.UNAUTHORIZED)
                404, 422 -> BrandImportPreviewResult.Failure(BrandImportFailureReason.NO_DATA)
                408, 429, 500, 502, 503, 504 ->
                    BrandImportPreviewResult.Failure(BrandImportFailureReason.UNREACHABLE)

                else -> BrandImportPreviewResult.Failure(BrandImportFailureReason.UNKNOWN)
            }
        } catch (_: IOException) {
            BrandImportPreviewResult.Failure(BrandImportFailureReason.UNREACHABLE)
        } catch (_: Exception) {
            BrandImportPreviewResult.Failure(BrandImportFailureReason.UNKNOWN)
        }
    }

    private fun resolveImportEndpoint(): String? {
        val projectId = runCatching { FirebaseApp.getInstance().options.projectId }
            .getOrNull()
            ?.trim()
            .orEmpty()
        if (projectId.isBlank()) return null
        return "https://us-central1-$projectId.cloudfunctions.net/importBrandFromUrl"
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

    private fun String?.cleanOrNull(): String? = this?.trim()?.takeIf { it.isNotBlank() }
}
