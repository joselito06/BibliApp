package com.jbncode.bibliaapp.ui.feature.notifications

import androidx.lifecycle.ViewModel
import com.jbncode.bibliaapp.core.notifications.ReminderScheduler
import com.jbncode.bibliaapp.core.notifications.ReminderStore
import com.jbncode.bibliaapp.core.notifications.ReminderType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val store: ReminderStore,
    private val scheduler: ReminderScheduler
) : ViewModel() {
    val eveningEnabled: StateFlow<Boolean> = store.eveningEnabled
    val eveningTime: StateFlow<LocalTime> = store.eveningTime

    fun setEveningEnabled(on: Boolean) { store.setEveningEnabled(on); scheduler.rescheduleOne(ReminderType.EVENING) }
    fun setEveningTime(t: LocalTime) { store.setEveningTime(t); scheduler.rescheduleOne(ReminderType.EVENING) }
}
