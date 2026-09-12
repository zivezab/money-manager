package com.zive.moneymanager

import android.app.Application
import com.zive.moneymanager.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class MoneyManagerApp : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        appScope.launch {
            // First launch gets the starter accounts and category tree.
            val created = container.repository.seedIfEmpty()
            if (created) container.settings.setSeeded(true)

            // Catch up any scheduled transactions that fell due while the app was closed.
            val today = LocalDate.now()
            val lastRun = container.settings.settings.first().lastRecurringRunDay
            if (lastRun != today.toEpochDay()) {
                container.recurringEngine.postDue(today)
                container.settings.setLastRecurringRun(today.toEpochDay())
            }
        }
    }
}
