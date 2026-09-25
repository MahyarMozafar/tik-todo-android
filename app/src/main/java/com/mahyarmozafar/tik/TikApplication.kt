package com.mahyarmozafar.tik

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.mahyarmozafar.tik.app.AppIcons
import com.mahyarmozafar.tik.app.AppModel
import com.mahyarmozafar.tik.app.Sounds
import com.mahyarmozafar.tik.data.PhotoStore
import com.mahyarmozafar.tik.data.SettingsRepository
import com.mahyarmozafar.tik.data.TikDatabase
import com.mahyarmozafar.tik.data.TikStore
import com.mahyarmozafar.tik.reminders.Reminders
import com.mahyarmozafar.tik.reminders.TodayCount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Creates the few long-lived objects the app, the widget and the reminders share. */
class TikApplication : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val photos by lazy { PhotoStore(this) }
    val store by lazy { TikStore(TikDatabase.open(this), photos) }
    val settings by lazy { SettingsRepository(this) }
    val reminders by lazy { Reminders(this) }
    val sounds by lazy { Sounds(this) }
    val model by lazy { AppModel(this, store, settings, reminders, TodayCount(this, reminders), scope) }

    override fun onCreate() {
        super.onCreate()
        scope.launch { model.start() }

        // Switching the app icon can close the app on some phones, so it happens on the way out.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                val chosen = model.settings.value?.appIcon ?: return
                AppIcons.apply(this@TikApplication, chosen)
            }
        })
    }

    /** Called when a reminder's alarm goes off. */
    suspend fun showReminder(taskId: String) {
        val task = store.task(taskId) ?: return
        if (task.isDone) return
        val list = task.listId?.let { id -> store.allLists().firstOrNull { it.id == id } }
        reminders.show(task, list, settings.current())
    }
}
