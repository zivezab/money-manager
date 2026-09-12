package com.zive.moneymanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zive.moneymanager.data.prefs.AppSettings
import com.zive.moneymanager.ui.nav.MoneyManagerRoot
import com.zive.moneymanager.ui.theme.MoneyManagerTheme

class MainActivity : ComponentActivity() {

    companion object {
        /** Set by the expiry notification so the app opens on the checklist. */
        const val EXTRA_OPEN_EXPIRY = "open_expiry_checklist"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as MoneyManagerApp).container

        setContent {
            val settings by container.settings.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            MoneyManagerTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
            ) {
                MoneyManagerRoot(
                    settings = settings,
                    // Tapping the expiry notification should land on the checklist.
                    openExpiryChecklist = intent?.getBooleanExtra(EXTRA_OPEN_EXPIRY, false) == true,
                )
            }
        }
    }
}
