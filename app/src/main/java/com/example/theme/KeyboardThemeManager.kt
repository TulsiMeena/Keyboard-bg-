package com.example.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animation.KeyType

object KeyboardDimensions {
    val keyHeight = 48.dp
    val rowSpacing = 7.dp
    val keyHorizontalSpacing = 5.dp
    val keyCornerRadius = 9.dp
    val keyboardVerticalPadding = 8.dp
    val keyboardHorizontalPadding = 6.dp
}

class KeyboardThemeManager {

    companion object {
        val TextShadowStyle = Shadow(
            color = Color(0xDD000000),
            offset = Offset(0f, 2f),
            blurRadius = 4f
        )

        val PrimaryKeyTextStyle = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFFFFFF),
            shadow = TextShadowStyle,
            fontFamily = FontFamily.SansSerif
        )

        val SecondaryKeyTextStyle = TextStyle(
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFE2E8F0),
            shadow = TextShadowStyle,
            fontFamily = FontFamily.SansSerif
        )

        val ActionKeyTextStyle = TextStyle(
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFFFFFFF),
            shadow = TextShadowStyle,
            fontFamily = FontFamily.SansSerif
        )

        fun getKeyBaseColor(keyType: KeyType, opacity: Float): Color {
            val baseColor = when (keyType) {
                KeyType.ENTER -> Color(0xFF4F46E5) // Indigo accent for enter
                KeyType.BACKSPACE, KeyType.SHIFT, KeyType.ACTION -> Color(0xFF1E293B) // Dark slate
                KeyType.SPACE -> Color(0xFF0F172A) // Deep slate for spacebar
                KeyType.EMOJI -> Color(0xFF1E293B)
                KeyType.NORMAL -> Color(0xFF0F172A)
            }
            return baseColor.copy(alpha = opacity.coerceIn(0.1f, 0.9f))
        }

        fun getKeyBorderColor(keyType: KeyType, opacity: Float): Color {
            val borderAlpha = (opacity * 1.5f).coerceIn(0.2f, 0.8f)
            return when (keyType) {
                KeyType.ENTER -> Color(0xFF818CF8).copy(alpha = borderAlpha)
                KeyType.SHIFT -> Color(0xFFA5B4FC).copy(alpha = borderAlpha)
                else -> Color(0xFF94A3B8).copy(alpha = borderAlpha * 0.7f)
            }
        }

        fun getGlowColor(presetId: String, keyType: KeyType): Color {
            if (keyType == KeyType.ENTER) return Color(0xFF818CF8)
            if (keyType == KeyType.BACKSPACE) return Color(0xFFF43F5E)
            if (keyType == KeyType.SHIFT) return Color(0xFF38BDF8)
            if (keyType == KeyType.EMOJI) return Color(0xFFFBBF24)

            return when (presetId) {
                "aurora_borealis" -> Color(0xFF34D399)
                "cosmic_nebula" -> Color(0xFFC084FC)
                "liquid_motion" -> Color(0xFFFBBF24)
                "synthwave_sunset" -> Color(0xFFFB7185)
                else -> Color(0xFF38BDF8) // cyber neon default
            }
        }
    }
}
