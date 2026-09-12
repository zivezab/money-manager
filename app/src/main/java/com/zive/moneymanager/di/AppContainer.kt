package com.zive.moneymanager.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.zive.moneymanager.MoneyManagerApp
import com.zive.moneymanager.core.PeriodController
import com.zive.moneymanager.data.db.AppDatabase
import com.zive.moneymanager.data.prefs.SettingsStore
import com.zive.moneymanager.data.recurring.RecurringEngine
import com.zive.moneymanager.data.repo.FinanceRepository

/**
 * Hand-rolled dependency graph. The object count here is small enough that a DI
 * framework would cost more in build time than it saves in wiring.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.get(appContext) }
    val repository: FinanceRepository by lazy { FinanceRepository(database) }
    val settings: SettingsStore by lazy { SettingsStore(appContext) }
    val recurringEngine: RecurringEngine by lazy { RecurringEngine(repository) }

    /** Shared across tabs so the chosen month follows you between screens. */
    val periodController: PeriodController = PeriodController()

    companion object {
        @Volatile private var instance: AppContainer? = null

        /**
         * The process-wide container. Reachable from any [Context], which matters
         * for WorkManager: a worker is handed a plain context, and casting it to
         * the Application class only happens to work in production.
         *
         * Nothing is duplicated by calling this early — the database and the
         * preference store underneath are themselves singletons.
         */
        fun get(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context.applicationContext).also { instance = it }
            }
    }
}

/** Pulls the container out of the ViewModel creation extras. */
val CreationExtras.container: AppContainer
    get() = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as MoneyManagerApp).container

/** Registers every ViewModel the app can build. */
inline fun <reified VM : androidx.lifecycle.ViewModel> containerFactory(
    crossinline create: (AppContainer) -> VM,
) = viewModelFactory {
    initializer { create(container) }
}
