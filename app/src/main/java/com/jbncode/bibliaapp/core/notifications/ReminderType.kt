package com.jbncode.bibliaapp.core.notifications

/** Cada tipo tiene su propia alarma, notificación y canal (el usuario puede silenciarlos por separado). */
enum class ReminderType(val requestCode: Int, val notificationId: Int, val channelId: String) {
    VERSE(101, 2001, "daily_verse_channel"),          // mismo canal que ya existía
    EVENING(102, 2002, "reading_reminder_channel");

    companion object { fun from(name: String?) = entries.firstOrNull { it.name == name } }
}

data class ReminderContent(val title: String, val text: String, val destination: String)
