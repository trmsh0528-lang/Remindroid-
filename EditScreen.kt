package com.example.myreminders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

private fun defaultTime(): Long = Calendar.getInstance().apply {
    add(Calendar.HOUR_OF_DAY, 1)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

@Composable
fun EditScreen(initial: Reminder?, onSave: (Reminder) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    BackHandler(onBack = onCancel)

    var title by remember { mutableStateOf(initial?.title ?: "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var timeMillis by remember { mutableLongStateOf(initial?.timeMillis ?: defaultTime()) }
    var type by remember { mutableStateOf(initial?.type ?: AlertType.NOTIFICATION) }
    var repeat by remember { mutableStateOf(initial?.repeat ?: Repeat.NONE) }
    var error by remember { mutableStateOf<String?>(null) }

    fun pickDate() {
        val c = Calendar.getInstance().apply { timeInMillis = timeMillis }
        android.app.DatePickerDialog(
            context,
            { _, y, m, d ->
                val n = Calendar.getInstance().apply {
                    timeInMillis = timeMillis
                    set(y, m, d)
                }
                timeMillis = n.timeInMillis
            },
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun pickTime() {
        val c = Calendar.getInstance().apply { timeInMillis = timeMillis }
        android.app.TimePickerDialog(
            context,
            { _, h, min ->
                val n = Calendar.getInstance().apply {
                    timeInMillis = timeMillis
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, min)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                timeMillis = n.timeInMillis
            },
            c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true
        ).show()
    }

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, contentDescription = "סגור") }
            Text(
                if (initial == null) "תזכורת חדשה" else "עריכת תזכורת",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it; error = null },
            label = { Text("כותרת (למשל: אימון, תור למרפאה)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = desc,
            onValueChange = { desc = it },
            label = { Text("תיאור (לא חובה)") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Text("תאריך ושעה", color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { pickDate() }, modifier = Modifier.weight(1f)) {
                Text(formatDate(timeMillis))
            }
            OutlinedButton(onClick = { pickTime() }) { Text(formatTime(timeMillis)) }
        }

        Text("סוג ההתראה", color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = type == AlertType.NOTIFICATION,
                onClick = { type = AlertType.NOTIFICATION },
                label = { Text("🔔 הודעה") }
            )
            FilterChip(
                selected = type == AlertType.ALARM,
                onClick = { type = AlertType.ALARM },
                label = { Text("⏰ שעון מעורר") }
            )
        }

        Text("חזרה", color = MaterialTheme.colorScheme.primary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = repeat == Repeat.NONE, onClick = { repeat = Repeat.NONE }, label = { Text("פעם אחת") })
            FilterChip(selected = repeat == Repeat.DAILY, onClick = { repeat = Repeat.DAILY }, label = { Text("כל יום") })
            FilterChip(selected = repeat == Repeat.WEEKLY, onClick = { repeat = Repeat.WEEKLY }, label = { Text("כל שבוע") })
        }

        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                when {
                    title.isBlank() -> error = "נא להזין כותרת"
                    repeat == Repeat.NONE && timeMillis <= System.currentTimeMillis() ->
                        error = "הזמן שנבחר כבר עבר"
                    else -> onSave(
                        Reminder(
                            id = initial?.id ?: 0,
                            title = title.trim(),
                            description = desc.trim(),
                            timeMillis = timeMillis,
                            type = type,
                            repeat = repeat,
                            enabled = true
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("שמור") }
    }
}
