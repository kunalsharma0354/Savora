package com.nexora.savora.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexora.savora.BuildConfig
import com.nexora.savora.R
import com.nexora.savora.data.DownloadEngine
import com.nexora.savora.data.FetchResult
import com.nexora.savora.data.MediaInfo
import com.nexora.savora.data.PostProcess
import com.nexora.savora.data.SaveResult
import com.nexora.savora.model.AudioBitrates
import com.nexora.savora.model.DlPhase
import com.nexora.savora.model.MediaMode
import com.nexora.savora.model.VideoQualities
import com.nexora.savora.model.actualFileName
import com.nexora.savora.model.platformLabel
import com.nexora.savora.model.previewFileName
import com.nexora.savora.ui.components.GlassCard
import com.nexora.savora.ui.components.GlassIconButton
import com.nexora.savora.ui.components.GradNumberCircle
import com.nexora.savora.ui.components.PrimaryButton
import com.nexora.savora.ui.components.PulseDot
import com.nexora.savora.ui.components.QualityChip
import com.nexora.savora.ui.components.SectionLabel
import com.nexora.savora.ui.components.TagPill
import com.nexora.savora.ui.icons.AppIcons
import com.nexora.savora.ui.theme.MonoNavBar
import com.nexora.savora.ui.theme.MonoPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(snackbarHostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current

    var url by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(MediaMode.VIDEO_AUDIO) }
    var videoQuality by rememberSaveable { mutableStateOf("720p") }
    var audioBitrate by rememberSaveable { mutableStateOf("320 kbps") }
    var phase by rememberSaveable { mutableStateOf(DlPhase.Idle) }
    var progress by remember { mutableStateOf(0) }
    var resolved by remember { mutableStateOf<MediaInfo?>(null) }
    var availableQualities by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var resolvedUrl by remember { mutableStateOf("") }
    var resolvedSettingsKey by remember { mutableStateOf("") }
    var lastSavedName by remember { mutableStateOf<String?>(null) }

    // Settings/url change ho jaane par in-flight fetch/save ka result discard karo.
    var settingsVersion by remember { mutableStateOf(0) }
    var savingInFlight by remember { mutableStateOf(false) }

    fun currentSettingsKey() = "$mode|$videoQuality|$audioBitrate"

    // Rotation / process death: Parsing-Saving coroutines mar jate hain — UI ko stuck state se nikaalo.
    LaunchedEffect(Unit) {
        if (phase == DlPhase.Parsing || phase == DlPhase.Saving) {
            phase = DlPhase.Ready
            resolved = null
            progress = 0
        }
    }

    val openSavedFile: () -> Unit = {
        val name = lastSavedName
        if (!name.isNullOrBlank()) {
            scope.launch(Dispatchers.IO) {
            val uri = try {
                context.contentResolver.query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Downloads._ID),
                    "${MediaStore.Downloads.DISPLAY_NAME}=?",
                    arrayOf(name),
                    null
                )?.use { c ->
                    if (c.moveToFirst()) {
                        Uri.withAppendedPath(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            c.getLong(0).toString()
                        )
                    } else null
                }
            } catch (e: Exception) {
                null
            }
            if (uri != null) {
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setDataAndType(uri, "*/*")
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    )
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar("Could not open the saved file")
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    snackbarHostState.showSnackbar("File not found in Downloads")
                }
            }
        }
        }
    }

    val pasteFromClipboard: () -> Unit = {
        scope.launch {
            val text = clipboard.getText()?.text?.trim()
            if (!text.isNullOrBlank()) {
                url = text
                settingsVersion++
                phase = DlPhase.Idle
            }
        }
    }

    val fetchMedia: () -> Unit = {
        keyboard?.hide()
        if (url.trim().isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("Paste a valid link first") }
        } else {
            url = url.trim()
            phase = DlPhase.Parsing
            scope.launch {
                val fetchVersion = settingsVersion
                val reqMode = mode
                val reqQuality = videoQuality
                val reqBitrate = audioBitrate
                when (val result = DownloadEngine.resolve(url, reqMode, reqQuality, reqBitrate)) {
                    is FetchResult.Success -> {
                        // User ne beech mein settings/url badla — purana result discard karo.
                        if (fetchVersion != settingsVersion) return@launch
                        resolved = result.media
                        availableQualities = result.media.qualities
                        resolvedUrl = url
                        resolvedSettingsKey = currentSettingsKey()
                        phase = DlPhase.Ready
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    }

                    is FetchResult.Error -> {
                        if (fetchVersion != settingsVersion) return@launch
                        phase = DlPhase.Idle
                        snackbarHostState.showSnackbar(result.message)
                    }
                }
            }
        }
    }

    lateinit var saveMedia: () -> Unit

    val handleDownload: (MediaInfo, Boolean) -> Unit = { media, fromCache ->
        phase = DlPhase.Saving
        savingInFlight = true
        progress = 0
        scope.launch {
            val saveVersion = settingsVersion
            val fileName = actualFileName(platformLabel(url), media)
            val save = when (media.process) {
                PostProcess.VIDEO_ONLY, PostProcess.AUDIO_ONLY -> {
                    DownloadEngine.downloadAndSaveProcessed(
                        context, media.url, fileName, media.mime, media.process
                    ) { progress = it }
                }
                else -> if (media.audioUrl != null) {
                    DownloadEngine.downloadAndSaveMerged(
                        context, media.url, media.audioUrl, fileName, media.mime
                    ) { progress = it }
                } else {
                    DownloadEngine.downloadAndSave(
                        context, media.url, fileName, media.mime
                    ) { progress = it }
                }
            }
            // User ne beech mein settings/url badla — is save ka result UI pe mat lagao.
            if (saveVersion != settingsVersion) {
                if (save is SaveResult.Success) {
                    snackbarHostState.showSnackbar(
                        "Saved with previous settings — tap Save to download with new settings"
                    )
                } else {
                    snackbarHostState.showSnackbar("Previous save was interrupted — tap Save again")
                }
            } else {
                when (save) {
                    is SaveResult.Success -> {
                        phase = DlPhase.Saved
                        progress = 100
                        lastSavedName = save.fileName
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        snackbarHostState.showSnackbar("Saved to Downloads")
                    }

                    is SaveResult.Error -> {
                        if (fromCache) {
                            phase = DlPhase.Ready
                            resolved = null
                            saveMedia()
                        } else {
                            phase = DlPhase.Ready
                            snackbarHostState.showSnackbar(save.message)
                        }
                    }
                }
            }
            savingInFlight = false
        }
    }

    saveMedia = {
        if (!savingInFlight) {
            val media = resolved
            if (media != null && resolvedUrl == url.trim() && resolvedSettingsKey == currentSettingsKey()) {
                handleDownload(media, true)
            } else {
                phase = DlPhase.Saving
                savingInFlight = true
                scope.launch {
                    val saveVersion = settingsVersion
                    val reqMode = mode
                    val reqQuality = videoQuality
                    val reqBitrate = audioBitrate
                    when (val result = DownloadEngine.resolve(url, reqMode, reqQuality, reqBitrate)) {
                        is FetchResult.Success -> {
                            if (saveVersion != settingsVersion) {
                                savingInFlight = false
                                return@launch
                            }
                            resolved = result.media
                            resolvedSettingsKey = currentSettingsKey()
                            handleDownload(result.media, false)
                        }
                        is FetchResult.Error -> {
                            savingInFlight = false
                            phase = DlPhase.Ready
                            snackbarHostState.showSnackbar(result.message)
                        }
                    }
                }
            }
        }
    }

    val drawerState = rememberDrawerState(DrawerValue.Closed)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MonoNavBar,
                drawerContentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(320.dp)
            ) {
                AboutDrawer(onClose = { scope.launch { drawerState.close() } })
            }
        }
    ) {
    Box(modifier.fillMaxSize()) {
        // subtle top fade — modern depth
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.045f), Color.Transparent)
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 10.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeHeader()
            Spacer(Modifier.weight(1f))
            GlassIconButton(
                icon = AppIcons.Settings,
                contentDescription = "About",
                onClick = { scope.launch { drawerState.open() } }
            )
        }

        GlassCard {
            SectionLabel("Paste link", icon = AppIcons.Link)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(MonoPanel)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        RoundedCornerShape(50)
                    )
                    .padding(start = 18.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = url,
                    onValueChange = {
                        url = it
                        settingsVersion++
                        if (phase != DlPhase.Idle) phase = DlPhase.Idle
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = {
                        Text(
                            "Enter Link Here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyMedium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { fetchMedia() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
                GlassIconButton(
                    icon = AppIcons.ContentPaste,
                    contentDescription = "Paste from clipboard",
                    onClick = pasteFromClipboard
                )
                Spacer(Modifier.width(8.dp))
                GlassIconButton(
                    icon = AppIcons.Search,
                    contentDescription = "Fetch media",
                    onClick = fetchMedia
                )
            }
        }

        AnimatedVisibility(
            visible = phase == DlPhase.Idle && resolvedUrl.isEmpty(),
            enter = fadeIn(spring()) + slideInVertically(initialOffsetY = { it / 3 }),
            exit = fadeOut()
        ) {
            EmptyStateCard()
        }

        AnimatedVisibility(
            visible = phase == DlPhase.Parsing,
            enter = fadeIn(spring()) + slideInVertically(initialOffsetY = { it / 3 }),
            exit = fadeOut()
        ) {
            ParsingCard()
        }

        val ready = phase == DlPhase.Ready || phase == DlPhase.Saving || phase == DlPhase.Saved
        AnimatedVisibility(
            visible = ready,
            enter = fadeIn(spring()) + slideInVertically(initialOffsetY = { it / 3 }),
            exit = fadeOut()
        ) {
            PreviewCard(platform = platformLabel(url), mode = mode, media = resolved)
        }

        AnimatedVisibility(
            visible = ready,
            enter = fadeIn(spring()) + slideInVertically(initialOffsetY = { it / 3 }),
            exit = fadeOut()
        ) {
            SettingsCard(
                platform = platformLabel(url),
                mode = mode,
                onModeChange = {
                    mode = it
                    settingsVersion++
                    phase = DlPhase.Ready
                },
                videoQuality = videoQuality,
                onVideoQualityChange = {
                    videoQuality = it
                    settingsVersion++
                    phase = DlPhase.Ready
                },
                audioBitrate = audioBitrate,
                onAudioBitrateChange = {
                    audioBitrate = it
                    settingsVersion++
                    phase = DlPhase.Ready
                },
                qualities = if (platformLabel(url) == "YouTube") availableQualities else emptyList(),
                fileName = resolved?.let { actualFileName(platformLabel(url), it) }
                    ?: previewFileName(platformLabel(url), mode, videoQuality, audioBitrate)
            )
        }

        AnimatedVisibility(
            visible = ready,
            enter = fadeIn(spring()) + slideInVertically(initialOffsetY = { it / 3 }),
            exit = fadeOut()
        ) {
            PrimaryButton(
                text = if (phase == DlPhase.Saved) "Open File" else "Save to Device",
                icon = if (phase == DlPhase.Saved) AppIcons.OpenInNew else AppIcons.Download,
                onClick = { if (phase == DlPhase.Saved) openSavedFile() else saveMedia() },
                enabled = !savingInFlight,
                loading = savingInFlight || phase == DlPhase.Saving,
                loadingText = if (progress > 0) "Saving… $progress%" else "Saving…",
                modifier = Modifier.fillMaxWidth()
            )
        }
        }
    }
    }
}

