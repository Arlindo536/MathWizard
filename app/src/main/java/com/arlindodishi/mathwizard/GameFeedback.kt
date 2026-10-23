package com.arlindodishi.mathwizard

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.annotation.RawRes
import com.arlindodishi.mathwizard.data.Setting
import com.arlindodishi.mathwizard.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
 * Sound effects, the music loop and haptics, driven by [SettingsRepository]. Values are kept
 * as StateFlows so the game can check them instantly.
 */
class GameFeedback(context: Context, private val settings: SettingsRepository, private val scope: CoroutineScope) {
    private val appContext = context.applicationContext

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()
    private val pool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attributes).build()
    private val soundIds = Sfx.entries.associateWith { pool.load(appContext, it.res, 1) }
    private var music: MediaPlayer? = null
    private var inForeground = false

    val soundOn: StateFlow<Boolean> = settings.settings.map { it.soundOn }.stateIn(scope, SharingStarted.Eagerly, true)
    val musicOn: StateFlow<Boolean> = settings.settings.map { it.musicOn }.stateIn(scope, SharingStarted.Eagerly, true)
    private val haptics: StateFlow<Boolean> = settings.settings.map { it.hapticsOn }.stateIn(scope, SharingStarted.Eagerly, true)

    val hapticsOn: Boolean get() = haptics.value

    init {
        // Start or stop the loop whenever the music setting changes.
        scope.launch(Dispatchers.Main) {
            musicOn.collect { on -> if (on && inForeground) startMusic() else music?.pause() }
        }
    }

    fun play(sfx: Sfx) {
        if (!soundOn.value) return
        soundIds[sfx]?.let { pool.play(it, SFX_VOLUME, SFX_VOLUME, 1, 0, 1f) }
    }

    fun setSound(on: Boolean) {
        scope.launch { settings.set(Setting.SOUND, on) }
    }

    fun setMusic(on: Boolean) {
        scope.launch { settings.set(Setting.MUSIC, on) }
    }

    fun onForeground() {
        inForeground = true
        if (musicOn.value) startMusic()
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
        const val SFX_VOLUME = 0.8f
        const val MUSIC_VOLUME = 0.35f
    }
}
