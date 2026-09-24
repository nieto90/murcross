package com.murcross.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer

/** Looping BGM — Sol EVENT_MAP. Shares murcross_mute with [MurcrossSfx]. */
enum class Bgm(val resName: String, val volume: Float) {
    BgmMenu("bgm_menu", 0.28f),
    BgmGameplayCalm("bgm_gameplay_calm", 0.22f), // ~0.8 × menu
    BgmVictory("bgm_victory", 0.28f),
}

class MurcrossBgm(context: Context) {
    private val app = context.applicationContext
    private val am = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val prefs = app.getSharedPreferences("murcross", Context.MODE_PRIVATE)
    private var player: MediaPlayer? = null
    private var current: Bgm? = null

    var muted: Boolean
        get() = prefs.getBoolean(KEY_MUTE, false)
        set(value) {
            prefs.edit().putBoolean(KEY_MUTE, value).apply()
            if (value) pause() else resume()
        }

    fun start(track: Bgm) {
        if (current == track && player != null) {
            applyVolume()
            if (!muted && !isSilent() && player?.isPlaying != true) {
                runCatching { player?.start() }
            }
            return
        }
        stop()
        current = track
        val resId = app.resources.getIdentifier(track.resName, "raw", app.packageName)
        if (resId == 0) return
        runCatching {
            player = MediaPlayer.create(app, resId)?.apply {
                isLooping = true
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                setVolume(track.volume, track.volume)
                if (!muted && !isSilent()) start()
            }
        }
    }

    fun applyVolume() {
        val track = current ?: return
        val v = if (muted || isSilent()) 0f else track.volume
        runCatching { player?.setVolume(v, v) }
        if (muted || isSilent()) {
            runCatching { if (player?.isPlaying == true) player?.pause() }
        } else {
            runCatching { if (player?.isPlaying != true) player?.start() }
        }
    }

    fun pause() {
        runCatching { if (player?.isPlaying == true) player?.pause() }
    }

    fun resume() {
        if (muted || isSilent()) return
        applyVolume()
        runCatching { if (player?.isPlaying != true) player?.start() }
    }

    fun stop() {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
        current = null
    }

    fun release() = stop()

    private fun isSilent(): Boolean {
        val mode = am.ringerMode
        return mode == AudioManager.RINGER_MODE_SILENT || mode == AudioManager.RINGER_MODE_VIBRATE
    }

    companion object {
        private const val KEY_MUTE = "murcross_mute"
    }
}
