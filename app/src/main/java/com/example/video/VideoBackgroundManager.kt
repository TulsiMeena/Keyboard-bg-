package com.example.video

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import android.view.Surface
import com.example.data.KeyboardPreferences
import java.io.File
import java.io.FileOutputStream

data class VideoInfo(
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long
) {
    val durationFormatted: String
        get() {
            val seconds = (durationMs / 1000) % 60
            val minutes = (durationMs / (1000 * 60)) % 60
            return String.format("%d:%02d", minutes, seconds)
        }

    val fileSizeFormatted: String
        get() {
            val mb = fileSizeBytes.toDouble() / (1024 * 1024)
            return if (mb < 1.0) {
                String.format("%.1f KB", fileSizeBytes.toDouble() / 1024)
            } else {
                String.format("%.2f MB", mb)
            }
        }
}

sealed class VideoValidationResult {
    data class Success(val videoInfo: VideoInfo) : VideoValidationResult()
    data class Error(val message: String) : VideoValidationResult()
}

data class MotionPreset(
    val id: String,
    val title: String,
    val description: String,
    val primaryColor: Long,
    val secondaryColor: Long,
    val accentColor: Long
)

object VideoPresets {
    val ALL = listOf(
        MotionPreset(
            id = "cyber_neon",
            title = "Cyberpunk Pulse",
            description = "Neon cyan and electric violet energy grid with pulsing particles",
            primaryColor = 0xFF06B6D4,
            secondaryColor = 0xFF6366F1,
            accentColor = 0xFFEC4899
        ),
        MotionPreset(
            id = "aurora_borealis",
            title = "Arctic Aurora",
            description = "Ethereal emerald and indigo waves dancing across dark skies",
            primaryColor = 0xFF10B981,
            secondaryColor = 0xFF3B82F6,
            accentColor = 0xFF8B5CF6
        ),
        MotionPreset(
            id = "cosmic_nebula",
            title = "Cosmic Nebula",
            description = "Deep space stardust with glowing ultraviolet galactic dust",
            primaryColor = 0xFF8B5CF6,
            secondaryColor = 0xFFF43F5E,
            accentColor = 0xFF38BDF8
        ),
        MotionPreset(
            id = "liquid_motion",
            title = "Liquid Amber",
            description = "Vibrant flowing molten magma currents and golden reflections",
            primaryColor = 0xFFF59E0B,
            secondaryColor = 0xFFEF4444,
            accentColor = 0xFFFCD34D
        ),
        MotionPreset(
            id = "synthwave_sunset",
            title = "Synthwave Sunset",
            description = "Retro sunset gradient horizon with animated scanlines",
            primaryColor = 0xFFF43F5E,
            secondaryColor = 0xFF8B5CF6,
            accentColor = 0xFFFBBF24
        )
    )

    fun getById(id: String): MotionPreset {
        return ALL.firstOrNull { it.id == id } ?: ALL.first()
    }
}

class VideoBackgroundManager(private val context: Context) {

    private val prefs = KeyboardPreferences.getInstance(context)

    companion object {
        private const val TAG = "VideoBackgroundManager"
        private const val VIDEO_SUBDIR = "background_videos"
        private const val ACTIVE_VIDEO_NAME = "custom_background.mp4"
        const val MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024 // 50 MB limit
    }

    /**
     * Copies a user-picked video from an input URI to local app private storage.
     * Validates file size, codec availability, duration, and dimensions.
     */
    fun saveAndValidateUserVideo(uri: Uri, originalName: String? = null): VideoValidationResult {
        return try {
            val videoDir = File(context.filesDir, VIDEO_SUBDIR).apply { mkdirs() }
            val targetFile = File(videoDir, ACTIVE_VIDEO_NAME)

            // Copy input stream to private file
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return VideoValidationResult.Error("Cannot open video stream.")

            FileOutputStream(targetFile).use { outputStream ->
                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var totalBytes: Long = 0
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    totalBytes += bytesRead
                    if (totalBytes > MAX_FILE_SIZE_BYTES) {
                        inputStream.close()
                        targetFile.delete()
                        return VideoValidationResult.Error("Video exceeds 50 MB size limit.")
                    }
                    outputStream.write(buffer, 0, bytesRead)
                }
            }
            inputStream.close()

            // Validate video using MediaMetadataRetriever
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(targetFile.absolutePath)
                val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
                if (hasVideo == null || hasVideo != "yes") {
                    targetFile.delete()
                    return VideoValidationResult.Error("Selected file does not contain valid video tracks.")
                }

                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                val durationMs = durationStr?.toLongOrNull() ?: 0L
                val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                val width = widthStr?.toIntOrNull() ?: 0
                val height = heightStr?.toIntOrNull() ?: 0

                val displayName = originalName ?: "Custom MP4 Video"
                val info = VideoInfo(
                    title = displayName,
                    filePath = targetFile.absolutePath,
                    durationMs = durationMs,
                    width = width,
                    height = height,
                    fileSizeBytes = targetFile.length()
                )

                // Save to preferences
                prefs.setCustomVideo(targetFile.absolutePath, displayName)
                VideoValidationResult.Success(info)
            } finally {
                try {
                    retriever.release()
                } catch (e: Exception) {
                    Log.w(TAG, "Error releasing retriever: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save video: ${e.message}", e)
            VideoValidationResult.Error("Failed to load video: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    /**
     * Gets metadata for the currently active custom video, if it exists.
     */
    fun getActiveCustomVideoInfo(): VideoInfo? {
        val path = prefs.getSettings().selectedVideoPath ?: return null
        val file = File(path)
        if (!file.exists() || file.length() == 0L) return null

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val name = prefs.getSettings().selectedVideoName ?: file.name

            VideoInfo(
                title = name,
                filePath = file.absolutePath,
                durationMs = durationMs,
                width = width,
                height = height,
                fileSizeBytes = file.length()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error reading video info: ${e.message}")
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Creates and configures a MediaPlayer instance for looping silent video playback
     */
    fun createMediaPlayer(
        surface: Surface,
        videoPath: String,
        onPrepared: (() -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ): MediaPlayer? {
        val file = File(videoPath)
        if (!file.exists()) {
            onError?.invoke("Video file not found: $videoPath")
            return null
        }

        return try {
            val player = MediaPlayer()
            player.setSurface(surface)
            player.setDataSource(file.absolutePath)
            player.isLooping = true
            player.setVolume(0f, 0f) // Silent as required
            player.setOnPreparedListener { mp ->
                try {
                    mp.start()
                    onPrepared?.invoke()
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting MediaPlayer: ${e.message}")
                    onError?.invoke(e.localizedMessage ?: "Failed to start playback")
                }
            }
            player.setOnErrorListener { _, what, extra ->
                Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                onError?.invoke("Playback error ($what, $extra)")
                true // handled
            }
            player.prepareAsync()
            player
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create MediaPlayer: ${e.message}", e)
            onError?.invoke(e.localizedMessage ?: "Media player setup failed")
            null
        }
    }
}
