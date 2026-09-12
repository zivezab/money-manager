package com.zive.moneymanager.data.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.zive.moneymanager.MainActivity
import com.zive.moneymanager.R
import com.zive.moneymanager.data.db.model.InventoryItemDetail
import java.time.LocalDate

/** Builds and posts the daily "something is going off" notification. */
object ExpiryNotifier {

    private const val TAG = "ExpiryNotifier"
    private const val CHANNEL_ID = "expiry_reminders"
    private const val NOTIFICATION_ID = 2001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Expiry reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "A daily summary of items about to expire"
            setShowBadge(true)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /**
     * Posts a summary of [items]. Does nothing when the list is empty, so a day
     * with nothing expiring stays silent.
     */
    fun notifyExpiring(context: Context, items: List<InventoryItemDetail>, today: LocalDate) {
        if (items.isEmpty() || !canPost(context)) return
        ensureChannel(context)

        val todayEpoch = today.toEpochDay()
        val expired = items.count { (it.item.expiryEpochDay ?: Long.MAX_VALUE) < todayEpoch }
        val dueToday = items.count { it.item.expiryEpochDay == todayEpoch }
        val upcoming = items.size - expired - dueToday

        val title = when {
            expired > 0 && upcoming + dueToday > 0 ->
                "$expired expired, ${dueToday + upcoming} expiring soon"
            expired > 0 -> if (expired == 1) "1 item has expired" else "$expired items have expired"
            dueToday > 0 -> if (dueToday == 1) "1 item expires today" else "$dueToday items expire today"
            else -> if (items.size == 1) "1 item expiring soon" else "${items.size} items expiring soon"
        }

        // Name the first few so the notification is useful without opening the app.
        val names = items.take(4).joinToString(", ") { it.item.name }
        val body = if (items.size > 4) "$names and ${items.size - 4} more" else names

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_OPEN_EXPIRY, true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification: Notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_expiry)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // The permission can be revoked between canPost() and here; there is
            // nothing useful to do about it, and it must not crash a background job.
            Log.w(TAG, "Notification permission went away before posting", e)
        }
    }
}
