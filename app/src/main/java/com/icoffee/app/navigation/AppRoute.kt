package com.icoffee.app.navigation

import android.net.Uri

sealed class AppRoute(val route: String) {
    data object Welcome : AppRoute("welcome")
    data object Home : AppRoute("home")
    data object Discover : AppRoute("discover")
    data object Brand : AppRoute("brand")
    data object BrandDetail : AppRoute("brand/{brandId}")
    data object BrandProductDetail : AppRoute("brand/{brandId}/product/{productId}")
    data object SuggestBrand : AppRoute("brand/suggest")
    data object MyBrandSuggestions : AppRoute("brand/suggestions/mine")
    data object BrandAdminPanel : AppRoute("brand/manage")
    data object BrandManage : AppRoute("brand/manage/{brandId}")
    data object Meet : AppRoute("meet")
    data object SignUp : AppRoute("meet/signup")
    data object SignIn : AppRoute("meet/signin")
    data object Scan : AppRoute("scan")
    data object BarcodeScanner : AppRoute("scan/barcode")
    data object ScanResult : AppRoute("scan/result/{code}")
    data object Profile : AppRoute("profile")
    data object CountryBeansDetail : AppRoute("beans/country/{countryId}")
    data object Detail : AppRoute("detail/{coffeeId}")
    data object BrewingDetail : AppRoute("brewing/{methodId}")
    data object MeetList : AppRoute("meet/list")
    data object CreateMeet : AppRoute("meet/create")
    data object EventDetail : AppRoute("meet/event/{meetId}")
    data object MenuScanner : AppRoute("scan/menu")
    data object MenuScanResult : AppRoute("scan/menu/result/{scanId}")

    companion object {
        const val EDIT_MEET_ID_ARG = "editMeetId"
        const val SIGN_IN_REDIRECT_ARG = "redirect"
        val createMeetRoutePattern: String = "meet/create?$EDIT_MEET_ID_ARG={$EDIT_MEET_ID_ARG}"
        val signInRoutePattern: String = "meet/signin?$SIGN_IN_REDIRECT_ARG={$SIGN_IN_REDIRECT_ARG}"

        fun detail(coffeeId: String): String = "detail/${coffeeId}"
        fun brewingDetail(methodId: String): String = "brewing/${methodId}"
        fun countryBeansDetail(countryId: String): String = "beans/country/${countryId}"
        fun brandDetail(brandId: String): String = "brand/${brandId}"
        fun brandProductDetail(brandId: String, productId: String): String =
            "brand/${brandId}/product/${productId}"
        fun brandManage(brandId: String): String = "brand/manage/$brandId"
        fun eventDetail(meetId: String): String = "meet/event/${meetId}"
        fun signIn(redirectRoute: String? = null): String {
            val normalized = redirectRoute?.trim().orEmpty()
            return if (normalized.isBlank()) {
                SignIn.route
            } else {
                "meet/signin?$SIGN_IN_REDIRECT_ARG=${Uri.encode(normalized)}"
            }
        }
        fun createMeet(editMeetId: String? = null): String =
            if (editMeetId.isNullOrBlank()) CreateMeet.route else "meet/create?$EDIT_MEET_ID_ARG=$editMeetId"
        fun scanResult(code: String): String = "scan/result/$code"
        fun menuScanResult(scanId: String): String = "scan/menu/result/${scanId}"
    }
}
