package com.icoffee.app

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.icoffee.app.localization.AppLocaleManager
import com.icoffee.app.ui.theme.ICoffeeTheme

class MainActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocaleManager.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLocaleManager.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            ICoffeeTheme {
                ICoffeeApp()
            }
        }
    }
}
