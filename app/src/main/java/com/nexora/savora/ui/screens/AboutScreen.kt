package com.nexora.savora.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.10f))
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SectionLabel("Developer", icon = AppIcons.Person, modifier = Modifier.fillMaxWidth())
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .shadow(20.dp, CircleShape, spotColor = Color.White.copy(alpha = 0.30f))
                        .clip(CircleShape)
                        .border(1.5.dp, scheme.outline.copy(alpha = 0.8f), CircleShape)
                ) {
                    Image(
                        painter = painterResource(R.drawable.developer),
                        contentDescription = "Kunal Sharma",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Text(
                    text = "Kunal Sharma",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        brush = Brush.linearGradient(listOf(Color.White, Color(0xFF9A9A9A)))
                    ),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Android developer crafting clean, minimal apps with Kotlin & Jetpack Compose. " +
                        "Savora is built as a hands-on problem-solving project — learn, build, and ship.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SocialButton(
                        icon = AppIcons.GitHub,
                        brandColor = Color.White,
                        contentDescription = "GitHub",
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/kunalsharma0354"))
                            )
                        }
                    )
                    Spacer(Modifier.width(16.dp))
                    SocialButton(
                        icon = AppIcons.Discord,
                        brandColor = Color(0xFF5865F2),
                        contentDescription = "Discord",
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.gg/VM6JNZrWTQ"))
                            )
                        }
                    )
                    Spacer(Modifier.width(16.dp))
                    SocialButton(
                        icon = AppIcons.Gmail,
                        brandColor = Color(0xFFEA4335),
                        contentDescription = "Gmail",
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:Kunalsharma9321@gmail.com")
                            }
                            context.startActivity(intent)
                        }
                    )
                }
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

@Composable
private fun SocialButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    brandColor: Color,
    contentDescription: String,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(scheme.surface.copy(alpha = 0.15f))
            .border(1.dp, scheme.outline.copy(alpha = 0.5f), CircleShape)
            .shadow(10.dp, CircleShape, spotColor = brandColor.copy(alpha = 0.35f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp),
            tint = brandColor
        )
    }
}