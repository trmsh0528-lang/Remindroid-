package com.example.myreminders

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    private const val CH_NOTIFICATION = "reminders_channel"
    private const val CH_ALARM = "alarms_channel"

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)

        val normal = NotificationChannel(
            CH_NOTIFICATION, "תזכורות", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "התראות רגילות לתזכורות"
            enableVibration(true)
        }

        val alarmAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val alarm = NotificationChannel(
            CH_ALARM, "שעון מעורר", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "תזכורות שמצלצלות כמו שעון מעורר"
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), alarmAttrs)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 800, 500, 800, 500, 800)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        nm.createNotificationChannels(listOf(normal, alarm))
    }

    @SuppressLint("MissingPermission")
    fun show(ctx: Context, r: Reminder) {
        createChannels(ctx)
        val nmc = NotificationManagerCompat.from(ctx)
        if (!nmc.areNotificationsEnabled()) return

        val isAlarm = r.type == AlertType.ALARM
        val text = r.description.ifBlank { "הגיע הזמן!" }

        val openApp = PendingIntent.getActivity(
            ctx, r.id,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(ctx, if (isAlarm) CH_ALARM else CH_NOTIFICATION)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(r.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(if (isAlarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openApp)
            .setAutoCancel(true)

        if (isAlarm) {
            val fullScreen = PendingIntent.getActivity(
                ctx, r.id + 4_000_000,
                Intent(ctx, AlarmActivity::class.java)
                    .putExtra(AlarmScheduler.EXTRA_ID, r.id)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder
                .setFullScreenIntent(fullScreen, true)
                .setTimeoutAfter(60_000)
                .addAction(R.drawable.ic_notification, "נודניק ${AlarmScheduler.SNOOZE_MINUTES} דק׳", actionPending(ctx, r.id, AlarmScheduler.ACTION_SNOOZE, 2_000_000))
                .addAction(R.drawable.ic_notification, "בטל", actionPending(ctx, r.id, AlarmScheduler.ACTION_DISMISS, 3_000_000))
        }

        val notification = builder.build()
        // צלצול חוזר עד שמבטלים (כמו שעון מעורר)
        if (isAlarm) notification.flags = notification.flags or Notification.FLAG_INSISTENT

        nmc.notify(r.id, notification)
    }

    private fun actionPending(ctx: Context, id: Int, action: String, offset: Int): PendingIntent {
        val intent = Intent(ctx, ReminderReceiver::class.java)
            .setAction(action)
            .putExtra(AlarmScheduler.EXTRA_ID, id)
        return PendingIntent.getBroadcast(
            ctx, id + offset, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
