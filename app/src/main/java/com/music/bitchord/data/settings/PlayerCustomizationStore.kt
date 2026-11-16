package com.music.bitchord.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * Thread-safe persistent reactive store for BeDivine Now Playing player customization.
 * Backed by SharedPreferences("bedivine_player_customization").
 */
object PlayerCustomizationStore {
    private const val PREFS_NAME = "bedivine_player_customization"
    private const val KEY_STATE_JSON = "player_customization_state_json"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val lock = Any()
    private var prefs: SharedPreferences? = null

    private val _state = MutableStateFlow(PlayerCustomizationState())
    val state: StateFlow<PlayerCustomizationState> = _state.asStateFlow()

    fun init(context: Context) {
        synchronized(lock) {
            if (prefs != null) return
            val sharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            init(sharedPreferences)
        }
    }

    fun init(sharedPreferences: SharedPreferences) {
        synchronized(lock) {
            this.prefs = sharedPreferences
            loadFromPrefsLocked()
        }
    }

    fun setControlsOffset(offsetDp: Int) {
        synchronized(lock) {
            val clamped = offsetDp.coerceIn(-40, 60)
            updateStateLocked { it.copy(controlsOffsetDp = clamped, preset = PlayerPreset.CUSTOM) }
        }
    }

    fun toggleShuffle() {
        synchronized(lock) {
            updateStateLocked { it.copy(showShuffle = !it.showShuffle, preset = PlayerPreset.CUSTOM) }
        }
    }

    fun toggleRepeat() {
        synchronized(lock) {
            updateStateLocked { it.copy(showRepeat = !it.showRepeat, preset = PlayerPreset.CUSTOM) }
        }
    }

    fun toggleProgressBar() {
        synchronized(lock) {
            updateStateLocked { it.copy(showProgressBar = !it.showProgressBar, preset = PlayerPreset.CUSTOM) }
        }
    }

    fun toggleTimestamps() {
        synchronized(lock) {
            updateStateLocked { it.copy(showTimestamps = !it.showTimestamps, preset = PlayerPreset.CUSTOM) }
        }
    }

    fun toggleFavorite() {
        synchronized(lock) {
            updateStateLocked { it.copy(showFavorite = !it.showFavorite, preset = PlayerPreset.CUSTOM) }
        }
    }

    fun toggleLyricsButton() {
        synchronized(lock) {
            updateStateLocked { it.copy(showLyricsButton = !it.showLyricsButton, preset = PlayerPreset.CUSTOM) }
        }
    }

    fun toggleQueueButton() {
        synchronized(lock) {
            updateStateLocked { it.copy(showQueueButton = !it.showQueueButton, preset = PlayerPreset.CUSTOM) }
        }
    }

    fun toggleVolumeBar() {
        synchronized(lock) {
            updateStateLocked { it.copy(showVolumeBar = !it.showVolumeBar, preset = PlayerPreset.CUSTOM) }
        }
    }

    fun setCoverSize(size: PlayerCoverSize) {
        synchronized(lock) {
            updateStateLocked { it.copy(coverSize = size, preset = PlayerPreset.CUSTOM) }
        }
    }

    fun setCoverAnimation(animation: PlayerCoverAnimation) {
        synchronized(lock) {
            updateStateLocked { it.copy(coverAnimation = animation, preset = PlayerPreset.CUSTOM) }
        }
    }

    fun setBlurIntensity(intensity: Float) {
        synchronized(lock) {
            val clamped = intensity.coerceIn(0f, 1f)
            val backdrop = if (clamped == 0f) PlayerBackdropStyle.OLED_BLACK else it.backdropStyle
            updateStateLocked { it.copy(blurIntensity = clamped, backdropStyle = backdrop, preset = PlayerPreset.CUSTOM) }
        }
    }

    fun setBackdropStyle(style: PlayerBackdropStyle) {
        synchronized(lock) {
            val blur = if (style == PlayerBackdropStyle.OLED_BLACK) 0f else it.blurIntensity.coerceAtLeast(0.3f)
            updateStateLocked { it.copy(backdropStyle = style, blurIntensity = blur, preset = PlayerPreset.CUSTOM) }
        }
    }

    fun applyPreset(preset: PlayerPreset) {
        synchronized(lock) {
            val newState = PlayerCustomizationState.forPreset(preset)
            saveStateLocked(newState)
        }
    }

    fun resetToDefaults() {
        synchronized(lock) {
            saveStateLocked(PlayerCustomizationState())
        }
    }

    private inline fun updateStateLocked(transform: (PlayerCustomizationState) -> PlayerCustomizationState) {
        val next = transform(_state.value)
        saveStateLocked(next)
    }

    private fun loadFromPrefsLocked() {
        val raw = prefs?.getString(KEY_STATE_JSON, null)
        if (raw.isNullOrBlank()) {
            _state.value = PlayerCustomizationState()
            return
        }
        val parsed = runCatching { json.decodeFromString<PlayerCustomizationState>(raw) }.getOrNull()
        _state.value = parsed ?: PlayerCustomizationState()
    }

    private fun saveStateLocked(newState: PlayerCustomizationState) {
        _state.value = newState
        val encoded = runCatching { json.encodeToString(newState) }.getOrNull() ?: return
        prefs?.edit()?.putString(KEY_STATE_JSON, encoded)?.apply()
    }
}
