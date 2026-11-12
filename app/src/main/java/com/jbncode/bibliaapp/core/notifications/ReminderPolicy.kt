package com.jbncode.bibliaapp.core.notifications

/**
 * Reglas para no cansar: tras 3 avisos seguidos sin que el usuario abra la app,
 * solo se envía uno cada 3 días. En cuanto abre la app, vuelve a lo normal.
 */
object ReminderPolicy {
    private const val DAY = 24L * 60 * 60 * 1000
    const val MAX_IGNORED = 3

    /** Avisos enviados desde la última vez que abrió la app. */
    fun ignoredNow(lastSentAt: Long, lastOpenAt: Long, ignored: Int) =
        if (lastOpenAt > lastSentAt) 0 else ignored

    fun shouldSend(now: Long, lastSentAt: Long, lastOpenAt: Long, ignored: Int): Boolean =
        ignoredNow(lastSentAt, lastOpenAt, ignored) < MAX_IGNORED || now - lastSentAt >= 3 * DAY

    fun afterSend(lastSentAt: Long, lastOpenAt: Long, ignored: Int) = ignoredNow(lastSentAt, lastOpenAt, ignored) + 1
}
