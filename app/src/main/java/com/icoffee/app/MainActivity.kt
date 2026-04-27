package com.icoffee.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.icoffee.app.analytics.AnalyticsEvents
import com.icoffee.app.analytics.AnalyticsParams
import com.icoffee.app.analytics.AnalyticsProvider
import com.icoffee.app.localization.AppLocaleManager
import com.icoffee.app.notifications.NotificationTapRouter
import com.icoffee.app.ui.theme.ICoffeeTheme

class MainActivity : AppCompatActivity() {
    companion object {
        private var hasTrackedAppOpenInProcess = false
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocaleManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appOpenSource = if (!hasTrackedAppOpenInProcess && savedInstanceState == null) {
            "cold_start"
        } else {
            "warm_start"
        }
        hasTrackedAppOpenInProcess = true
        AnalyticsProvider.tracker.logEvent(
            AnalyticsEvents.APP_OPEN,
            mapOf(AnalyticsParams.SOURCE to appOpenSource)
        )
        NotificationTapRouter.handleIntent(intent, source = "activity_onCreate")
        AppLocaleManager.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            ICoffeeTheme {
                ICoffeeApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        NotificationTapRouter.handleIntent(intent, source = "activity_onNewIntent")
    }
}
