package com.example.ui.keyboard

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.KeyboardReturn
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animation.KeyAnimationController
import com.example.animation.KeyRippleState
import com.example.animation.KeyType
import com.example.data.KeyboardSettings
import com.example.theme.KeyboardDimensions
import com.example.theme.KeyboardThemeManager
import kotlinx.coroutines.delay

enum class KeyboardPage {
    ALPHA,
    NUMBERS_SYMBOLS,
    MORE_SYMBOLS,
    EMOJIS
}

@Composable
fun KeyboardView(
    settings: KeyboardSettings,
    animationController: KeyAnimationController,
    onTextCommit: (String) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isPaused: Boolean = false
) {
    val view = LocalView.current
    var currentPage by remember { mutableStateOf(KeyboardPage.ALPHA) }

    fun playFeedback() {
        if (settings.hapticEnabled) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
        if (settings.soundEnabled) {
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .testTag("motion_keyboard_root")
    ) {
        // Layer 1: Animated Video Background (MP4 Video or Procedural Preset Loop)
        VideoBackgroundView(
            settings = settings,
            isPaused = isPaused
        )

        // Layer 2: Transparent / Glassmorphic Keyboard UI
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = KeyboardDimensions.keyboardHorizontalPadding,
                    vertical = KeyboardDimensions.keyboardVerticalPadding
                ),
            verticalArrangement = Arrangement.spacedBy(KeyboardDimensions.rowSpacing)
        ) {
            // Quick Action Header Bar (translucent pill)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(Color(0x330F172A), RoundedCornerShape(15.dp))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "MotionKeys • ${if (settings.usePreset) "Preset" else "Video Active"}",
                    style = KeyboardThemeManager.SecondaryKeyTextStyle.copy(fontSize = 12.sp),
                    color = Color(0xCCF8FAFC)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (onOpenSettings != null) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x44334155))
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        playFeedback()
                                        onOpenSettings()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color(0xFFCBD5E1),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            when (currentPage) {
                KeyboardPage.ALPHA -> {
                    AlphaKeyboardLayout(
                        settings = settings,
                        animationController = animationController,
                        onKeyInput = { char ->
                            playFeedback()
                            val textToSend = if (animationController.isShiftActive || animationController.isCapsLock) {
                                char.uppercase()
                            } else {
                                char.lowercase()
                            }
                            onTextCommit(textToSend)
                            animationController.resetShiftAfterType()
                        },
                        onSpace = {
                            playFeedback()
                            onTextCommit(" ")
                        },
                        onDelete = {
                            playFeedback()
                            onDelete()
                        },
                        onEnter = {
                            playFeedback()
                            onEnter()
                        },
                        onShiftToggle = {
                            playFeedback()
                            animationController.toggleShift()
                        },
                        onSwitchToSymbols = {
                            playFeedback()
                            currentPage = KeyboardPage.NUMBERS_SYMBOLS
                        },
                        onSwitchToEmoji = {
                            playFeedback()
                            currentPage = KeyboardPage.EMOJIS
                        }
                    )
                }

                KeyboardPage.NUMBERS_SYMBOLS -> {
                    NumbersSymbolsKeyboardLayout(
                        settings = settings,
                        animationController = animationController,
                        onKeyInput = { char ->
                            playFeedback()
                            onTextCommit(char)
                        },
                        onSpace = {
                            playFeedback()
                            onTextCommit(" ")
                        },
                        onDelete = {
                            playFeedback()
                            onDelete()
                        },
                        onEnter = {
                            playFeedback()
                            onEnter()
                        },
                        onSwitchToAlpha = {
                            playFeedback()
                            currentPage = KeyboardPage.ALPHA
                        },
                        onSwitchToMoreSymbols = {
                            playFeedback()
                            currentPage = KeyboardPage.MORE_SYMBOLS
                        },
                        onSwitchToEmoji = {
                            playFeedback()
                            currentPage = KeyboardPage.EMOJIS
                        }
                    )
                }

                KeyboardPage.MORE_SYMBOLS -> {
                    MoreSymbolsKeyboardLayout(
                        settings = settings,
                        animationController = animationController,
                        onKeyInput = { char ->
                            playFeedback()
                            onTextCommit(char)
                        },
                        onSpace = {
                            playFeedback()
                            onTextCommit(" ")
                        },
                        onDelete = {
                            playFeedback()
                            onDelete()
                        },
                        onEnter = {
                            playFeedback()
                            onEnter()
                        },
                        onSwitchToAlpha = {
                            playFeedback()
                            currentPage = KeyboardPage.ALPHA
                        },
                        onSwitchToSymbols1 = {
                            playFeedback()
                            currentPage = KeyboardPage.NUMBERS_SYMBOLS
                        }
                    )
                }

                KeyboardPage.EMOJIS -> {
                    EmojiKeyboardLayout(
                        onEmojiSelected = { emoji ->
                            playFeedback()
                            onTextCommit(emoji)
                        },
                        onSwitchToAlpha = {
                            playFeedback()
                            currentPage = KeyboardPage.ALPHA
                        },
                        onDelete = {
                            playFeedback()
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AlphaKeyboardLayout(
    settings: KeyboardSettings,
    animationController: KeyAnimationController,
    onKeyInput: (String) -> Unit,
    onSpace: () -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
    onShiftToggle: () -> Unit,
    onSwitchToSymbols: () -> Unit,
    onSwitchToEmoji: () -> Unit
) {
    val isUpper = animationController.isShiftActive || animationController.isCapsLock

    val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val row3 = listOf("z", "x", "c", "v", "b", "n", "m")

    Column(
        verticalArrangement = Arrangement.spacedBy(KeyboardDimensions.rowSpacing),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KeyboardDimensions.keyHorizontalSpacing)
        ) {
            row1.forEach { key ->
                val display = if (isUpper) key.uppercase() else key
                TranslucentKey(
                    label = display,
                    settings = settings,
                    rippleState = animationController.activeRipples[key],
                    modifier = Modifier.weight(1f),
                    onPress = { onKeyInput(key) },
                    onAnimate = { x, y ->
                        animationController.triggerKeyPress(key, KeyType.NORMAL, x, y)
                    }
                )
            }
        }

        // Row 2 (with side spacing for classic QWERTY stagger)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KeyboardDimensions.keyHorizontalSpacing)
        ) {
            Spacer(modifier = Modifier.width(10.dp))
            row2.forEach { key ->
                val display = if (isUpper) key.uppercase() else key
                TranslucentKey(
                    label = display,
                    settings = settings,
                    rippleState = animationController.activeRipples[key],
                    modifier = Modifier.weight(1f),
                    onPress = { onKeyInput(key) },
                    onAnimate = { x, y ->
                        animationController.triggerKeyPress(key, KeyType.NORMAL, x, y)
                    }
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
        }

        // Row 3: Shift, Z-M, Backspace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KeyboardDimensions.keyHorizontalSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shift Key
            TranslucentKey(
                icon = Icons.Default.ArrowUpward,
                keyType = KeyType.SHIFT,
                settings = settings,
                rippleState = animationController.activeRipples["shift"],
                isHighlighted = animationController.isShiftActive || animationController.isCapsLock,
                modifier = Modifier.weight(1.4f),
                onPress = onShiftToggle,
                onAnimate = { x, y ->
                    animationController.triggerKeyPress("shift", KeyType.SHIFT, x, y)
                }
            )

            row3.forEach { key ->
                val display = if (isUpper) key.uppercase() else key
                TranslucentKey(
                    label = display,
                    settings = settings,
                    rippleState = animationController.activeRipples[key],
                    modifier = Modifier.weight(1f),
                    onPress = { onKeyInput(key) },
                    onAnimate = { x, y ->
                        animationController.triggerKeyPress(key, KeyType.NORMAL, x, y)
                    }
                )
            }

            // Backspace Key
            TranslucentKey(
                icon = Icons.AutoMirrored.Filled.Backspace,
                keyType = KeyType.BACKSPACE,
                settings = settings,
                rippleState = animationController.activeRipples["backspace"],
                modifier = Modifier.weight(1.4f),
                onPress = onDelete,
                onAnimate = { x, y ->
                    animationController.triggerKeyPress("backspace", KeyType.BACKSPACE, x, y)
                }
            )
        }

        // Row 4: ?123, Emoji, Space, Period, Enter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KeyboardDimensions.keyHorizontalSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ?123 switch
            TranslucentKey(
                label = "?123",
                keyType = KeyType.ACTION,
                settings = settings,
                rippleState = animationController.activeRipples["?123"],
                modifier = Modifier.weight(1.3f),
                onPress = onSwitchToSymbols,
                onAnimate = { x, y ->
                    animationController.triggerKeyPress("?123", KeyType.ACTION, x, y)
                }
            )

            // Emoji button
            TranslucentKey(
                icon = Icons.Default.EmojiEmotions,
                keyType = KeyType.EMOJI,
                settings = settings,
                rippleState = animationController.activeRipples["emoji"],
                modifier = Modifier.weight(1.1f),
                onPress = onSwitchToEmoji,
                onAnimate = { x, y ->
                    animationController.triggerKeyPress("emoji", KeyType.EMOJI, x, y)
                }
            )

            // Spacebar (wide wave effect)
            TranslucentKey(
                label = "MotionKeys",
                keyType = KeyType.SPACE,
                settings = settings,
                rippleState = animationController.activeRipples["space"],
                modifier = Modifier.weight(4.4f),
                onPress = onSpace,
                onAnimate = { x, y ->
                    animationController.triggerKeyPress("space", KeyType.SPACE, x, y)
                }
            )

            // Period
            TranslucentKey(
                label = ".",
                settings = settings,
                rippleState = animationController.activeRipples["."],
                modifier = Modifier.weight(1f),
                onPress = { onKeyInput(".") },
                onAnimate = { x, y ->
                    animationController.triggerKeyPress(".", KeyType.NORMAL, x, y)
                }
            )

            // Enter key
            TranslucentKey(
                icon = Icons.AutoMirrored.Filled.KeyboardReturn,
                keyType = KeyType.ENTER,
                settings = settings,
                rippleState = animationController.activeRipples["enter"],
                modifier = Modifier.weight(1.4f),
                onPress = onEnter,
                onAnimate = { x, y ->
                    animationController.triggerKeyPress("enter", KeyType.ENTER, x, y)
                }
            )
        }
    }
}

@Composable
fun NumbersSymbolsKeyboardLayout(
    settings: KeyboardSettings,
    animationController: KeyAnimationController,
    onKeyInput: (String) -> Unit,
    onSpace: () -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
    onSwitchToAlpha: () -> Unit,
    onSwitchToMoreSymbols: () -> Unit,
    onSwitchToEmoji: () -> Unit
) {
    val row1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val row2 = listOf("@", "#", "$", "%", "&", "*", "-", "+", "(", ")")
    val row3 = listOf("!", "\"", "'", ":", ";", "/", "?")

    Column(
        verticalArrangement = Arrangement.spacedBy(KeyboardDimensions.rowSpacing),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KeyboardDimensions.keyHorizontalSpacing)
        ) {
            row1.forEach { key ->
                TranslucentKey(
                    label = key,
                    settings = settings,
                    rippleState = animationController.activeRipples[key],
                    modifier = Modifier.weight(1f),
                    onPress = { onKeyInput(key) },
                    onAnimate = { x, y ->
                        animationController.triggerKeyPress(key, KeyType.NORMAL, x, y)
                    }
                )
            }
        }

        // Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KeyboardDimensions.keyHorizontalSpacing)
        ) {
            row2.forEach { key ->
                TranslucentKey(
                    label = key,
                    settings = settings,
                    rippleState = animationController.activeRipples[key],
                    modifier = Modifier.weight(1f),
                    onPress = { onKeyInput(key) },
                    onAnimate = { x, y ->
                        animationController.triggerKeyPress(key, KeyType.NORMAL, x, y)
                    }
                )
            }
        }

        // Row 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KeyboardDimensions.keyHorizontalSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TranslucentKey(
                label = "=/<",
                keyType = KeyType.ACTION,
                settings = settings,
                rippleState = animationController.activeRipples["more_sym"],
                modifier = Modifier.weight(1.4f),
                onPress = onSwitchToMoreSymbols,
                onAnimate = { x, y ->
                    animationController.triggerKeyPress("more_sym", KeyType.ACTION, x, y)
                }
            )

            row3.forEach { key ->
                TranslucentKey(
                    label = key,
                    settings = settings,
                    rippleState = animationController.activeRipples[key],
                    modifier = Modifier.weight(1f),
                    onPress = { onKeyInput(key) },
                    onAnimate = { x, y ->
                        animationController.triggerKeyPress(key, KeyType.NORMAL, x, y)
                    }
                )
            }

            TranslucentKey(
                icon = Icons.AutoMirrored.Filled.Backspace,
                keyType = KeyType.BACKSPACE,
                settings = settings,
                rippleState = animationController.activeRipples["backspace"],
                modifier = Modifier.weight(1.4f),
                onPress = onDelete,
                onAnimate = { x, y ->
                    animationController.triggerKeyPress("backspace", KeyType.BACKSPACE, x, y)
                }
            )
        }

        // Row 4
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KeyboardDimensions.keyHorizontalSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TranslucentKey(
                label = "ABC",
                keyType = KeyType.ACTION,
                settings = settings,
                rippleState = animationController.activeRipples["ABC"],
                modifier = Modifier.weight(1.4f),
                onPress = onSwitchToAlpha,
                onAnimate = { x, y ->
                    animationController.triggerKeyPress("ABC", KeyType.ACTION, x, y)
                }
            )

            TranslucentKey(
                icon = Icons.Default.EmojiEmotions,
                keyType = KeyType.EMOJI,
                settings = settings,
                rippleState = animationController.activeRipples["emoji"],
                modifier = Modifier.weight(1.1f),
                onPress = onSwitchToEmoji,
                onAnimate = { x, y ->
                    animationController.triggerKeyPress("emoji", KeyType.EMOJI, x, y)
                }
            )

            TranslucentKey(
                label = "Space",
                keyType = KeyType.SPACE,
                settings = settings,
                rippleState = animationController.activeRipples["space"],
                modifier = Modifier.weight(4.4f),
                onPress = onSpace,
                onAnimate = { x, y ->
                    animationController.triggerKeyPress("space", KeyType.SPACE, x, y)
                }
            )

            TranslucentKey(
                label = ".",
                settings = settings,
                rippleState = animationController.activeRipples["."],
                modifier = Modifier.weight(1f),
                onPress = { onKeyInput(".") },
                onAnimate = { x, y ->
                    animationController.triggerKeyPress(".", KeyType.NORMAL, x, y)
                }
            )

            TranslucentKey(
                icon = Icons.AutoMirrored.Filled.KeyboardReturn,
                keyType = KeyType.ENTER,
                settings = settings,
                rippleState = animationController.activeRipples["enter"],
                modifier = Modifier.weight(1.4f),
                onPress = onEnter,
                onAnimate = { x, y ->
                    animationController.triggerKeyPress("enter", KeyType.ENTER, x, y)
                }
            )
        }
    }
}

