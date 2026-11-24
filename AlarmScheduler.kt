package com.example.myreminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

/**
 * קביעת התראות עם AlarmManager.setAlarmClock:
 * מדויק לדקה, עובד גם כשהאפליקציה סגורה ובמצב חיסכון, ולא דורש הרשאה מיוחדת.
 */
object AlarmScheduler {

    const val ACTION_FIRE = "com.example.myreminders.FIRE"
    const val ACTION_DISMISS = "com.example.myreminders.DISMISS"
    const val ACTION_SNOOZE = "com.example.myreminders.SNOOZE"
    const val EXTRA_ID = "reminder_id"
    const val EXTRA_SNOOZE = "is_snooze"
    const val SNOOZE_MINUTES = 10

    private const val SNOOZE_OFFSET = 1_000_000

    fun schedule(ctx: Context, r: Reminder) {
        if (!r.enabled || r.timeMillis <= System.currentTimeMillis()) return
        setAlarm(ctx, r.timeMillis, firePending(ctx, r.id, snooze = false), r.id)
    }

    /** מבטל את כל ההתראות העתידיות והמוצגות של תזכורת. */
    fun cancel(ctx: Context, id: Int) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(firePending(ctx, id, snooze = false))
        am.cancel(firePending(ctx, id, snooze = true))
        NotificationManagerCompat.from(ctx).cancel(id)
    }

    fun snooze(ctx: Context, id: Int, minutes: Int = SNOOZE_MINUTES) {
        NotificationManagerCompat.from(ctx).cancel(id)
        val whenMs = System.currentTimeMillis() + minutes * 60_000L
        setAlarm(ctx, whenMs, firePending(ctx, id, snooze = true), id)
    }

    fun dismiss(ctx: Context, id: Int) {
        NotificationManagerCompat.from(ctx).cancel(id)
    }

    /** קובע מחדש את כל ההתראות (אחרי הפעלה מחדש של הטלפון / עדכון אפליקציה). */
    fun rescheduleAll(ctx: Context) {
        val store = ReminderStore(ctx)
        val now = System.currentTimeMillis()
        store.getAll().filter { it.enabled }.forEach { original ->
            var r = original
            if (r.timeMillis <= now) {
                if (r.repeat == Repeat.NONE) {
                    store.save(r.copy(enabled = false))
                    return@forEach
                }
                r = r.copy(timeMillis = nextTime(r, now))
                store.save(r)
            }
            schedule(ctx, r)
        }
    }

    /** הזמן הבא של תזכורת חוזרת, אחרי "עכשיו". */
    fun nextTime(r: Reminder, now: Long): Long {
        val step = when (r.repeat) {
            Repeat.DAILY -> 1
            Repeat.WEEKLY -> 7
            Repeat.NONE -> return r.timeMillis
        }
        val cal = Calendar.getInstance().apply { timeInMillis = r.timeMillis }
        while (cal.timeInMillis <= now) cal.add(Calendar.DAY_OF_YEAR, step)
        return cal.timeInMillis
    }

    private fun setAlarm(ctx: Context, whenMs: Long, operation: PendingIntent, id: Int) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val showIntent = PendingIntent.getActivity(
            ctx, id, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(whenMs, showIntent), operation)
    }

    private fun firePending(ctx: Context, id: Int, snooze: Boolean): PendingIntent {
        val intent = Intent(ctx, ReminderReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(EXTRA_ID, id)
            .putExtra(EXTRA_SNOOZE, snooze)
        val requestCode = if (snooze) id + SNOOZE_OFFSET else id
        return PendingIntent.getBroadcast(
            ctx, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
