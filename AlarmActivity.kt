package com.example.myreminders

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** מסך מלא שקופץ כשתזכורת מסוג "שעון מעורר" מצלצלת. */
class AlarmActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ID, -1)
        val store = ReminderStore(this)
        val reminder = store.get(id)
        if (reminder == null) {
            finish()
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContent {
            AppTheme(accent = AccentOptions[store.accentIndex].color) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(formatTime(reminder.timeMillis), fontSize = 56.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(16.dp))
                            Text(reminder.title, fontSize = 28.sp, textAlign = TextAlign.Center)
                            if (reminder.description.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    reminder.description,
                                    fontSize = 18.sp,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(48.dp))
                            Button(
                                onClick = {
                                    AlarmScheduler.dismiss(this@AlarmActivity, id)
                                    finish()
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) { Text("בטל", fontSize = 18.sp) }
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    AlarmScheduler.snooze(this@AlarmActivity, id)
                                    finish()
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) { Text("נודניק ${AlarmScheduler.SNOOZE_MINUTES} דקות", fontSize = 18.sp) }
                        }
                    }
                }
            }
        }
    }
}
