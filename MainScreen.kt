package com.example.myreminders

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val HE = Locale.forLanguageTag("he-IL")

fun formatDate(ms: Long): String = SimpleDateFormat("EEEE, d MMMM yyyy", HE).format(Date(ms))
fun formatTime(ms: Long): String = SimpleDateFormat("HH:mm", HE).format(Date(ms))

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val store = remember { ReminderStore(context) }

    var accentIndex by remember { mutableIntStateOf(store.accentIndex) }
    var reminders by remember { mutableStateOf(store.getAll()) }
    var notifOk by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var showEditor by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Reminder?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<Reminder?>(null) }

    // בקשת הרשאת התראות (אנדרואיד 13 ומעלה)
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notifOk = it }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !notifOk) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        reminders = store.getAll()
        notifOk = NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun refresh() { reminders = store.getAll() }

    fun saveReminder(input: Reminder) {
        var r = input
        if (r.id == 0) r = r.copy(id = store.nextId())
        if (r.repeat != Repeat.NONE) r = r.copy(timeMillis = AlarmScheduler.nextTime(r, System.currentTimeMillis()))
        AlarmScheduler.cancel(context, r.id)
        store.save(r)
        AlarmScheduler.schedule(context, r)
        refresh()
        showEditor = false
        editTarget = null
    }

    fun toggle(r: Reminder, on: Boolean) {
        if (!on) {
            store.save(r.copy(enabled = false))
            AlarmScheduler.cancel(context, r.id)
        } else {
            var n = r.copy(enabled = true)
            if (n.timeMillis <= System.currentTimeMillis()) {
                if (n.repeat == Repeat.NONE) {
                    Toast.makeText(context, "הזמן כבר עבר – ערוך את התזכורת", Toast.LENGTH_SHORT).show()
                    return
                }
                n = n.copy(timeMillis = AlarmScheduler.nextTime(n, System.currentTimeMillis()))
            }
            store.save(n)
            AlarmScheduler.schedule(context, n)
        }
        refresh()
    }

    AppTheme(accent = AccentOptions[accentIndex].color) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                if (showEditor) {
                    EditScreen(
                        initial = editTarget,
                        onSave = { saveReminder(it) },
                        onCancel = { showEditor = false; editTarget = null }
                    )
                } else {
                    ListScreen(
                        reminders = reminders,
                        notifOk = notifOk,
                        onAdd = { editTarget = null; showEditor = true },
                        onEdit = { editTarget = it; showEditor = true },
                        onToggle = { r, on -> toggle(r, on) },
                        onDelete = { toDelete = it },
                        onOpenSettings = { showSettings = true }
                    )
                }

                toDelete?.let { r ->
                    AlertDialog(
                        onDismissRequest = { toDelete = null },
                        title = { Text("למחוק את התזכורת?") },
                        text = { Text(r.title) },
                        confirmButton = {
                            TextButton(onClick = {
                                AlarmScheduler.cancel(context, r.id)
                                store.delete(r.id)
                                refresh()
                                toDelete = null
                            }) { Text("מחק") }
                        },
                        dismissButton = { TextButton(onClick = { toDelete = null }) { Text("ביטול") } }
                    )
                }

                if (showSettings) {
                    SettingsDialog(
                        selected = accentIndex,
                        onSelect = { accentIndex = it; store.accentIndex = it },
                        onDismiss = { showSettings = false }
                    )
                }
            }
        }
    }
}

@Composable
fun ListScreen(
    reminders: List<Reminder>,
    notifOk: Boolean,
    onAdd: () -> Unit,
    onEdit: (Reminder) -> Unit,
    onToggle: (Reminder, Boolean) -> Unit,
    onDelete: (Reminder) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val upcoming = reminders.filter { it.enabled }.sortedBy { it.timeMillis }
    val inactive = reminders.filter { !it.enabled }.sortedByDescending { it.timeMillis }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) { Icon(Icons.Default.Add, contentDescription = "תזכורת חדשה") }
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "התזכורות שלי",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "הגדרות")
                    }
                }
            }

            if (!notifOk) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("ההתראות חסומות. בלי הרשאה לא תקבל תזכורות.")
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                )
                            }) { Text("אפשר התראות") }
                        }
                    }
                }
            }

            if (reminders.isEmpty()) {
                item {
                    Text(
                        "אין עדיין תזכורות.\nלחץ על + כדי להוסיף.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            if (upcoming.isNotEmpty()) {
                item { SectionTitle("קרובות") }
                items(upcoming, key = { it.id }) { r ->
                    ReminderCard(r, { onEdit(r) }, { onToggle(r, it) }, { onDelete(r) })
                }
            }
            if (inactive.isNotEmpty()) {
                item { SectionTitle("כבויות / שעברו") }
                items(inactive, key = { it.id }) { r ->
                    ReminderCard(r, { onEdit(r) }, { onToggle(r, it) }, { onDelete(r) })
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun ReminderCard(
    r: Reminder,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).alpha(if (r.enabled) 1f else 0.5f)) {
                Text(r.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (r.description.isNotBlank()) {
                    Text(r.description, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "${formatDate(r.timeMillis)} · ${formatTime(r.timeMillis)}",
                    color = MaterialTheme.colorScheme.primary
                )
                val badges = buildString {
                    append(if (r.type == AlertType.ALARM) "⏰ שעון מעורר" else "🔔 התראה")
                    when (r.repeat) {
                        Repeat.DAILY -> append("  ·  🔁 כל יום")
                        Repeat.WEEKLY -> append("  ·  🔁 כל שבוע")
                        Repeat.NONE -> {}
                    }
                }
                Text(badges, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(checked = r.enabled, onCheckedChange = onToggle)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "מחיקה", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(selected: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("סגור") } },
        title = { Text("הגדרות") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("צבע הדגשה: ${AccentOptions[selected].name}")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccentOptions.forEachIndexed { i, option ->
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(option.color)
                                .border(
                                    BorderStroke(if (i == selected) 3.dp else 0.dp, Color.White),
                                    CircleShape
                                )
                                .clickable { onSelect(i) }
                        )
                    }
                }

                OutlinedButton(onClick = {
                    NotificationHelper.show(
                        context,
                        Reminder(999_999, "בדיקת התראה", "ככה ייראו התזכורות שלך", System.currentTimeMillis(), AlertType.NOTIFICATION, Repeat.NONE)
                    )
                }) { Text("שלח התראת בדיקה") }

                if (Build.VERSION.SDK_INT >= 34) {
                    val nm = context.getSystemService(NotificationManager::class.java)
                    if (!nm.canUseFullScreenIntent()) {
                        Text(
                            "כדי ששעון מעורר יקפוץ על כל המסך, אפשר 'התראות במסך מלא':",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                    Uri.parse("package:${context.packageName}")
                                )
                            )
                        }) { Text("פתח הגדרה") }
                    }
                }
            }
        }
    )
}
