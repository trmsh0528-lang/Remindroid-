package com.example.myreminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** מקבל את "הצלצול" מה-AlarmManager ואת הכפתורים (נודניק / בטל) מההתראה. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ID, -1)
        if (id < 0) return
        val store = ReminderStore(context)

        when (intent.action) {
            AlarmScheduler.ACTION_FIRE -> {
                val r = store.get(id) ?: return
                NotificationHelper.show(context, r)

                val isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_SNOOZE, false)
                if (!isSnooze) {
                    if (r.repeat == Repeat.NONE) {
                        store.save(r.copy(enabled = false))
                    } else {
                        val next = r.copy(timeMillis = AlarmScheduler.nextTime(r, System.currentTimeMillis()))
                        store.save(next)
                        AlarmScheduler.schedule(context, next)
                    }
                }
            }
            AlarmScheduler.ACTION_SNOOZE -> AlarmScheduler.snooze(context, id)
            AlarmScheduler.ACTION_DISMISS -> AlarmScheduler.dismiss(context, id)
        }
    }
}
