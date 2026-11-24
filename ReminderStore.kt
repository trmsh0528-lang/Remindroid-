package com.example.myreminders

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** שמירה מקומית של התזכורות וההגדרות (SharedPreferences + JSON). */
class ReminderStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("reminders_store", Context.MODE_PRIVATE)

    fun getAll(): List<Reminder> = synchronized(LOCK) { readAll() }

    fun get(id: Int): Reminder? = synchronized(LOCK) { readAll().firstOrNull { it.id == id } }

    fun save(r: Reminder) = synchronized(LOCK) {
        val list = readAll()
        val index = list.indexOfFirst { it.id == r.id }
        if (index >= 0) list[index] = r else list.add(r)
        writeAll(list)
    }

    fun delete(id: Int) = synchronized(LOCK) {
        val list = readAll()
        list.removeAll { it.id == id }
        writeAll(list)
    }

    fun nextId(): Int = synchronized(LOCK) {
        val n = prefs.getInt(KEY_NEXT_ID, 1)
        prefs.edit().putInt(KEY_NEXT_ID, n + 1).commit()
        n
    }

    var accentIndex: Int
        get() = prefs.getInt(KEY_ACCENT, 0).coerceIn(0, AccentOptions.lastIndex)
        set(value) {
            prefs.edit().putInt(KEY_ACCENT, value).apply()
        }

    private fun readAll(): MutableList<Reminder> {
        val arr = JSONArray(prefs.getString(KEY_LIST, "[]"))
        return MutableList(arr.length()) { arr.getJSONObject(it).toReminder() }
    }

    private fun writeAll(list: List<Reminder>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_LIST, arr.toString()).commit()
    }

    private fun Reminder.toJson() = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("description", description)
        .put("time", timeMillis)
        .put("type", type.name)
        .put("repeat", repeat.name)
        .put("enabled", enabled)

    private fun JSONObject.toReminder() = Reminder(
        id = getInt("id"),
        title = getString("title"),
        description = optString("description", ""),
        timeMillis = getLong("time"),
        type = AlertType.valueOf(getString("type")),
        repeat = Repeat.valueOf(getString("repeat")),
        enabled = optBoolean("enabled", true)
    )

    companion object {
        private val LOCK = Any()
        private const val KEY_LIST = "list"
        private const val KEY_NEXT_ID = "next_id"
        private const val KEY_ACCENT = "accent"
    }
}
