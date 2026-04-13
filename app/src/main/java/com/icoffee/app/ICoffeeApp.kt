package com.icoffee.app

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.icoffee.app.data.CoffeeRepository
import com.icoffee.app.data.MeetRepository
import com.icoffee.app.data.growth.GrowthAnalytics
import com.icoffee.app.data.growth.GrowthEventNames
import com.icoffee.app.navigation.AppRoute
import com.icoffee.app.ui.components.AppBottomBar
import com.icoffee.app.ui.screens.beans.CountryBeanDetailScreen
import com.icoffee.app.ui.screens.beans.CountryBeansScreen
import com.icoffee.app.ui.screens.brand.BrandAdminPanelScreen
import com.icoffee.app.ui.screens.brand.BrandDetailScreen
import com.icoffee.app.ui.screens.brand.BrandManagementScreen
import com.icoffee.app.ui.screens.brand.BrandProductDetailScreen
import com.icoffee.app.ui.screens.brand.BrandScreen
import com.icoffee.app.ui.screens.detail.BrewingDetailScreen
import com.icoffee.app.ui.screens.detail.CoffeeDetailScreen
import com.icoffee.app.ui.screens.discover.DiscoverHubScreen
import com.icoffee.app.ui.screens.meet.CreateMeetScreen
import com.icoffee.app.ui.screens.meet.EventDetailScreen
import com.icoffee.app.ui.screens.meet.MeetListScreen
import com.icoffee.app.ui.screens.meet.MeetScreen
import com.icoffee.app.ui.screens.meet.MeetSignInScreen
import com.icoffee.app.ui.screens.meet.MeetSignUpScreen
import com.icoffee.app.ui.screens.paywall.UpgradePaywallScreen
import com.icoffee.app.ui.screens.profile.ProfileScreen
import com.icoffee.app.ui.screens.scan.ScanResultScreen
import com.icoffee.app.ui.screens.scan.ScanScreen
import com.icoffee.app.ui.screens.scanner.BarcodeScannerScreen
import com.icoffee.app.ui.screens.scanner.MenuScanResultScreen
import com.icoffee.app.ui.screens.scanner.MenuScannerScreen
import com.icoffee.app.ui.screens.suggestions.MySuggestionsScreen
import com.icoffee.app.ui.screens.suggestions.SuggestBrandScreen
import com.icoffee.app.ui.screens.welcome.WelcomeScreen
import com.icoffee.app.viewmodel.AuthViewModel
import com.icoffee.app.viewmodel.MeetViewModel

