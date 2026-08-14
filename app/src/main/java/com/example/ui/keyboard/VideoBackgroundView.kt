package com.example.ui.keyboard

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.util.Log
import android.view.Surface
import android.view.TextureView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.KeyboardSettings
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun VideoBackgroundView(
    settings: KeyboardSettings,
    modifier: Modifier = Modifier,
    isPaused: Boolean = false
) {
    var hasPlaybackError by remember(settings.selectedVideoPath, settings.usePreset) {
        mutableStateOf(false)
    }

    val customFileExists = remember(settings.selectedVideoPath) {
        settings.selectedVideoPath?.let { File(it).exists() } ?: false
    }

    val shouldPlayRealVideo = !settings.usePreset &&
            !settings.selectedVideoPath.isNullOrEmpty() &&
            customFileExists &&
            !hasPlaybackError

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF090D16))) {
        if (shouldPlayRealVideo) {
            RealVideoPlayerView(
                videoPath = settings.selectedVideoPath!!,
                isPaused = isPaused,
                onError = {
                    Log.w("VideoBackgroundView", "Playback fallback to procedural: $it")
                    hasPlaybackError = true
                }
            )
        } else {
            // High fidelity procedural motion canvas background (Presets / Fallback)
            ProceduralMotionBackground(presetId = settings.presetVideoId)
        }

        // Ambient dark vignette overlay to preserve key character readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x33000000),
                            Color(0x66000000),
                            Color(0x99000000)
                        )
                    )
                )
        )
    }
}

@Composable
fun RealVideoPlayerView(
    videoPath: String,
    isPaused: Boolean,
    onError: (String) -> Unit
) {
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentSurface by remember { mutableStateOf<Surface?>(null) }

    DisposableEffect(videoPath) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (_: Exception) {}
            mediaPlayer = null
            currentSurface?.release()
            currentSurface = null
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            TextureView(context).apply {
                fun applyCenterCrop(videoW: Int, videoH: Int) {
                    val viewW = width
                    val viewH = height
                    if (viewW == 0 || viewH == 0 || videoW == 0 || videoH == 0) return

                    val viewRatio = viewW.toFloat() / viewH.toFloat()
                    val videoRatio = videoW.toFloat() / videoH.toFloat()

                    var scaleX = 1f
                    var scaleY = 1f

                    if (viewRatio > videoRatio) {
                        scaleY = (viewRatio / videoRatio)
                    } else {
                        scaleX = (videoRatio / viewRatio)
                    }

                    val matrix = android.graphics.Matrix()
                    matrix.setScale(scaleX, scaleY, viewW / 2f, viewH / 2f)
                    setTransform(matrix)
                }

                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                        try {
                            val surface = Surface(st)
                            currentSurface = surface

                            val player = MediaPlayer().apply {
                                setSurface(surface)
                                val videoFile = File(videoPath)
                                if (videoFile.exists() && videoFile.length() > 0L) {
                                    java.io.FileInputStream(videoFile).use { fis ->
                                        setDataSource(fis.fd)
                                    }
                                } else {
                                    setDataSource(videoPath)
                                }
                                isLooping = true
                                setVolume(0f, 0f) // Completely silent / muted
                                setOnErrorListener { _, what, extra ->
                                    onError("Playback error code: $what, extra: $extra")
                                    true
                                }
                                setOnVideoSizeChangedListener { _, vWidth, vHeight ->
                                    post {
                                        applyCenterCrop(vWidth, vHeight)
                                    }
                                }
                                setOnPreparedListener { mp ->
                                    try {
                                        applyCenterCrop(mp.videoWidth, mp.videoHeight)
                                        if (!isPaused) {
                                            mp.start()
                                        }
                                    } catch (e: Exception) {
                                        onError("Start failed: ${e.message}")
                                    }
                                }
                                prepareAsync()
                            }
                            mediaPlayer = player
                        } catch (e: Exception) {
                            Log.e("RealVideoPlayerView", "Error initializing MediaPlayer", e)
                            onError(e.localizedMessage ?: "Player error")
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {
                        mediaPlayer?.let { mp ->
                            try {
                                applyCenterCrop(mp.videoWidth, mp.videoHeight)
                            } catch (_: Exception) {}
                        }
                    }

                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        try {
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                        } catch (_: Exception) {}
                        mediaPlayer = null
                        currentSurface?.release()
                        currentSurface = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                }
            }
        },
        update = {
            if (isPaused) {
                try {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.pause()
                    }
                } catch (_: Exception) {}
            } else {
                try {
                    if (mediaPlayer?.isPlaying == false) {
                        mediaPlayer?.start()
                    }
                } catch (_: Exception) {}
            }
        }
    )
}

