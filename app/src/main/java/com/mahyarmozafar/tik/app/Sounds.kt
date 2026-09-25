package com.mahyarmozafar.tik.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.mahyarmozafar.tik.R

/**
 * Plays the two short sounds made by scripts/make-sounds.py. Like the iOS app, it stays quiet
 * when the phone is on silent or vibrate.
 */
class Sounds(context: Context) {
    enum class Sound { Tick, Celebrate }

    private val audio = context.getSystemService(AudioManager::class.java)
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val tick = pool.load(context, R.raw.tick, 1)
    private val celebrate = pool.load(context, R.raw.celebrate, 1)

    fun play(sound: Sound) {
        if (audio?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        when (sound) {
            Sound.Tick -> pool.play(tick, 0.6f, 0.6f, 1, 0, 1f)
            Sound.Celebrate -> pool.play(celebrate, 0.75f, 0.75f, 1, 0, 1f)
        }
    }
}
