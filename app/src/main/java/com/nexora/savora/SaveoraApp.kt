package com.nexora.savora

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nexora.savora.ui.components.GlowBackground
import com.nexora.savora.ui.screens.AboutScreen
import com.nexora.savora.ui.screens.HomeScreen
import com.nexora.savora.ui.screens.PermissionScreen
import com.nexora.savora.ui.theme.MonoSnackBar
import com.nexora.savora.ui.theme.SavoraTheme

@Composable
fun SaveoraApp() {
    SavoraTheme(darkTheme = true) {
        val context = LocalContext.current
        val neededPermissions = remember {
            buildList {
                // Android 13+ (API 33+): Notifications runtime permission.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
                // Android 10 (API 29) aur neeche: storage runtime permission.
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
        val alreadyGranted = remember(neededPermissions) {
            neededPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        }
        val prefs = remember {
            context.getSharedPreferences("savora_settings", Context.MODE_PRIVATE)
        }
        val setupDone = remember {
            prefs.getBoolean("permission_setup_done", false)
        }
        // Setup done hone ke baad bhi: skip ki gayi permissions (notification/storage) dobara pucho,
        // jab tak grant na ho — app data clear ki zaroorat nahi.
        val shouldAsk = !alreadyGranted && (
            !setupDone || neededPermissions.any { perm ->
                prefs.getBoolean("perm_skipped_$perm", false) &&
                    ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
            }
            )
        var showPermissionFlow by rememberSaveable { mutableStateOf(shouldAsk) }

        if (showPermissionFlow) {
            PermissionScreen(onAllGranted = {
                prefs.edit().putBoolean("permission_setup_done", true).apply()
                showPermissionFlow = false
            })
        } else {
            MainContent()
        }
    }
}

@Composable
private fun MainContent() {
    val snackbarHostState = remember { SnackbarHostState() }
    val scheme = MaterialTheme.colorScheme
    var showAbout by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        containerColor = scheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    modifier = Modifier.border(
                        1.dp,
                        Color.White.copy(alpha = 0.16f),
                        RoundedCornerShape(14.dp)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    containerColor = MonoSnackBar,
                    contentColor = scheme.onSurface,
                    actionColor = scheme.primary
                )
            }
        }
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(scheme.background)
                .statusBarsPadding()
                .padding(innerPadding)
        ) {
            GlowBackground(Modifier.fillMaxSize())
            if (showAbout) {
                AboutScreen(onBack = { showAbout = false })
            } else {
                HomeScreen(
                    snackbarHostState = snackbarHostState,
                    onOpenAbout = { showAbout = true }
                )
            }
        }
    }
}