@Composable
fun MoreSymbolsKeyboardLayout(
    settings: KeyboardSettings,
    animationController: KeyAnimationController,
    onKeyInput: (String) -> Unit,
    onSpace: () -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
    onSwitchToAlpha: () -> Unit,
    onSwitchToSymbols1: () -> Unit
) {
    val row1 = listOf("~", "`", "|", "•", "√", "π", "÷", "×", "¶", "∆")
    val row2 = listOf("£", "¢", "€", "¥", "^", "°", "=", "{", "}", "\\")
    val row3 = listOf("%", "©", "®", "™", "✓", "[", "]")

    Column(
        verticalArrangement = Arrangement.spacedBy(KeyboardDimensions.rowSpacing),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KeyboardDimensions.keyHorizontalSpacing)
        ) {
            row1.forEach { key ->
                TranslucentKey(
                    label = key,
                    settings = settings,
                    rippleState = animationController.activeRipples[key],
                    modifier = Modifier.weight(1f),
                    onPress = { onKeyInput(key) },
                    onAnimate = { x, y ->
                        animationController.triggerKeyPress(key, KeyType.NORMAL, x, y)
                    }
                )
            }
        }

        // Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KeyboardDimensions.keyHorizontalSpacing)
        ) {
            row2.forEach { key ->
                TranslucentKey(
                    label = key,
                    settings = settings,
                    rippleState = animationController.activeRipples[key],
                    modifier = Modifier.weight(1f),
                    onPress = { onKeyInput(key) },
                    onAnimate = { x, y ->
                        animationController.triggerKeyPress(key, KeyType.NORMAL, x, y)
                    }
                )
            }
        }

        // Row 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KeyboardDimensions.keyHorizontalSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TranslucentKey(
                label = "?123",
                keyType = KeyType.ACTION,
                settings = settings,
                rippleState = animationController.activeRipples["123"],
                modifier = Modifier.weight(1.4f),
                onPress = onSwitchToSymbols1,
                onAnimate = { x, y ->
                    animationController.triggerKeyPress("123", KeyType.ACTION, x, y)
                }
            )

            row3.forEach { key ->
                TranslucentKey(
                    label = key,
                    settings = settings,
                    rippleState = animationController.activeRipples[key],
                    modifier = Modifier.weight(1f),
                    onPress = { onKeyInput(key) },
                    onAnimate = { x, y ->
                        animationController.triggerKeyPress(key, KeyType.NORMAL, x, y)
                    }
                )
            }

            TranslucentKey(
                icon = Icons.AutoMirrored.Filled.Backspace,
                keyType = KeyType.BACKSPACE,
                settings = settings,
                rippleState = animationController.activeRipples["backspace"],
                modifier = Modifier.weight(1.4f),
                onPress = onDelete,
                onAnimate = { x, y ->
                    animationController.triggerKeyPress("backspace", KeyType.BACKSPACE, x, y)
                }
            )
        }

        // Row 4
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(KeyboardDimensions.keyHorizontalSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TranslucentKey(
                label = "ABC",
                keyType = KeyType.ACTION,
                settings = settings,
                rippleState = animationController.activeRipples["ABC"],
                modifier = Modifier.weight(1.4f),
                onPress = onSwitchToAlpha,
                onAnimate = { x, y ->
                    animationController.triggerKeyPress("ABC", KeyType.ACTION, x, y)
                }
            )

            TranslucentKey(
                label = "Space",
                keyType = KeyType.SPACE,
                settings = settings,
                rippleState = animationController.activeRipples["space"],
                modifier = Modifier.weight(5.5f),
                onPress = onSpace,
                onAnimate = { x, y ->
                    animationController.triggerKeyPress("space", KeyType.SPACE, x, y)
                }
            )

            TranslucentKey(
                icon = Icons.AutoMirrored.Filled.KeyboardReturn,
                keyType = KeyType.ENTER,
                settings = settings,
                rippleState = animationController.activeRipples["enter"],
                modifier = Modifier.weight(1.4f),
                onPress = onEnter,
                onAnimate = { x, y ->
                    animationController.triggerKeyPress("enter", KeyType.ENTER, x, y)
                }
            )
        }
    }
}

