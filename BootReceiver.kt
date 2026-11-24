package com.example.myreminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** אחרי הפעלה מחדש של הטלפון כל ההתראות נמחקות – כאן קובעים אותן מחדש. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmScheduler.rescheduleAll(context)
    }
}