@Composable
private fun AboutDrawer(onClose: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MonoNavBar)
            .padding(horizontal = 22.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "About",
                style = MaterialTheme.typography.headlineSmall.copy(
                    brush = Brush.linearGradient(listOf(Color.White, Color(0xFF9A9A9A)))
                ),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.weight(1f))
            GlassIconButton(
                icon = AppIcons.Close,
                contentDescription = "Close",
                onClick = onClose
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .shadow(20.dp, RoundedCornerShape(24.dp), spotColor = Color.White.copy(alpha = 0.30f))
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, scheme.outline.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(R.drawable.saveora_logo),
                    contentDescription = "Savora Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                text = "Savora",
                style = MaterialTheme.typography.headlineSmall.copy(
                    brush = Brush.linearGradient(listOf(Color.White, Color(0xFF9A9A9A)))
                ),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelMedium,
                color = scheme.onSurface.copy(alpha = 0.5f),
                letterSpacing = 0.8.sp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.10f))
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Purpose", icon = AppIcons.Settings)
            Text(
                text = "This app is built purely for problem-solving and learning purposes. " +
                    "It is not intended for commercial distribution.",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Details", icon = AppIcons.YouTube)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.surface.copy(alpha = 0.10f))
                    .border(1.dp, scheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = AppIcons.YouTube,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = scheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "YouTube & Instagram downloads",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurface.copy(alpha = 0.8f)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.surface.copy(alpha = 0.10f))
                    .border(1.dp, scheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = AppIcons.Videocam,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = scheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Up to 4K quality, audio & video modes",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurface.copy(alpha = 0.8f)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.surface.copy(alpha = 0.10f))
                    .border(1.dp, scheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = AppIcons.Android,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = scheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Android 10 and above",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Text(
            text = "Made with Kotlin & Jetpack Compose",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun HomeHeader() {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .shadow(18.dp, RoundedCornerShape(17.dp), spotColor = Color.White.copy(alpha = 0.30f))
                .clip(RoundedCornerShape(17.dp))
                .border(1.dp, scheme.outline.copy(alpha = 0.8f), RoundedCornerShape(17.dp))
        ) {
            Image(
                painter = painterResource(R.drawable.saveora_logo),
                contentDescription = "Savora Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = "Savora",
                style = MaterialTheme.typography.headlineSmall.copy(
                    brush = Brush.linearGradient(listOf(Color.White, Color(0xFF9A9A9A)))
                ),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "Reels & Shorts Downloader",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurface.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyStateCard() {
    val scheme = MaterialTheme.colorScheme
    GlassCard {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(scheme.surface.copy(alpha = 0.14f))
                .border(1.dp, scheme.outline.copy(alpha = 0.5f), RoundedCornerShape(50))
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PulseDot()
            Spacer(Modifier.width(9.dp))
            Text(
                text = "Paste a link to begin",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = scheme.onSurface.copy(alpha = 0.8f)
            )
        }
        SectionLabel("How it works", icon = AppIcons.Link)
        StepRow(1, "Paste a link", "Instagram reel or YouTube video / Shorts")
        StepRow(2, "Fetch media", "All available formats detected instantly")
        StepRow(3, "Save", "Video, video-only or audio — to Downloads")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatBox("2", "Platforms", Modifier.weight(1f))
            StatBox("3", "Modes", Modifier.weight(1f))
            StatBox("100%", "Free", Modifier.weight(1f))
        }
    }
}

@Composable
private fun StepRow(number: Int, title: String, subtitle: String) {
    val scheme = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        GradNumberCircle(number)
        Spacer(Modifier.width(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun StatBox(value: String, label: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surface.copy(alpha = 0.10f))
            .border(1.dp, scheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                brush = Brush.linearGradient(listOf(Color.White, Color(0xFF9A9A9A)))
            ),
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.2.sp,
            color = scheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun ParsingCard() {
    val scheme = MaterialTheme.colorScheme
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp,
                color = scheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Parsing link…",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Detecting platform & available formats",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}

@Composable
private fun PreviewCard(platform: String, mode: MediaMode, media: MediaInfo?) {
    val scheme = MaterialTheme.colorScheme
    val platformIcon = when (platform) {
        "YouTube" -> AppIcons.YouTube
        "Instagram" -> AppIcons.Instagram
        else -> null
    }
    val title = media?.title?.takeIf { it.isNotBlank() }
        ?: if (platform == "Instagram") "Instagram Reel" else "Media found"
    val sizeText = media?.sizeBytes?.takeIf { it > 0 }?.let { "~${(it / 1024 / 1024).coerceAtLeast(1)} MB" }
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 84.dp, height = 96.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MonoPanel,
                                scheme.surface.copy(alpha = 0.06f)
                            )
                        )
                    )
                    .border(1.dp, scheme.outline.copy(alpha = 0.55f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(12.dp, RoundedCornerShape(50), spotColor = Color.White.copy(alpha = 0.4f))
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.linearGradient(listOf(Color.White, Color(0xFFCFCFCF)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = platformIcon ?: AppIcons.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp),
                        tint = Color(0xFF0A0A0A)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagPill(platform, icon = platformIcon)
                    TagPill(mode.label)
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(scheme.primary)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Ready • configure format below",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurface.copy(alpha = 0.55f)
                    )
                    if (sizeText != null) {
                        Spacer(Modifier.width(10.dp))
                        TagPill(sizeText)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsCard(
    platform: String,
    mode: MediaMode,
    onModeChange: (MediaMode) -> Unit,
    videoQuality: String,
    onVideoQualityChange: (String) -> Unit,
    audioBitrate: String,
    onAudioBitrateChange: (String) -> Unit,
    qualities: List<String>,
    fileName: String
) {
    val scheme = MaterialTheme.colorScheme
    GlassCard {
        SectionLabel("Download settings", icon = AppIcons.Settings)

        ModeSelector(selected = mode, onSelect = onModeChange)

        AnimatedContent(
            targetState = mode,
            transitionSpec = { fadeIn(spring()) togetherWith fadeOut(spring()) },
            label = "qualityOptions"
        ) { selected ->
            if (platform == "Instagram") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.surface.copy(alpha = 0.08f))
                        .border(1.dp, scheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.Check,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = scheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Best available format is selected automatically",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel(if (selected == MediaMode.AUDIO_ONLY) "Audio bitrate" else "Video quality")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val options = if (selected == MediaMode.AUDIO_ONLY) {
                            AudioBitrates
                        } else {
                            qualities.ifEmpty { VideoQualities }
                        }
                        val selectedValue = if (selected == MediaMode.AUDIO_ONLY) audioBitrate else videoQuality
                        options.forEach { option ->
                            QualityChip(
                                label = option,
                                selected = option == selectedValue,
                                onClick = {
                                    if (selected == MediaMode.AUDIO_ONLY) onAudioBitrateChange(option)
                                    else onVideoQualityChange(option)
                                }
                            )
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("Output file", icon = AppIcons.File)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(50))
                    .background(MonoPanel)
                    .border(1.dp, scheme.outline.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = AppIcons.File,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = scheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = scheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ModeSelector(selected: MediaMode, onSelect: (MediaMode) -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    val itemHeight = 52.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MonoPanel)
            .border(1.dp, scheme.outline.copy(alpha = 0.5f), shape)
    ) {
        val items = MediaMode.entries
        val itemWidth = maxWidth / items.size
        val selectedIndex = items.indexOf(selected)
        val pillX by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "pillX"
        )
        Box(
            modifier = Modifier
                .width(itemWidth)
                .height(itemHeight)
                .offset(x = pillX)
                .padding(5.dp)
                .shadow(14.dp, RoundedCornerShape(50), spotColor = Color.White.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(Color.White, Color(0xFF9A9A9A))))
        )
        Row(Modifier.fillMaxWidth()) {
            items.forEach { mode ->
                val isSelected = mode == selected
                val fg by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFF0A0A0A) else scheme.onSurface.copy(alpha = 0.55f),
                    animationSpec = spring(stiffness = 350f),
                    label = "modeFg"
                )
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .width(itemWidth)
                        .height(itemHeight)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onSelect(mode) }
                        )
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = mode.icon,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = fg
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = fg,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }
    }
}