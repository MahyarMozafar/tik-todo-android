package com.mahyarmozafar.tik.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.mahyarmozafar.tik.app.Sounds
import com.mahyarmozafar.tik.model.TikSettings
import com.mahyarmozafar.tik.ui.theme.TikTheme

val LocalSounds = staticCompositionLocalOf<Sounds?> { null }

/** Haptics and sounds. Each one can be turned off in Settings. */
class Feedback(
    private val haptics: HapticFeedback,
    private val sounds: Sounds?,
    private val settings: TikSettings,
) {
    /** A task was ticked or unticked. */
    fun tick(done: Boolean) {
        if (settings.haptics) {
            haptics.performHapticFeedback(if (done) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
        }
        if (done && settings.sounds) sounds?.play(Sounds.Sound.Tick)
    }

    /** A task was added. */
    fun added() {
        if (settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
    }

    /** A small tap, for pickers and toggles. */
    fun selection() {
        if (settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
    }

    fun longPress() {
        if (settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    /** The last task of the day was ticked. */
    fun celebrate() {
        if (settings.haptics) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        if (settings.sounds) sounds?.play(Sounds.Sound.Celebrate)
    }
}

@Composable
fun rememberFeedback(): Feedback {
    val haptics = LocalHapticFeedback.current
    val sounds = LocalSounds.current
    val settings = TikTheme.settings
    return remember(haptics, sounds, settings) { Feedback(haptics, sounds, settings) }
}
