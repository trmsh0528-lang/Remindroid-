package com.example.myreminders

enum class AlertType { NOTIFICATION, ALARM }

enum class Repeat { NONE, DAILY, WEEKLY }

data class Reminder(
    val id: Int,
    val title: String,
    val description: String,
    val timeMillis: Long,
    val type: AlertType,
    val repeat: Repeat,
    val enabled: Boolean = true
)
