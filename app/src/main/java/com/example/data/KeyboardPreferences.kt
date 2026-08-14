package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class KeyboardSettings(
    val selectedVideoPath: String? = null,
    val selectedVideoName: String? = null,
    val presetVideoId: String = "cyber_neon",
    val usePreset: Boolean = true,
    val keyOpacity: Float = 0.35f,
    val glowIntensity: Float = 1.0f,
    val hapticEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val animationStyle: String = "NEON_RIPPLE"
)

class KeyboardPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settingsFlow = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<KeyboardSettings> = _settingsFlow.asStateFlow()

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            _settingsFlow.value = loadSettings()
        }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    fun getSettings(): KeyboardSettings = loadSettings()

    fun refresh() {
        _settingsFlow.value = loadSettings()
    }

    private fun loadSettings(): KeyboardSettings {
        return KeyboardSettings(
            selectedVideoPath = prefs.getString(KEY_VIDEO_PATH, null),
            selectedVideoName = prefs.getString(KEY_VIDEO_NAME, null),
            presetVideoId = prefs.getString(KEY_PRESET_ID, "cyber_neon") ?: "cyber_neon",
            usePreset = prefs.getBoolean(KEY_USE_PRESET, true),
            keyOpacity = prefs.getFloat(KEY_OPACITY, 0.35f),
            glowIntensity = prefs.getFloat(KEY_GLOW_INTENSITY, 1.0f),
            hapticEnabled = prefs.getBoolean(KEY_HAPTIC, true),
            soundEnabled = prefs.getBoolean(KEY_SOUND, false),
            animationStyle = prefs.getString(KEY_ANIM_STYLE, "NEON_RIPPLE") ?: "NEON_RIPPLE"
        )
    }

    fun setCustomVideo(path: String, name: String) {
        prefs.edit()
            .putString(KEY_VIDEO_PATH, path)
            .putString(KEY_VIDEO_NAME, name)
            .putBoolean(KEY_USE_PRESET, false)
            .apply()
        _settingsFlow.value = loadSettings()
    }

    fun setPresetVideo(presetId: String) {
        prefs.edit()
            .putString(KEY_PRESET_ID, presetId)
            .putBoolean(KEY_USE_PRESET, true)
            .apply()
        _settingsFlow.value = loadSettings()
    }

    fun setKeyOpacity(opacity: Float) {
        prefs.edit().putFloat(KEY_OPACITY, opacity).apply()
        _settingsFlow.value = loadSettings()
    }

    fun setGlowIntensity(intensity: Float) {
        prefs.edit().putFloat(KEY_GLOW_INTENSITY, intensity).apply()
        _settingsFlow.value = loadSettings()
    }

    fun setHapticEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_HAPTIC, enabled).apply()
        _settingsFlow.value = loadSettings()
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND, enabled).apply()
        _settingsFlow.value = loadSettings()
    }

    fun setAnimationStyle(style: String) {
        prefs.edit().putString(KEY_ANIM_STYLE, style).apply()
        _settingsFlow.value = loadSettings()
    }

    fun clearCustomVideo() {
        prefs.edit()
            .remove(KEY_VIDEO_PATH)
            .remove(KEY_VIDEO_NAME)
            .putBoolean(KEY_USE_PRESET, true)
            .apply()
        _settingsFlow.value = loadSettings()
    }

    companion object {
        private const val PREFS_NAME = "motion_keys_prefs"
        private const val KEY_VIDEO_PATH = "selected_video_path"
        private const val KEY_VIDEO_NAME = "selected_video_name"
        private const val KEY_PRESET_ID = "preset_video_id"
        private const val KEY_USE_PRESET = "use_preset"
        private const val KEY_OPACITY = "key_opacity"
        private const val KEY_GLOW_INTENSITY = "glow_intensity"
        private const val KEY_HAPTIC = "haptic_enabled"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_ANIM_STYLE = "anim_style"

        @Volatile
        private var INSTANCE: KeyboardPreferences? = null

        fun getInstance(context: Context): KeyboardPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: KeyboardPreferences(context).also { INSTANCE = it }
            }
        }
    }
}
