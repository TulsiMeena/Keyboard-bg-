package com.example.animation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class KeyType {
    NORMAL,
    SPACE,
    BACKSPACE,
    ENTER,
    SHIFT,
    EMOJI,
    ACTION
}

enum class AnimationStyle(val label: String) {
    NEON_RIPPLE("Neon Ripple"),
    CYBER_GLOW("Cyber Glow"),
    COSMIC_WAVE("Cosmic Wave"),
    MINIMAL_PULSE("Minimal Pulse")
}

data class KeyRippleState(
    val keyId: String,
    val keyType: KeyType,
    val triggerTimeMs: Long,
    val touchXPercent: Float = 0.5f,
    val touchYPercent: Float = 0.5f
)

class KeyAnimationController {

    // Map of active key animation states by key identifier
    private val _activeRipples = mutableStateMapOf<String, KeyRippleState>()
    val activeRipples: Map<String, KeyRippleState> get() = _activeRipples

    var isShiftActive by mutableStateOf(false)
    var isCapsLock by mutableStateOf(false)

    /**
     * Triggers visual animation for a key press immediately.
     * Guaranteed to never block or delay text input.
     */
    fun triggerKeyPress(
        keyId: String,
        keyType: KeyType = KeyType.NORMAL,
        touchXPercent: Float = 0.5f,
        touchYPercent: Float = 0.5f
    ) {
        val now = System.currentTimeMillis()
        _activeRipples[keyId] = KeyRippleState(
            keyId = keyId,
            keyType = keyType,
            triggerTimeMs = now,
            touchXPercent = touchXPercent,
            touchYPercent = touchYPercent
        )
    }

    fun clearKey(keyId: String) {
        _activeRipples.remove(keyId)
    }

    fun toggleShift() {
        if (isShiftActive && !isCapsLock) {
            isCapsLock = true
        } else if (isCapsLock) {
            isCapsLock = false
            isShiftActive = false
        } else {
            isShiftActive = true
        }
    }

    fun resetShiftAfterType() {
        if (isShiftActive && !isCapsLock) {
            isShiftActive = false
        }
    }
}
