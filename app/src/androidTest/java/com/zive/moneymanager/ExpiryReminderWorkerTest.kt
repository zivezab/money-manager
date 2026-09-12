package com.zive.moneymanager

import android.Manifest
import android.app.NotificationManager
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.zive.moneymanager.data.db.entity.InventoryItem
import com.zive.moneymanager.data.db.entity.ItemStatus
import com.zive.moneymanager.data.reminder.ExpiryNotifier
import com.zive.moneymanager.data.reminder.ExpiryReminderWorker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/**
 * Exercises the reminder end to end against the real database and the real
 * notification manager: seed something about to expire, run the worker, and
 * check a notification naming it actually reaches the shade.
 */
@RunWith(AndroidJUnit4::class)
class ExpiryReminderWorkerTest {

    /**
     * A test install starts with runtime permissions reset, so grant the one the
     * notifier checks — otherwise every assertion here would pass vacuously.
     */
    @get:Rule
    val notificationPermission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val app = context.applicationContext as MoneyManagerApp
    private val repo get() = app.container.repository
    private val notifications =
        context.getSystemService(NotificationManager::class.java)

    private val marker = "ZZTest Yoghurt"
    private val createdIds = mutableListOf<Long>()

    @Before
    fun setUp() {
        notifications.cancelAll()
        assertTrue(
            "the permission rule should have made posting possible",
            ExpiryNotifier.canPost(context),
        )
    }

    @After
    fun tearDown() = runBlocking {
        createdIds.forEach { repo.deleteInventoryItem(it) }
        notifications.cancelAll()
    }

    private fun seed(name: String, expiresInDays: Long, status: ItemStatus = ItemStatus.IN_STOCK) =
        runBlocking {
            val id = repo.upsertInventoryItem(
                InventoryItem(
                    name = name,
                    nameKey = InventoryItem.keyOf(name),
                    quantity = 1.0,
                    expiryEpochDay = LocalDate.now().plusDays(expiresInDays).toEpochDay(),
                    status = status,
                )
            )
            createdIds += id
            id
        }

    private fun runWorker(): ListenableWorker.Result = runBlocking {
        TestListenableWorkerBuilder<ExpiryReminderWorker>(context).build().doWork()
    }

    /**
     * notify() hands off to the notification service asynchronously, so a read
     * straight after posting can still come back empty. Poll briefly rather than
     * sleeping a fixed amount.
     */
    private fun awaitNotifications(timeoutMs: Long = 5_000): Array<StatusBarNotification> {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            val current = notifications.activeNotifications
            if (current.isNotEmpty()) return current
            SystemClock.sleep(100)
        }
        return notifications.activeNotifications
    }

    /** Title and body of everything currently in the shade, joined. */
    private fun postedText(): String = notifications.activeNotifications.joinToString(" ") {
        val extras = it.notification.extras
        listOfNotNull(
            extras.getCharSequence("android.title")?.toString(),
            extras.getCharSequence("android.text")?.toString(),
        ).joinToString(" ")
    }

    /**
     * Asserts the marker was not chased. Scoped to the marker rather than to an
     * empty shade because the device may already hold unrelated inventory, and a
     * notification about *that* is correct behaviour.
     */
    private fun assertMarkerNotMentioned(message: String) {
        SystemClock.sleep(1_000)
        val text = postedText()
        assertFalse("$message, but posted: $text", text.contains(marker))
    }

    /** Asserts the feature posted nothing at all, which is only ever right when
     *  the reminder itself is switched off. */
    private fun assertNothingPosted(message: String) {
        SystemClock.sleep(1_000)
        assertEquals(message, 0, notifications.activeNotifications.size)
    }

    /** Guards the notification itself: channel, icon and content all valid. */
    @Test
    fun notifierPostsDirectly() {
        val id = seed(marker, expiresInDays = 1)
        val detail = runBlocking {
            repo.expiringByOnce(LocalDate.now().plusDays(7))
        }.first { it.item.id == id }
        // Posting just this one item keeps the assertion independent of any other
        // inventory already on the device.

        ExpiryNotifier.notifyExpiring(context, listOf(detail), LocalDate.now())

        assertTrue("notifier itself posted nothing", awaitNotifications().isNotEmpty())
        assertTrue("posted text should name the item", postedText().contains(marker))
    }

    @Test
    fun postsANotificationNamingTheExpiringItem() {
        // Long overdue, so it sorts ahead of anything else the device may hold
        // and is certain to appear among the names the notification lists.
        seed(marker, expiresInDays = -9_000)
        runBlocking { app.container.settings.setExpiryReminderEnabled(true) }

        assertEquals(ListenableWorker.Result.success(), runWorker())

        assertTrue("expected a notification, found none", awaitNotifications().isNotEmpty())

        val text = postedText()
        assertTrue("notification should name the item, was: $text", text.contains(marker))
    }

    @Test
    fun staysSilentWhenTheReminderIsSwitchedOff() {
        seed(marker, expiresInDays = 1)
        runBlocking { app.container.settings.setExpiryReminderEnabled(false) }

        assertEquals(ListenableWorker.Result.success(), runWorker())
        assertNothingPosted("a disabled reminder must post nothing")

        runBlocking { app.container.settings.setExpiryReminderEnabled(true) }
    }

    @Test
    fun staysSilentWhenNothingIsNearItsDate() {
        // Far outside the default 7 day horizon.
        seed(marker, expiresInDays = 400)
        runBlocking { app.container.settings.setExpiryReminderEnabled(true) }

        assertEquals(ListenableWorker.Result.success(), runWorker())
        assertMarkerNotMentioned("an item 400 days out should not be chased")
    }

    @Test
    fun ignoresThingsAlreadyOutOfStock() {
        seed(marker, expiresInDays = 1, status = ItemStatus.USED_UP)
        runBlocking { app.container.settings.setExpiryReminderEnabled(true) }

        assertEquals(ListenableWorker.Result.success(), runWorker())
        assertMarkerNotMentioned("a used-up item should not be chased")
    }
}
