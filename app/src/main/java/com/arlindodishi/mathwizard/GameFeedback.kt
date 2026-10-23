package com.arlindodishi.mathwizard

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.annotation.RawRes
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class Sfx(@param:RawRes val res: Int) {
    CLICK(R.raw.sfx_click),
    HIT(R.raw.sfx_hit),
    CRITICAL(R.raw.sfx_critical),
    MISS(R.raw.sfx_miss),
    SHIELD(R.raw.sfx_shield),
    FLOOR(R.raw.sfx_floor),
    BOSS(R.raw.sfx_boss),
    RELIC(R.raw.sfx_relic),
    BUY(R.raw.sfx_buy),
    GAME_OVER(R.raw.sfx_gameover),
}

/**
 * Sound effects, the music loop and the haptics switch, all mutable from the Home screen.
 * Settings live in the same SharedPreferences file as the classic screens until DataStore
 * arrives with the settings screen (step 8).
 */
class GameFeedback(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("Settings", Context.MODE_PRIVATE)

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val pool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attributes).build()
    private val soundIds = Sfx.entries.associateWith { pool.load(appContext, it.res, 1) }
    private var music: MediaPlayer? = null
    private var inForeground = false

    private val _soundOn = MutableStateFlow(prefs.getBoolean(KEY_SOUND, true))
    val soundOn: StateFlow<Boolean> = _soundOn.asStateFlow()

    private val _musicOn = MutableStateFlow(prefs.getBoolean(KEY_MUSIC, true))
    val musicOn: StateFlow<Boolean> = _musicOn.asStateFlow()

    val hapticsOn: Boolean get() = prefs.getBoolean(KEY_HAPTICS, true)

    fun play(sfx: Sfx) {
        if (!_soundOn.value) return
        soundIds[sfx]?.let { pool.play(it, SFX_VOLUME, SFX_VOLUME, 1, 0, 1f) }
    }

    fun setSound(on: Boolean) {
        _soundOn.value = on
        prefs.edit { putBoolean(KEY_SOUND, on) }
    }

    fun setMusic(on: Boolean) {
        _musicOn.value = on
        prefs.edit { putBoolean(KEY_MUSIC, on) }
        if (on && inForeground) startMusic() else music?.pause()
    }

    fun onForeground() {
        inForeground = true
        if (_musicOn.value) startMusic()
    }

    fun onBackground() {
        inForeground = false
        music?.pause()
    }

    private fun startMusic() {
        val player = music ?: MediaPlayer.create(appContext, R.raw.music_spire)?.apply {
            isLooping = true
            setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
        }?.also { music = it } ?: return
        if (!player.isPlaying) player.start()
    }

    private companion object {
        const val KEY_SOUND = "sound_enabled"
        const val KEY_MUSIC = "music_enabled"
        const val KEY_HAPTICS = "haptics_enabled"
        const val SFX_VOLUME = 0.8f
        const val MUSIC_VOLUME = 0.35f
    }
}