@Composable
fun ProceduralMotionBackground(presetId: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "motion_loop")

    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "motion_progress"
    )

    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_phase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0 || height <= 0) return@Canvas

        val phase = animationProgress * 2f * Math.PI.toFloat()

        when (presetId) {
            "aurora_borealis" -> {
                // Ethereal emerald & violet auroral ribbons
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF021B1A), Color(0xFF0A0F24), Color(0xFF030712))
                    )
                )

                for (i in 0..3) {
                    val wavePath = Path()
                    val waveYBase = height * (0.3f + i * 0.18f)
                    wavePath.moveTo(0f, height)
                    wavePath.lineTo(0f, waveYBase)

                    val steps = 24
                    for (step in 0..steps) {
                        val x = (width / steps) * step
                        val y = waveYBase +
                                sin(phase + step * 0.4f + i * 1.2f) * (height * 0.12f) +
                                cos(phase * 0.5f + step * 0.2f) * 15f
                        wavePath.lineTo(x, y)
                    }
                    wavePath.lineTo(width, height)
                    wavePath.close()

                    val color = if (i % 2 == 0) {
                        Color(0xFF10B981).copy(alpha = 0.25f + pulsePhase * 0.15f)
                    } else {
                        Color(0xFF8B5CF6).copy(alpha = 0.22f + (1f - pulsePhase) * 0.15f)
                    }

                    drawPath(
                        path = wavePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(color, color.copy(alpha = 0.05f)),
                            startY = 0f,
                            endY = height
                        ),
                        style = Fill
                    )
                }
            }

            "cosmic_nebula" -> {
                // Deep space stardust and cosmic energy
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF2E1065), Color(0xFF090D16), Color(0xFF030712)),
                        center = Offset(width * (0.3f + 0.4f * sin(phase)), height * 0.5f),
                        radius = width * 0.8f
                    )
                )

                // Swirling nebula orb 1
                val orb1X = width * (0.5f + 0.3f * cos(phase))
                val orb1Y = height * (0.4f + 0.25f * sin(phase * 0.8f))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x66F43F5E), Color(0x228B5CF6), Color(0x00000000)),
                        center = Offset(orb1X, orb1Y),
                        radius = width * 0.45f
                    ),
                    radius = width * 0.45f,
                    center = Offset(orb1X, orb1Y)
                )

                // Swirling nebula orb 2
                val orb2X = width * (0.5f - 0.35f * sin(phase * 0.9f))
                val orb2Y = height * (0.6f + 0.2f * cos(phase))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x5538BDF8), Color(0x116366F1), Color(0x00000000)),
                        center = Offset(orb2X, orb2Y),
                        radius = width * 0.4f
                    ),
                    radius = width * 0.4f,
                    center = Offset(orb2X, orb2Y)
                )
            }

            "liquid_motion" -> {
                // Flowing magma amber/rose currents
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1C0A00), Color(0xFF2D1204), Color(0xFF0B0401))
                    )
                )

                for (i in 0..2) {
                    val path = Path()
                    val startY = height * (0.25f + i * 0.28f)
                    path.moveTo(0f, startY)

                    val points = 20
                    for (p in 0..points) {
                        val px = (width / points) * p
                        val py = startY +
                                sin(phase * 1.2f + p * 0.5f + i) * 35f +
                                cos(phase * 0.8f + p * 0.3f) * 20f
                        path.lineTo(px, py)
                    }

                    drawPath(
                        path = path,
                        color = if (i == 0) Color(0xFFF59E0B).copy(alpha = 0.3f)
                        else if (i == 1) Color(0xFFEF4444).copy(alpha = 0.25f)
                        else Color(0xFFFCD34D).copy(alpha = 0.2f),
                        style = Stroke(width = 32f)
                    )
                }
            }

            "synthwave_sunset" -> {
                // Retro synthwave horizon with perspective grid
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E1035), Color(0xFF4C0519), Color(0xFF030712))
                    )
                )

                // Sun
                val sunCenter = Offset(width * 0.5f, height * 0.35f)
                drawCircle(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFDE047), Color(0xFFF43F5E), Color(0xFF7C3AED)),
                        startY = sunCenter.y - 60f,
                        endY = sunCenter.y + 60f
                    ),
                    radius = 65f,
                    center = sunCenter
                )

                // Perspective grid lines
                val horizonY = height * 0.5f
                for (x in 0..12) {
                    val bottomX = (width / 12) * x
                    drawLine(
                        color = Color(0x44EC4899),
                        start = Offset(width * 0.5f, horizonY),
                        end = Offset(bottomX, height),
                        strokeWidth = 2f
                    )
                }

                // Moving horizontal grid bars
                val offset = (animationProgress * 30f) % 30f
                var gy = horizonY + offset
                while (gy < height) {
                    val progress = (gy - horizonY) / (height - horizonY)
                    drawLine(
                        color = Color(0xFFEC4899).copy(alpha = 0.1f + progress * 0.4f),
                        start = Offset(0f, gy),
                        end = Offset(width, gy),
                        strokeWidth = 1.5f + progress * 2f
                    )
                    gy += 20f + progress * 25f
                }
            }

            else -> {
                // "cyber_neon" - Cyberpunk energy wave grid
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF050E1F), Color(0xFF0B1B3D), Color(0xFF060913))
                    )
                )

                // Glowing energy streams
                for (i in 0..3) {
                    val wavePath = Path()
                    val yCenter = height * (0.25f + i * 0.2f)
                    wavePath.moveTo(0f, yCenter)

                    val steps = 20
                    for (s in 0..steps) {
                        val x = (width / steps) * s
                        val y = yCenter + sin(phase + s * 0.5f + i * 1.5f) * (20f + i * 10f)
                        wavePath.lineTo(x, y)
                    }

                    val color = if (i % 2 == 0) Color(0xFF06B6D4) else Color(0xFF818CF8)
                    drawPath(
                        path = wavePath,
                        color = color.copy(alpha = 0.35f + pulsePhase * 0.2f),
                        style = Stroke(width = 16f - i * 2f)
                    )
                }

                // Pulsing floating energy particles
                for (p in 0..15) {
                    val seed = p * 137.5f
                    val px = (width * ((sin(seed + phase * 0.3f) + 1f) / 2f))
                    val py = (height * ((cos(seed * 0.8f + phase * 0.5f) + 1f) / 2f))
                    drawCircle(
                        color = Color(0xFF22D3EE).copy(alpha = 0.3f + 0.4f * sin(phase + p)),
                        radius = 4f + (p % 4) * 2f,
                        center = Offset(px, py)
                    )
                }
            }
        }
    }
}
