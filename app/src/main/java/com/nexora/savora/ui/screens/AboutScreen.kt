package com.nexora.savora.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexora.savora.BuildConfig
import com.nexora.savora.R
import com.nexora.savora.ui.components.GlassIconButton
import com.nexora.savora.ui.components.SectionLabel
import com.nexora.savora.ui.icons.AppIcons

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                icon = AppIcons.ArrowBack,
                contentDescription = "Back",
                onClick = onBack
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = "About",
                style = MaterialTheme.typography.headlineSmall.copy(
                    brush = Brush.linearGradient(listOf(Color.White, Color(0xFF9A9A9A)))
                ),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(Modifier.height(28.dp))

        Column(
            modifier = Modifier.widthIn(max = 600.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .shadow(22.dp, RoundedCornerShape(26.dp), spotColor = Color.White.copy(alpha = 0.30f))
                        .clip(RoundedCornerShape(26.dp))
                        .border(1.dp, scheme.outline.copy(alpha = 0.8f), RoundedCornerShape(26.dp))
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
                    style = MaterialTheme.typography.headlineMedium.copy(
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
                DetailRow(AppIcons.YouTube, "YouTube & Instagram downloads")
                DetailRow(AppIcons.Videocam, "Up to 4K quality, audio & video modes")
                DetailRow(AppIcons.Android, "Android 10 and above")
                DetailRow(AppIcons.Download, "Files saved to Download/Savora")
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Made with Kotlin & Jetpack Compose",
                style = MaterialTheme.typography.labelSmall,
                color = scheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.surface.copy(alpha = 0.10f))
            .border(1.dp, scheme.outline.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = scheme.onSurface.copy(alpha = 0.7f)
        )
        Spacer(Modifier.width(11.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onSurface.copy(alpha = 0.8f)
        )
    }
}