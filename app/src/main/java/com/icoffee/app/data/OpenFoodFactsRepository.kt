package com.icoffee.app.data

import com.icoffee.app.data.model.OpenFoodFactsProduct
import com.icoffee.app.data.remote.OpenFoodFactsApi
import com.icoffee.app.data.remote.OpenFoodFactsApiFactory
import com.icoffee.app.data.remote.model.OpenFoodFactsProductDto
import retrofit2.HttpException
import java.io.IOException

sealed class ProductLookupResult {
    data class Found(val product: OpenFoodFactsProduct) : ProductLookupResult()
    data object NotFound : ProductLookupResult()
    data class Error(val message: String) : ProductLookupResult()
}

class OpenFoodFactsRepository(
    private val api: OpenFoodFactsApi = OpenFoodFactsApiFactory.api
) {

    suspend fun lookupByBarcode(barcode: String): ProductLookupResult {
        val normalizedBarcode = barcode.filter(Char::isDigit)
        if (normalizedBarcode.isBlank()) return ProductLookupResult.NotFound

        return try {
            val response = api.getProductByBarcode(normalizedBarcode)
            val productDto = response.product
            val hasProduct = response.status == 1 && productDto != null

            if (!hasProduct) {
                ProductLookupResult.NotFound
            } else {
                val product = productDto.toDomain(normalizedBarcode)
                if (product.name.isBlank() && product.genericName.isNullOrBlank()) {
                    ProductLookupResult.NotFound
                } else {
                    ProductLookupResult.Found(product)
                }
            }
        } catch (_: HttpException) {
            ProductLookupResult.Error("We couldn’t reach the product database right now.")
        } catch (_: IOException) {
            ProductLookupResult.Error("We couldn’t reach the product database right now.")
        } catch (_: Exception) {
            ProductLookupResult.Error("Something went wrong while checking this barcode.")
        }
    }

    private fun OpenFoodFactsProductDto.toDomain(barcode: String): OpenFoodFactsProduct {
        val normalizedName = productName.cleanOrNull()
            ?: genericName.cleanOrNull()
            ?: ""

        return OpenFoodFactsProduct(
            barcode = barcode,
            name = normalizedName,
            brand = brands.firstSegmentOrNull(),
            countries = countries.cleanOrNull(),
            categories = categories.cleanOrNull(),
            imageUrl = imageFrontUrl.cleanOrNull() ?: imageUrl.cleanOrNull(),
            genericName = genericName.cleanOrNull(),
            quantity = quantity.cleanOrNull() ?: productQuantity.cleanOrNull(),
            packaging = packaging.cleanOrNull(),
            ingredientsText = ingredientsText.cleanOrNull(),
            labels = labels.cleanOrNull(),
            stores = stores.cleanOrNull()
        )
    }

    private fun String?.cleanOrNull(): String? {
        if (this == null) return null
        val value = trim()
        return value.takeIf { it.isNotEmpty() }
    }

    private fun String?.firstSegmentOrNull(): String? =
        cleanOrNull()
            ?.split(",")
            ?.map { it.trim() }
            ?.firstOrNull { it.isNotEmpty() }
}