@Composable
fun EmojiKeyboardLayout(
    onEmojiSelected: (String) -> Unit,
    onSwitchToAlpha: () -> Unit,
    onDelete: () -> Unit
) {
    val emojis = listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣",
        "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰",
        "😘", "😗", "😙", "😚", "😋", "😛", "😜", "🤪",
        "😎", "🤩", "🥳", "😏", "😒", "😞", "😔", "😟",
        "🥺", "😢", "😭", "😤", "😠", "😡", "🤬", "🤯",
        "🔥", "✨", "🎉", "❤️", "💙", "💜", "💯", "👍",
        "🙌", "👏", "🚀", "⚡", "🌟", "💡", "🎯", "🏆"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            items(emojis) { emoji ->
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectTapGestures {
                                onEmojiSelected(emoji)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 22.sp)
                }
            }
        }

        // Bottom action bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x661E293B))
                    .pointerInput(Unit) {
                        detectTapGestures { onSwitchToAlpha() }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Keyboard",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "ABC", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Box(
                modifier = Modifier
                    .width(70.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x661E293B))
                    .pointerInput(Unit) {
                        detectTapGestures { onDelete() }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Translucent Glassmorphic Key with subtle glow, ripple animation, and scale feedback.
 */
@Composable
fun TranslucentKey(
    settings: KeyboardSettings,
    modifier: Modifier = Modifier,
    label: String? = null,
    icon: ImageVector? = null,
    keyType: KeyType = KeyType.NORMAL,
    rippleState: KeyRippleState? = null,
    isHighlighted: Boolean = false,
    height: Dp = KeyboardDimensions.keyHeight,
    onPress: () -> Unit,
    onAnimate: (x: Float, y: Float) -> Unit
) {
    var isPressedState by remember { mutableStateOf(false) }

    // Fast scale feedback animation (100–160ms)
    val scale by animateFloatAsState(
        targetValue = if (isPressedState) 0.93f else 1.0f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "key_scale"
    )

    val baseBackgroundColor = remember(keyType, settings.keyOpacity, isHighlighted) {
        if (isHighlighted) {
            Color(0x886366F1) // glowing active shift/action
        } else {
            KeyboardThemeManager.getKeyBaseColor(keyType, settings.keyOpacity)
        }
    }

    val baseBorderColor = remember(keyType, settings.keyOpacity, isHighlighted) {
        if (isHighlighted) {
            Color(0xFFA5B4FC)
        } else {
            KeyboardThemeManager.getKeyBorderColor(keyType, settings.keyOpacity)
        }
    }

    val glowColor = remember(settings.presetVideoId, keyType) {
        KeyboardThemeManager.getGlowColor(settings.presetVideoId, keyType)
    }

    // Ripple expansion progress
    var rippleActive by remember { mutableStateOf(false) }
    var rippleScale by remember { mutableFloatStateOf(0f) }
    var rippleAlpha by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(rippleState?.triggerTimeMs) {
        if (rippleState != null) {
            rippleActive = true
            rippleScale = 0.2f
            rippleAlpha = 0.8f * settings.glowIntensity

            // Quick 140ms visual ripple
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 160) {
                val elapsed = (System.currentTimeMillis() - startTime).toFloat() / 160f
                rippleScale = 0.2f + elapsed * 1.6f
                rippleAlpha = (1f - elapsed) * 0.8f * settings.glowIntensity
                delay(16)
            }
            rippleActive = false
            rippleAlpha = 0f
        }
    }

    Box(
        modifier = modifier
            .height(height)
            .scale(scale)
            .shadow(
                elevation = if (isHighlighted) 4.dp else 1.dp,
                shape = RoundedCornerShape(KeyboardDimensions.keyCornerRadius),
                ambientColor = Color.Black,
                spotColor = glowColor
            )
            .clip(RoundedCornerShape(KeyboardDimensions.keyCornerRadius))
            .background(baseBackgroundColor)
            .border(
                width = if (isHighlighted) 1.5.dp else 1.dp,
                color = if (isHighlighted) glowColor else baseBorderColor,
                shape = RoundedCornerShape(KeyboardDimensions.keyCornerRadius)
            )
            .pointerInput(keyType, label) {
                detectTapGestures(
                    onPress = { offset ->
                        isPressedState = true
                        val touchX = if (size.width > 0) offset.x / size.width else 0.5f
                        val touchY = if (size.height > 0) offset.y / size.height else 0.5f
                        // 1. Immediately send key event (Non-blocking)
                        onPress()
                        // 2. Trigger visual ripple animation
                        onAnimate(touchX, touchY)
                        tryAwaitRelease()
                        isPressedState = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Active visual ripple/glow layer
        if (rippleActive && rippleAlpha > 0.05f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(KeyboardDimensions.keyCornerRadius))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = rippleAlpha.coerceIn(0f, 0.9f)),
                                glowColor.copy(alpha = (rippleAlpha * 0.4f).coerceIn(0f, 0.6f)),
                                Color.Transparent
                            ),
                            center = Offset(
                                (rippleState?.touchXPercent ?: 0.5f) * 100f,
                                (rippleState?.touchYPercent ?: 0.5f) * 100f
                            ),
                            radius = (120f * rippleScale)
                        )
                    )
            )
        }

        // Key Label or Icon
        if (label != null) {
            Text(
                text = label,
                style = if (keyType == KeyType.NORMAL) {
                    KeyboardThemeManager.PrimaryKeyTextStyle
                } else {
                    KeyboardThemeManager.ActionKeyTextStyle
                },
                color = if (isHighlighted) Color.White else Color(0xFFF8FAFC)
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = keyType.name,
                tint = if (isHighlighted) Color.White else Color(0xFFF8FAFC),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
