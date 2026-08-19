package com.nexora.savora.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.nexora.savora.R
import com.nexora.savora.ui.components.GlassCard
import com.nexora.savora.ui.components.GlassCircle
import com.nexora.savora.ui.components.PrimaryButton
import com.nexora.savora.ui.icons.AppIcons

private data class PermissionStep(
    val permission: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

private fun requiredSteps(): List<PermissionStep> = buildList {
    // Android 13+ (API 33+): Notification runtime permission — download progress ke liye.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(
            PermissionStep(
                permission = Manifest.permission.POST_NOTIFICATIONS,
                title = "Notifications",
                description = "Shows download progress while media is saving",
                icon = AppIcons.Notifications
            )
        )
    }

    // Android 10 (API 29) aur neeche: storage runtime permission zaroori hai.
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
        add(
            PermissionStep(
                permission = Manifest.permission.WRITE_EXTERNAL_STORAGE,
                title = "Storage",
                description = "Saves videos and audio to your device (Android 10)",
                icon = AppIcons.StorageDrive
            )
        )
    }
}

@Composable
fun PermissionScreen(onAllGranted: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scheme = MaterialTheme.colorScheme

    val steps = remember { requiredSteps() }
    val pending = remember {
        steps.filter {
            context.checkSelfPermission(it.permission) != PackageManager.PERMISSION_GRANTED
        }.toMutableStateList()
    }
    var currentStep by remember { mutableStateOf(pending.firstOrNull()) }
    var permanentlyDenied by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val prefs = context.getSharedPreferences("savora_settings", android.content.Context.MODE_PRIVATE)
            currentStep?.let { step ->
                prefs.edit().remove("perm_skipped_${step.permission}").apply()
            }
            val index = pending.indexOfFirst { it.permission == currentStep?.permission }
            if (index >= 0) pending.removeAt(index)
            permanentlyDenied = false
            currentStep = pending.firstOrNull()
            if (pending.isEmpty()) onAllGranted()
        } else {
            permanentlyDenied = currentStep?.let { step ->
                activity == null || !ActivityCompat.shouldShowRequestPermissionRationale(activity, step.permission)
            } ?: false
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        // Resume par re-check — Settings se grant karke wapas aane par screen khud update ho jaye.
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val stillPending = steps.filter {
                context.checkSelfPermission(it.permission) != PackageManager.PERMISSION_GRANTED
            }
            pending.clear()
            pending.addAll(stillPending)
            permanentlyDenied = false
            currentStep = pending.firstOrNull()
            if (pending.isEmpty()) onAllGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .shadow(16.dp, RoundedCornerShape(26.dp), spotColor = Color.White.copy(alpha = 0.10f))
                .clip(RoundedCornerShape(26.dp))
                .background(scheme.surface.copy(alpha = 0.10f))
                .border(1.dp, scheme.outline.copy(alpha = 0.7f), RoundedCornerShape(26.dp))
        ) {
            Image(
                painter = painterResource(R.drawable.saveora_logo),
                contentDescription = "Savora Logo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = "Savora",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "One-time setup",
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurface.copy(alpha = 0.55f)
        )
        Spacer(Modifier.height(28.dp))

        GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 26.dp) {
            currentStep?.let { step ->
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        steps.forEachIndexed { index, _ ->
                            val done = index < steps.size - pending.size
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (done) scheme.primary
                                        else scheme.onSurface.copy(alpha = 0.15f)
                                    )
                            )
                            if (index < steps.size - 1) Spacer(Modifier.width(8.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "${steps.size - pending.size + 1} of ${steps.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurface.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(Modifier.height(6.dp))
                    GlassCircle(icon = step.icon, size = 68.dp, iconSize = 30.dp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurface.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))

                    if (permanentlyDenied) {
                        Text(
                            text = "Permission is blocked. Enable it from Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurface.copy(alpha = 0.55f),
                            textAlign = TextAlign.Center
                        )
                        PrimaryButton(
                            text = "Open Settings",
                            icon = AppIcons.Settings,
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        PrimaryButton(
                            text = "Allow ${step.title}",
                            icon = step.icon,
                            onClick = { launcher.launch(step.permission) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextButton(
                            onClick = {
                                // Skip — lekin next launch par phir pucho (agar abhi bhi denied hai).
                                val prefs = context.getSharedPreferences(
                                    "savora_settings", android.content.Context.MODE_PRIVATE
                                )
                                pending.forEach { s ->
                                    prefs.edit().putBoolean("perm_skipped_${s.permission}", true).apply()
                                }
                                prefs.edit().putBoolean("permission_setup_done", true).apply()
                                onAllGranted()
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                "Skip for now",
                                style = MaterialTheme.typography.labelMedium,
                                color = scheme.onSurface.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "Permissions are used only to download and save media you choose",
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurface.copy(alpha = 0.4f),
            textAlign = TextAlign.Center
        )
    }
}