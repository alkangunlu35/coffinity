package com.icoffee.app

import android.app.Application
import com.icoffee.app.analytics.AnalyticsProvider
import com.icoffee.app.data.MeetRepository
import com.icoffee.app.data.RecommendationEngine
import com.icoffee.app.data.BrandRepository
import com.icoffee.app.data.firebase.FirebaseServiceLocator
import com.icoffee.app.data.location.LocationProvider
import com.icoffee.app.data.menu.MenuScanRepository
import com.icoffee.app.data.notifications.NotificationTokenSyncManager
import com.icoffee.app.data.profile.UserTasteProfileRepository
import com.icoffee.app.data.venue.VenueRepository
import com.icoffee.app.localization.AppLocaleManager

class ICoffeeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseServiceLocator.initialize(this)
        AnalyticsProvider.initialize(this)
        AppLocaleManager.initialize(this)
        RecommendationEngine.initialize(this)
        UserTasteProfileRepository.initialize(this)
        MenuScanRepository.initialize(this)
        LocationProvider.initialize(this)
        MeetRepository.initialize(this)
        BrandRepository.initialize(this)
        VenueRepository.initialize(this)
        NotificationTokenSyncManager.initialize()
    }
}
