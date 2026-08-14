package com.example.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.animation.AnimationStyle
import com.example.animation.KeyAnimationController
import com.example.data.KeyboardPreferences
import com.example.ui.keyboard.KeyboardView
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepBackground
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.IndigoPrimaryLight
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceDark
import com.example.video.VideoBackgroundManager
import com.example.video.VideoInfo
import com.example.video.VideoPresets
import com.example.video.VideoValidationResult
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MotionKeysSettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { KeyboardPreferences.getInstance(context) }
    val videoManager = remember { VideoBackgroundManager(context) }
    val animationController = remember { KeyAnimationController() }

    val settings by preferences.settingsFlow.collectAsState()

    var isImeEnabled by remember { mutableStateOf(false) }
    var isImeDefault by remember { mutableStateOf(false) }
    var customVideoInfo by remember { mutableStateOf<VideoInfo?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var previewTestText by remember { mutableStateOf("") }

    // Helper to refresh keyboard enabled / active status
    fun refreshImeStatus() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledImes = imm.enabledInputMethodList
        val myPackage = context.packageName
        isImeEnabled = enabledImes.any { it.packageName == myPackage }

        val currentIme = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        )
        isImeDefault = currentIme != null && currentIme.contains(myPackage)
    }

    LaunchedEffect(Unit) {
        refreshImeStatus()
        customVideoInfo = videoManager.getActiveCustomVideoInfo()
    }

    // Video Picker Launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                errorMessage = null
                // Extract file display name if possible
                var originalName: String? = null
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            originalName = cursor.getString(nameIndex)
                        }
                    }
                } catch (_: Exception) {}

                when (val result = videoManager.saveAndValidateUserVideo(uri, originalName)) {
                    is VideoValidationResult.Success -> {
                        customVideoInfo = result.videoInfo
                        errorMessage = null
                    }
                    is VideoValidationResult.Error -> {
                        errorMessage = result.message
                    }
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DeepBackground
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item { Spacer(modifier = Modifier.height(16.dp)) }

            // 1. App Hero Banner
            item {
                HeaderHeroCard()
            }

            // 2. Setup / Activation Status Card
            item {
                SetupStatusCard(
                    isImeEnabled = isImeEnabled,
                    isImeDefault = isImeDefault,
                    onEnableClick = {
                        try {
                            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    },
                    onSelectClick = {
                        try {
                            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            imm.showInputMethodPicker()
                        } catch (_: Exception) {}
                    },
                    onRefresh = { refreshImeStatus() }
                )
            }

            // 3. Error Banner (if any)
            if (errorMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x33F43F5E)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RoseAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = RoseAccent,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // 4. Background Video Section
            item {
                BackgroundVideoSection(
                    settings = settings,
                    customVideoInfo = customVideoInfo,
                    onChooseVideo = { videoPickerLauncher.launch("video/*") },
                    onRemoveCustomVideo = {
                        preferences.clearCustomVideo()
                        customVideoInfo = null
                    },
                    onSelectPreset = { presetId ->
                        preferences.setPresetVideo(presetId)
                    }
                )
            }

            // 5. Live Interactive Keyboard Preview
            item {
                PreviewSection(
                    settings = settings,
                    animationController = animationController,
                    previewTestText = previewTestText,
                    onTextCommit = { char ->
                        previewTestText += char
                    },
                    onDelete = {
                        if (previewTestText.isNotEmpty()) {
                            previewTestText = previewTestText.dropLast(1)
                        }
                    },
                    onEnter = {
                        previewTestText += "\n"
                    },
                    onClearText = { previewTestText = "" }
                )
            }

            // 6. Animation Effects & Customization
            item {
                AnimationCustomizationSection(
                    settings = settings,
                    onOpacityChange = { preferences.setKeyOpacity(it) },
                    onGlowIntensityChange = { preferences.setGlowIntensity(it) },
                    onAnimationStyleChange = { preferences.setAnimationStyle(it) },
                    onHapticToggle = { preferences.setHapticEnabled(it) },
                    onSoundToggle = { preferences.setSoundEnabled(it) }
                )
            }

            // 7. Privacy & Offline Architecture Card
            item {
                PrivacyCard()
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun HeaderHeroCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E1B4B),
                        Color(0xFF0F172A),
                        Color(0xFF111827)
                    )
                )
            )
            .border(1.dp, Color(0x406366F1), RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(listOf(IndigoPrimary, CyanAccent))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "MotionKeys",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "MotionKeys",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Animated Video Keyboard",
                            color = CyanAccent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Offline tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x3310B981))
                        .border(1.dp, EmeraldAccent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "100% Offline",
                        color = EmeraldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Turn any 10-second animated MP4 video into the live looping background of your real smartphone keyboard.",
                color = Color(0xFF94A3B8),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun SetupStatusCard(
    isImeEnabled: Boolean,
    isImeDefault: Boolean,
    onEnableClick: () -> Unit,
    onSelectClick: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("setup_status_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33475569))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Keyboard Activation",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onRefresh, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh status",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Step 1: Enable Keyboard in Settings
            StatusStepItem(
                stepNumber = "1",
                title = "Enable MotionKeys in Android Settings",
                description = if (isImeEnabled) "Enabled in keyboard list" else "Open keyboard settings and enable MotionKeys",
                isCompleted = isImeEnabled,
                actionLabel = "Enable",
                onAction = onEnableClick
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Step 2: Select Keyboard as Default
            StatusStepItem(
                stepNumber = "2",
                title = "Select MotionKeys as Active Keyboard",
                description = if (isImeDefault) "Currently active keyboard" else "Switch default keyboard to MotionKeys",
                isCompleted = isImeDefault,
                actionLabel = "Select",
                onAction = onSelectClick
            )
        }
    }
}

@Composable
fun StatusStepItem(
    stepNumber: String,
    title: String,
    description: String,
    isCompleted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x330F172A))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon or Number
        if (isCompleted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Completed",
                tint = EmeraldAccent,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(0x446366F1)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber,
                    color = IndigoPrimaryLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isCompleted) Color(0xFFF1F5F9) else Color(0xFFE2E8F0),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                color = if (isCompleted) EmeraldAccent else Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCompleted) Color(0x3310B981) else IndigoPrimary,
                contentColor = if (isCompleted) EmeraldAccent else Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text(
                text = if (isCompleted) "Done" else actionLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun BackgroundVideoSection(
    settings: com.example.data.KeyboardSettings,
    customVideoInfo: VideoInfo?,
    onChooseVideo: () -> Unit,
    onRemoveCustomVideo: () -> Unit,
    onSelectPreset: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33475569))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = "Background Video",
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Background Video",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Choose Video Button
                Button(
                    onClick = onChooseVideo,
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp).testTag("choose_video_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Choose Video", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Custom Video Info Banner (if chosen)
            if (customVideoInfo != null && !settings.usePreset) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x3306B6D4))
                        .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Background video selected",
                                    color = CyanAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = customVideoInfo.title,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${customVideoInfo.width}x${customVideoInfo.height} • ${customVideoInfo.durationFormatted} • ${customVideoInfo.fileSizeFormatted}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }

                        IconButton(
                            onClick = onRemoveCustomVideo,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove custom video",
                                tint = RoseAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            Text(
                text = "Preset Motion Themes",
                color = Color(0xFFCBD5E1),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Presets Horizontal Carousel
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(VideoPresets.ALL) { preset ->
                    val isSelected = settings.usePreset && settings.presetVideoId == preset.id
                    PresetItemCard(
                        preset = preset,
                        isSelected = isSelected,
                        onSelect = { onSelectPreset(preset.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun PresetItemCard(
    preset: com.example.video.MotionPreset,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(130.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x330F172A))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) CyanAccent else Color(0x33475569),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
            .padding(10.dp)
    ) {
        Column {
            // Gradient swatch
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(preset.primaryColor),
                                Color(preset.secondaryColor),
                                Color(preset.accentColor)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0x99000000))
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = preset.title,
                color = if (isSelected) CyanAccent else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isSelected) "Active" else "Select",
                color = if (isSelected) EmeraldAccent else Color(0xFF94A3B8),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun PreviewSection(
    settings: com.example.data.KeyboardSettings,
    animationController: KeyAnimationController,
    previewTestText: String,
    onTextCommit: (String) -> Unit,
    onDelete: () -> Unit,
    onEnter: () -> Unit,
    onClearText: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("preview_section_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33475569))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Preview",
                        tint = IndigoPrimaryLight,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Live Keyboard Preview",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (previewTestText.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearText,
                        modifier = Modifier.height(28.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Clear", fontSize = 10.sp, color = RoseAccent)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Typing Test Bar
            OutlinedTextField(
                value = previewTestText,
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("Tap preview keys below to test typing...", fontSize = 13.sp, color = Color(0xFF64748B)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0x44090D16),
                    unfocusedContainerColor = Color(0x44090D16),
                    focusedBorderColor = IndigoPrimary,
                    unfocusedBorderColor = Color(0x33475569),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Keyboard Preview Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x446366F1), RoundedCornerShape(12.dp))
            ) {
                KeyboardView(
                    settings = settings,
                    animationController = animationController,
                    onTextCommit = onTextCommit,
                    onDelete = onDelete,
                    onEnter = onEnter,
                    onOpenSettings = null
                )
            }
        }
    }
}

@Composable
fun AnimationCustomizationSection(
    settings: com.example.data.KeyboardSettings,
    onOpacityChange: (Float) -> Unit,
    onGlowIntensityChange: (Float) -> Unit,
    onAnimationStyleChange: (String) -> Unit,
    onHapticToggle: (Boolean) -> Unit,
    onSoundToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33475569))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Animation Settings",
                    tint = RoseAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Effects & Transparency",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Key Opacity Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Key Transparency", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    Text(
                        text = "${(settings.keyOpacity * 100).toInt()}%",
                        color = CyanAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = settings.keyOpacity,
                    onValueChange = onOpacityChange,
                    valueRange = 0.15f..0.85f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanAccent,
                        activeTrackColor = CyanAccent,
                        inactiveTrackColor = Color(0x33334155)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Glow Intensity Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Touch Ripple Glow", color = Color(0xFFCBD5E1), fontSize = 13.sp)
                    Text(
                        text = String.format("%.1fx", settings.glowIntensity),
                        color = IndigoPrimaryLight,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = settings.glowIntensity,
                    onValueChange = onGlowIntensityChange,
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = IndigoPrimary,
                        activeTrackColor = IndigoPrimary,
                        inactiveTrackColor = Color(0x33334155)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Animation Style Selector
            Text(text = "Key Press Animation Effect", color = Color(0xFFCBD5E1), fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AnimationStyle.values().forEach { style ->
                    val isSelected = settings.animationStyle == style.name
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) IndigoPrimary else Color(0x331E293B))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) IndigoPrimaryLight else Color(0x33475569),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onAnimationStyleChange(style.name) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = style.label.split(" ").first(),
                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Haptic Feedback Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Haptic Vibration", color = Color.White, fontSize = 13.sp)
                }
                Switch(
                    checked = settings.hapticEnabled,
                    onCheckedChange = onHapticToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = IndigoPrimary
                    )
                )
            }

            // 5. Sound Feedback Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Key Click Sound", color = Color.White, fontSize = 13.sp)
                }
                Switch(
                    checked = settings.soundEnabled,
                    onCheckedChange = onSoundToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = IndigoPrimary
                    )
                )
            }
        }
    }
}

@Composable
fun PrivacyCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x2210B981)),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Privacy",
                tint = EmeraldAccent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "100% Private & Offline",
                    color = EmeraldAccent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "MotionKeys operates entirely on-device without internet access. No keystrokes are ever logged, stored, or transmitted.",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
