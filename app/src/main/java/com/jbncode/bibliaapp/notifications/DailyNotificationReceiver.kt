package com.jbncode.bibliaapp.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jbncode.bibliaapp.core.notifications.NotificationHelper
import com.jbncode.bibliaapp.core.notifications.ReminderPlanner
import com.jbncode.bibliaapp.core.notifications.ReminderScheduler
import com.jbncode.bibliaapp.core.notifications.ReminderType
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReminderEntryPoint {
    fun scheduler(): ReminderScheduler
    fun planner(): ReminderPlanner
    fun helper(): NotificationHelper
}

class DailyNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SHOW = "com.jbncode.bibliaapp.ACTION_SHOW_REMINDER"
        const val EXTRA_TYPE = "type"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val ep = EntryPointAccessors.fromApplication(context.applicationContext, ReminderEntryPoint::class.java)
        val pending = goAsync()   // permite consultar la base de datos sin que el sistema mate el receptor
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (intent.action == ACTION_SHOW) {
                    val type = ReminderType.from(intent.getStringExtra(EXTRA_TYPE)) ?: return@launch
                    ep.scheduler().rescheduleOne(type)                 // primero el de mañana: la cadena no se rompe
                    ep.planner().build(type)?.let { ep.helper().show(type, it) }
                } else {
                    ep.scheduler().rescheduleAll()                     // reinicio, actualización, cambio de hora/zona
                }
            } finally {
                pending.finish()
            }
        }
    }
}
