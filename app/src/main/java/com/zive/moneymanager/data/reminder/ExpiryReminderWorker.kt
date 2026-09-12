package com.zive.moneymanager.data.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.zive.moneymanager.di.AppContainer
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Runs once a day and posts a notification when something is close to its expiry
 * date. WorkManager rather than an alarm, so the schedule survives reboots and
 * app updates without a BOOT_COMPLETED receiver of our own.
 */
class ExpiryReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = AppContainer.get(applicationContext)

        val settings = container.settings.settings.first()
        if (!settings.expiryReminderEnabled) return Result.success()

        val today = LocalDate.now()
        val items = container.repository.expiringByOnce(settings.expiryHorizon(today))
        ExpiryNotifier.notifyExpiring(applicationContext, items, today)

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "expiry-reminder"

        /**
         * (Re)schedules the daily check for [hour]. Replacing the existing work is
         * what lets a changed reminder time take effect immediately.
         */
        fun schedule(context: Context, hour: Int) {
            val request = PeriodicWorkRequestBuilder<ExpiryReminderWorker>(Duration.ofDays(1))
                .setInitialDelay(delayUntilNext(hour))
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /** Time from now until the next occurrence of [hour] o'clock. */
        internal fun delayUntilNext(
            hour: Int,
            now: LocalDateTime = LocalDateTime.now(ZoneId.systemDefault()),
        ): Duration {
            val todayAtHour = now.toLocalDate().atTime(LocalTime.of(hour.coerceIn(0, 23), 0))
            val next = if (todayAtHour.isAfter(now)) todayAtHour else todayAtHour.plusDays(1)
            return Duration.between(now, next)
        }
    }
}