@Composable
fun ICoffeeApp() {
    val authViewModel: AuthViewModel = viewModel()
    val meetViewModel: MeetViewModel = viewModel()
    val isUserSignedIn = authViewModel.isSignedIn

    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences("coffinity_ux", android.content.Context.MODE_PRIVATE)
    }
    var showOnboarding by rememberSaveable {
        mutableStateOf(!prefs.getBoolean("onboarding_done", false))
    }

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val bottomRoutes = setOf(
        AppRoute.Home.route,
        AppRoute.Brand.route,
        AppRoute.Meet.route,
        AppRoute.Scan.route,
        AppRoute.Profile.route
    )

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = AppRoute.Welcome.route,
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(animationSpec = tween(durationMillis = 220)) },
            exitTransition = { fadeOut(animationSpec = tween(durationMillis = 140)) },
            popEnterTransition = { fadeIn(animationSpec = tween(durationMillis = 220)) },
            popExitTransition = { fadeOut(animationSpec = tween(durationMillis = 140)) }
        ) {
            composable(AppRoute.Welcome.route) {
                WelcomeScreen(
                    onStartClick = {
                        navController.navigate(AppRoute.Home.route) {
                            popUpTo(AppRoute.Welcome.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(AppRoute.Home.route) {
                DiscoverHubScreen(
                    onBrewingMethodClick = { methodId ->
                        navController.navigate(AppRoute.brewingDetail(methodId))
                    },
                    onCountryClick = { countryId ->
                        navController.navigate(AppRoute.countryBeansDetail(countryId))
                    },
                    onOpenAllOrigins = {
                        navController.navigate(AppRoute.Discover.route)
                    }
                )
            }

            composable(AppRoute.Discover.route) {
                CountryBeansScreen(
                    onCountryClick = { countryId ->
                        navController.navigate(AppRoute.countryBeansDetail(countryId))
                    }
                )
            }

            composable(AppRoute.Brand.route) {
                BrandScreen(
                    onBrandClick = { brandId ->
                        navController.navigate(AppRoute.brandDetail(brandId))
                    },
                    onSuggestBrand = {
                        navController.navigate(AppRoute.SuggestBrand.route)
                    },
                    onOpenMySuggestions = {
                        navController.navigate(AppRoute.MyBrandSuggestions.route)
                    }
                )
            }

            composable(AppRoute.SuggestBrand.route) {
                SuggestBrandScreen(
                    onBack = { navController.popBackStack() },
                    onRequestSignIn = {
                        navController.navigate(AppRoute.signIn()) { launchSingleTop = true }
                    },
                    onOpenMySuggestions = {
                        navController.navigate(AppRoute.MyBrandSuggestions.route) { launchSingleTop = true }
                    }
                )
            }

            composable(AppRoute.MyBrandSuggestions.route) {
                MySuggestionsScreen(
                    onBack = { navController.popBackStack() },
                    onRequestSignIn = {
                        navController.navigate(AppRoute.signIn()) { launchSingleTop = true }
                    }
                )
            }

            composable(AppRoute.Meet.route) {
                MeetScreen(
                    isUserSignedIn = isUserSignedIn,
                    onCreateMeet = {
                        navController.navigate(AppRoute.CreateMeet.route) { launchSingleTop = true }
                    },
                    onRequestSignIn = {
                        navController.navigate(AppRoute.signIn()) { launchSingleTop = true }
                    },
                    onExploreAllEvents = {
                        navController.navigate(AppRoute.MeetList.route) { launchSingleTop = true }
                    },
                    onEventClick = { meetId ->
                        navController.navigate(AppRoute.eventDetail(meetId)) { launchSingleTop = true }
                    },
                    meetViewModel = meetViewModel
                )
            }

            composable(AppRoute.Scan.route) {
                ScanScreen(
                    onScanProductClick = {
                        navController.navigate(AppRoute.BarcodeScanner.route)
                    },
                    onScanMenuClick = {
                        navController.navigate(AppRoute.MenuScanner.route)
                    }
                )
            }

            composable(AppRoute.MenuScanner.route) {
                MenuScannerScreen(
                    onBack = { navController.popBackStack() },
                    onMenuScanned = { scanId ->
                        navController.navigate(AppRoute.menuScanResult(scanId)) {
                            popUpTo(AppRoute.MenuScanner.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = AppRoute.MenuScanResult.route,
                arguments = listOf(navArgument("scanId") { defaultValue = "" })
            ) { entry ->
                val scanId = entry.arguments?.getString("scanId").orEmpty()
                MenuScanResultScreen(
                    scanId = scanId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(AppRoute.BarcodeScanner.route) {
                BarcodeScannerScreen(
                    onBack = { navController.popBackStack() },
                    onBarcodeScanned = { code ->
                        navController.navigate(AppRoute.scanResult(Uri.encode(code))) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = AppRoute.ScanResult.route,
                arguments = listOf(navArgument("code") { defaultValue = "" })
            ) { entry ->
                val code = Uri.decode(entry.arguments?.getString("code").orEmpty())
                ScanResultScreen(
                    code = code,
                    onBack = { navController.popBackStack() },
                    onScanAgain = {
                        navController.navigate(AppRoute.BarcodeScanner.route) {
                            popUpTo(AppRoute.Scan.route)
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(AppRoute.Profile.route) {
                ProfileScreen(
                    authViewModel = authViewModel,
                    onRequestSignIn = {
                        navController.navigate(AppRoute.signIn()) {
                            launchSingleTop = true
                        }
                    },
                    onOpenBrandManagement = { navController.navigate(AppRoute.BrandAdminPanel.route) }
                )
            }

            composable(AppRoute.BrandAdminPanel.route) {
                BrandAdminPanelScreen(
                    onBack = { navController.popBackStack() },
                    onOpenBrand = { brandId ->
                        navController.navigate(AppRoute.brandManage(brandId))
                    }
                )
            }

            composable(
                route = AppRoute.BrandManage.route,
                arguments = listOf(navArgument("brandId") { defaultValue = "" })
            ) { entry ->
                val brandId = entry.arguments?.getString("brandId").orEmpty()
                BrandManagementScreen(
                    brandId = brandId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = AppRoute.BrandDetail.route,
                arguments = listOf(navArgument("brandId") { defaultValue = "" })
            ) { entry ->
                val brandId = entry.arguments?.getString("brandId").orEmpty()
                BrandDetailScreen(
                    brandId = brandId,
                    onBack = { navController.popBackStack() },
                    onProductClick = { productId ->
                        navController.navigate(AppRoute.brandProductDetail(brandId, productId))
                    },
                    onManageBrand = { selectedBrandId ->
                        navController.navigate(AppRoute.brandManage(selectedBrandId))
                    }
                )
            }

            composable(
                route = AppRoute.BrandProductDetail.route,
                arguments = listOf(
                    navArgument("brandId") { defaultValue = "" },
                    navArgument("productId") { defaultValue = "" }
                )
            ) { entry ->
                val brandId = entry.arguments?.getString("brandId").orEmpty()
                val productId = entry.arguments?.getString("productId").orEmpty()
                BrandProductDetailScreen(
                    brandId = brandId,
                    productId = productId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = AppRoute.CountryBeansDetail.route,
                arguments = listOf(navArgument("countryId") { defaultValue = "brazil" })
            ) { entry ->
                val countryId = entry.arguments?.getString("countryId").orEmpty()
                CountryBeanDetailScreen(
                    countryId = countryId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = AppRoute.Detail.route,
                arguments = listOf(navArgument("coffeeId") { defaultValue = "brazil_santos" })
            ) { entry ->
                val coffeeId = entry.arguments?.getString("coffeeId").orEmpty()
                val coffee = CoffeeRepository.findCoffee(coffeeId) ?: CoffeeRepository.coffees.first()
                CoffeeDetailScreen(
                    coffee = coffee,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = AppRoute.BrewingDetail.route,
                arguments = listOf(navArgument("methodId") { defaultValue = "espresso" })
            ) { entry ->
                val methodId = entry.arguments?.getString("methodId").orEmpty()
                BrewingDetailScreen(
                    methodId = methodId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(AppRoute.MeetList.route) {
                MeetListScreen(
                    onBack = { navController.popBackStack() },
                    onCreateMeet = {
                        navController.navigate(AppRoute.CreateMeet.route) { launchSingleTop = true }
                    },
                    onEventClick = { meetId ->
                        navController.navigate(AppRoute.eventDetail(meetId)) { launchSingleTop = true }
                    },
                    meetViewModel = meetViewModel
                )
            }

            composable(
                route = AppRoute.createMeetRoutePattern,
                arguments = listOf(navArgument(AppRoute.EDIT_MEET_ID_ARG) { defaultValue = "" })
            ) { entry ->
                val editMeetId = entry.arguments?.getString(AppRoute.EDIT_MEET_ID_ARG)
                    .orEmpty()
                    .ifBlank { null }

                CreateMeetScreen(
                    onBack = { navController.popBackStack() },
                    isUserSignedIn = isUserSignedIn,
                    onRequestSignIn = {
                        navController.navigate(AppRoute.signIn()) { launchSingleTop = true }
                    },
                    onOpenPaywall = { navController.navigate("paywall") },
                    meetViewModel = meetViewModel,
                    editMeetId = editMeetId
                )
            }

            composable(
                route = AppRoute.EventDetail.route,
                arguments = listOf(navArgument("meetId") { defaultValue = "" }),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "coffinity://event/{meetId}" },
                    navDeepLink { uriPattern = "https://coffinity.app/event/{meetId}" }
                )
            ) { entry ->
                val meetId = entry.arguments?.getString("meetId").orEmpty()
                val openedFromDeepLink = entry.arguments
                    ?.containsKey(NavController.KEY_DEEP_LINK_INTENT) == true
                var deepLinkLogged by rememberSaveable(meetId, openedFromDeepLink) {
                    mutableStateOf(false)
                }

                LaunchedEffect(meetId, openedFromDeepLink, deepLinkLogged) {
                    if (!deepLinkLogged && openedFromDeepLink && meetId.isNotBlank()) {
                        GrowthAnalytics.log(
                            GrowthEventNames.DEEP_LINK_OPENED,
                            params = mapOf("target" to "event", "eventId" to meetId)
                        )
                        GrowthAnalytics.log(
                            GrowthEventNames.DEEP_LINK_EVENT_OPENED,
                            params = mapOf("eventId" to meetId)
                        )
                        deepLinkLogged = true
                    }
                }
                EventDetailScreen(
                    meetId = meetId,
                    onBack = { navController.popBackStack() },
                    isUserSignedIn = isUserSignedIn,
                    onRequestSignIn = {
                        navController.navigate(
                            AppRoute.signIn(
                                redirectRoute = AppRoute.eventDetail(meetId)
                            )
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onEditEvent = { selectedMeetId ->
                        navController.navigate(AppRoute.createMeet(selectedMeetId))
                    },
                    onOpenPaywall = { navController.navigate("paywall") },
                    meetViewModel = meetViewModel
                )
            }

            composable(AppRoute.SignUp.route) {
                MeetSignUpScreen(
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() },
                    onJoinCommunity = { navController.popBackStack() },
                    onSignIn = {
                        navController.navigate(AppRoute.signIn()) {
                            popUpTo(AppRoute.SignUp.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(
                route = AppRoute.signInRoutePattern,
                arguments = listOf(navArgument(AppRoute.SIGN_IN_REDIRECT_ARG) { defaultValue = "" })
            ) { entry ->
                val redirectRoute = entry.arguments?.getString(AppRoute.SIGN_IN_REDIRECT_ARG).orEmpty()
                MeetSignInScreen(
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() },
                    onSignInSuccess = {
                        val safeRedirect = sanitizeSignInRedirectRoute(redirectRoute)
                        if (safeRedirect != null) {
                            val popped = navController.popBackStack()
                            val currentRoute = navController.currentBackStackEntry?.destination?.route
                            val alreadyOnEventDetail = safeRedirect.startsWith("meet/event/") &&
                                currentRoute == AppRoute.EventDetail.route
                            if (!popped || !alreadyOnEventDetail) {
                                runCatching {
                                    navController.navigate(safeRedirect) {
                                        popUpTo(AppRoute.SignIn.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }.onFailure {
                                    navController.navigate(AppRoute.Meet.route) {
                                        popUpTo(AppRoute.SignIn.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onCreateAccount = {
                        navController.navigate(AppRoute.SignUp.route) {
                            popUpTo(AppRoute.SignIn.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable("paywall") {
                UpgradePaywallScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }

        AnimatedVisibility(
            visible = showOnboarding,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(250))
        ) {
            OnboardingOverlay(
                onDone = {
                    prefs.edit().putBoolean("onboarding_done", true).apply()
                    showOnboarding = false
                }
            )
        }

        if (currentRoute in bottomRoutes) {
            AppBottomBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                currentDestination = backStackEntry?.destination,
                nearbyMeetCount = MeetRepository.nearbyActivityCount(),
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(AppRoute.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

private fun sanitizeSignInRedirectRoute(rawRedirect: String): String? {
    val decoded = Uri.decode(rawRedirect).trim()
    if (decoded.isBlank()) return null
    return if (decoded.startsWith("meet/event/")) decoded else null
}

private data class OnboardingStep(
    val titleRes: Int,
    val bodyRes: Int
)

@Composable
private fun OnboardingOverlay(onDone: () -> Unit) {
    val steps = remember {
        listOf(
            OnboardingStep(R.string.onboarding_step1_title, R.string.onboarding_step1_body),
            OnboardingStep(R.string.onboarding_step2_title, R.string.onboarding_step2_body),
            OnboardingStep(R.string.onboarding_step3_title, R.string.onboarding_step3_body)
        )
    }
    var step by rememberSaveable { mutableIntStateOf(0) }
    val current = steps[step]
    val isLast = step == steps.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xCC0D0705))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF3A2419), Color(0xFF2A160F))
                    )
                )
                .padding(horizontal = 28.dp, vertical = 32.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = stringResource(R.string.onboarding_step_of, step + 1, steps.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFFC8A07A)
                )
                Text(
                    text = stringResource(current.titleRes),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFF8E6D1)
                )
                Text(
                    text = stringResource(current.bodyRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xCCE0C4A4)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color(0xFFD4935A))
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = { if (isLast) onDone() else step++ }
                            )
                            .padding(horizontal = 28.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(
                                if (isLast) R.string.onboarding_done else R.string.onboarding_next
                            ),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF1A0D08)
                        )
                    }
                }
            }
        }
    }
}
