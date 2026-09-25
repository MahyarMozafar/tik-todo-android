package com.mahyarmozafar.tik.app

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.mahyarmozafar.tik.model.AppIconChoice

/**
 * Each app icon is a launcher entry in the manifest; only the chosen one is turned on.
 *
 * Switching can close the app on some phones, so it is done when Tik goes to the background,
 * not while someone is looking at it.
 */
object AppIcons {
    fun current(context: Context): AppIconChoice {
        val manager = context.packageManager
        return AppIconChoice.entries.firstOrNull { choice ->
            when (manager.getComponentEnabledSetting(ComponentName(context, choice.aliasName))) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> choice == AppIconChoice.Blue
                else -> false
            }
        } ?: AppIconChoice.Blue
    }

    fun apply(context: Context, choice: AppIconChoice) {
        if (current(context) == choice) return
        val manager = context.packageManager
        // Turn the new one on first, so there is never a moment with no icon at all.
        manager.setComponentEnabledSetting(
            ComponentName(context, choice.aliasName),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
        for (other in AppIconChoice.entries) {
            if (other == choice) continue
            manager.setComponentEnabledSetting(
                ComponentName(context, other.aliasName),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
