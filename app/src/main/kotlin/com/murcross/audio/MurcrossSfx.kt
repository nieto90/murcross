package com.murcross.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.preference.PreferenceManager
import com.murcross.R

/**
 * SoundPool wiring — Sol EVENT_MAP.
 * Missing raw → no-op. Mute in-app + silent/vibrate ringer → skip.
 */
enum class Sfx(val resName: String, val volume: Float) {
    SfxTapUi("tap_ui", 0.45f),
    SfxPlaceOk("place_ok", 0.65f),
    SfxIllegal("illegal", 0.50f),
    SfxMarkX("mark_x", 0.65f),
    SfxUndo("undo", 0.65f),
    SfxRotateO("rotate_o", 0.65f),
    SfxReveal("reveal", 0.75f),
    SfxCoachDismiss("coach_dismiss", 0.45f),
    SfxTramoOk("tramo_ok", 0.35f),
    SfxResolverReady("resolver_ready", 0.35f),
}

class MurcrossSfx(context: Context) {
    private val app = context.applicationContext
    private val am = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val prefs = PreferenceManager.getDefaultSharedPreferences(app)

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val soundIds = mutableMapOf<Sfx, Int>()

    var muted: Boolean
        get() = prefs.getBoolean(KEY_MUTE, false)
        set(value) = prefs.edit().putBoolean(KEY_MUTE, value).apply()

    init {
        for (sfx in Sfx.entries) {
            val id = app.resources.getIdentifier(sfx.resName, "raw", app.packageName)
            if (id != 0) {
                runCatching { soundIds[sfx] = pool.load(app, id, 1) }
            }
        }
    }

    fun play(sfx: Sfx) {
        if (muted) return
        val ringer = am.ringerMode
        if (ringer == AudioManager.RINGER_MODE_SILENT || ringer == AudioManager.RINGER_MODE_VIBRATE) return
        val sid = soundIds[sfx] ?: return
        runCatching { pool.play(sid, sfx.volume, sfx.volume, 1, 0, 1f) }
    }

    fun release() {
        runCatching { pool.release() }
    }

    companion object {
        private const val KEY_MUTE = "murcross_mute"
    }
}
