package com.murcross.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build

/**
 * Looping BGM — Sol BGM_INTEGRATION.md.
 * Shares murcross_mute with [MurcrossSfx].
 * Gate: in-app mute OR ringer SILENT/VIBRATE → no play (same policy as SFX).
 * Stream: USAGE_GAME + CONTENT_TYPE_MUSIC. Default vol_bgm = 0.32.
 */
enum class Bgm(val resName: String, val volume: Float) {
    BgmMenu("bgm_menu", 0.32f),
    BgmGameplayCalm("bgm_gameplay_calm", 0.27f), // ~0.85 × vol_bgm
    BgmVictory("bgm_victory", 0.32f),
}

class MurcrossBgm(context: Context) {
    private val app = context.applicationContext
    private val am = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val prefs = app.getSharedPreferences("murcross", Context.MODE_PRIVATE)
    private var player: MediaPlayer? = null
    private var current: Bgm? = null
    private var focusRequest: AudioFocusRequest? = null

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
                requestFocus()
                runCatching { player?.start() }
            }
            return
        }
        stopPlayerOnly()
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
                if (!muted && !isSilent()) {
                    requestFocus()
                    start()
                }
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
            requestFocus()
            runCatching { if (player?.isPlaying != true) player?.start() }
        }
    }

    fun pause() {
        runCatching { if (player?.isPlaying == true) player?.pause() }
    }

    fun resume() {
        if (muted || isSilent()) return
        applyVolume()
        requestFocus()
        runCatching { if (player?.isPlaying != true) player?.start() }
    }

    fun stop() {
        stopPlayerOnly()
        abandonFocus()
        current = null
    }

    fun release() = stop()

    private fun stopPlayerOnly() {
        runCatching {
            player?.stop()
            player?.release()
        }
        player = null
    }

    private fun requestFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attrs)
                .setOnAudioFocusChangeListener { }
                .build()
            focusRequest = req
            runCatching { am.requestAudioFocus(req) }
        } else {
            @Suppress("DEPRECATION")
            runCatching {
                am.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK,
                )
            }
        }
    }

    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { runCatching { am.abandonAudioFocusRequest(it) } }
            focusRequest = null
        }
    }

    /** Same gate as SFX: SILENT/VIBRATE → skip BGM (Sol BGM_INTEGRATION §5). */
    private fun isSilent(): Boolean {
        val mode = am.ringerMode
        return mode == AudioManager.RINGER_MODE_SILENT || mode == AudioManager.RINGER_MODE_VIBRATE
    }

    companion object {
        private const val KEY_MUTE = "murcross_mute"
    }
}
