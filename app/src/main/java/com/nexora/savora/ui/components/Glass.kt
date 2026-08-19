package com.nexora.savora.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexora.savora.ui.icons.AppIcons
import com.nexora.savora.ui.theme.MonoCardBottom
import com.nexora.savora.ui.theme.MonoCardTop

private val Ink = Color(0xFF0A0A0A)
private val GradEnd = Color(0xFF9A9A9A)
private val GradSoftEnd = Color(0xFFCFCFCF)

@Composable
fun GlowBackground(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(modifier.background(scheme.background)) {
        Box(
            Modifier
                .fillMaxWidth(0.8f)
                .height(280.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-140).dp)
                .background(
                    Brush.radialGradient(
                        listOf(scheme.primary.copy(alpha = 0.06f), Color.Transparent)
                    )
                )
        )
        Box(
            Modifier
                .size(320.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 120.dp, y = 60.dp)
                .background(
                    Brush.radialGradient(
                        listOf(scheme.primary.copy(alpha = 0.035f), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    contentPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Column(
        modifier = modifier
            .shadow(22.dp, shape, spotColor = Color.Black.copy(alpha = 0.8f))
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(MonoCardTop, MonoCardBottom)
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.09f))
                ),
                shape
            )
            .drawWithContent {
                drawContent()
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.34f),
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.34f)
                        )
                    ),
                    start = Offset(22.dp.toPx(), 1.dp.toPx()),
                    end = Offset(size.width - 22.dp.toPx(), 1.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

@Composable
fun SectionLabel(title: String, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val color = scheme.onSurface.copy(alpha = 0.5f)
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color
            )
            Spacer(Modifier.width(7.dp))
        }
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp,
            color = color
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingText: String = "Saving…",
    success: Boolean = false,
    successText: String = "Saved"
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "btnScale"
    )
    val fg = Ink
    Box(
        modifier = modifier
            .height(56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(24.dp, shape, spotColor = Color.White.copy(alpha = if (enabled) 0.26f else 0f))
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color.White, GradEnd)
                )
            )
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            success -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AppIcons.Check, null, Modifier.size(20.dp), tint = fg)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = successText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = fg
                    )
                }
            }
            loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = fg
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = loadingText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = fg
                    )
                }
            }
            else -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (icon != null) {
                        Icon(icon, null, Modifier.size(20.dp), tint = fg)
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = fg
                    )
                }
            }
        }
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "iconScale"
    )
    Box(
        modifier = modifier
            .size(46.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(scheme.surface.copy(alpha = 0.14f))
            .border(1.dp, scheme.outline.copy(alpha = 0.6f), shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, Modifier.size(20.dp), tint = scheme.onSurface)
    }
}

@Composable
fun QualityChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(50)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else if (selected) 1.03f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "chipScale"
    )
    val bg by animateColorAsState(
        targetValue = if (selected) Color.White else scheme.surface.copy(alpha = 0.10f),
        animationSpec = spring(stiffness = 300f),
        label = "chipBg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) Ink else scheme.onSurface.copy(alpha = 0.8f),
        animationSpec = spring(stiffness = 300f),
        label = "chipFg"
    )
    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (selected) 16.dp else 0.dp, shape, spotColor = Color.White.copy(alpha = 0.35f))
            .clip(shape)
            .background(bg)
            .border(
                1.dp,
                if (selected) Color.Transparent else scheme.outline.copy(alpha = 0.5f),
                shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selected) {
                Icon(
                    imageVector = AppIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = fg
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = fg
            )
        }
    }
}

@Composable
fun GlassCircle(icon: ImageVector, size: Dp = 44.dp, iconSize: Dp = 20.dp, tint: Color? = null) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(scheme.surface.copy(alpha = 0.10f))
            .border(1.dp, scheme.outline.copy(alpha = 0.55f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            Modifier.size(iconSize),
            tint = tint ?: scheme.onSurface.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun TagPill(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(scheme.surface.copy(alpha = 0.14f))
            .border(1.dp, scheme.outline.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(11.dp),
                    tint = scheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface.copy(alpha = 0.8f),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
fun PulseDot(modifier: Modifier = Modifier, size: Dp = 7.dp) {
    val transition = rememberInfiniteTransition(label = "dotPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    Box(
        modifier
            .size(size)
            .shadow(8.dp, CircleShape, spotColor = Color.White.copy(alpha = 0.7f))
            .clip(CircleShape)
            .background(Color.White.copy(alpha = alpha))
    )
}

@Composable
fun GradNumberCircle(number: Int, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(12.dp, CircleShape, spotColor = Color.White.copy(alpha = 0.35f))
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color.White, GradEnd))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$number",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Ink
        )
    }
}