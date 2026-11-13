package com.universal.dm.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.universal.dm.R
import com.universal.dm.core.AppMetadata
import com.universal.dm.ui.theme.CodecAv1Badge
import com.universal.dm.ui.theme.CodecM4aBadge
import com.universal.dm.ui.theme.UDMTheme

/**
 * World-Class Brand Logo Composable for Universal Download Manager (UDM).
 * Features subtle ambient back-glow, glassmorphic container, and vector rendering.
 */
@Composable
fun UdmBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    showGlow: Boolean = true,
    animateGlow: Boolean = false
) {
    val scale = if (animateGlow) {
        val infiniteTransition = rememberInfiniteTransition(label = "logoGlowTransition")
        val animatedScale by infiniteTransition.animateFloat(
            initialValue = 0.98f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        animatedScale
    } else {
        1f
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        if (showGlow) {
            // Ambient soft multi-layer radial aura
            Box(
                modifier = Modifier
                    .size(size * 1.15f)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                CodecM4aBadge.copy(alpha = 0.35f),
                                CodecAv1Badge.copy(alpha = 0.20f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Vector Logo Asset
        Image(
            painter = painterResource(id = R.drawable.ic_udm_logo),
            contentDescription = AppMetadata.APP_NAME,
            modifier = Modifier.size(size)
        )
    }
}

/**
 * Premium Hero Header with Logo, Title, and Developer Tagline
 */
@Composable
fun UdmHeroHeader(
    modifier: Modifier = Modifier,
    logoSize: Dp = 72.dp
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.75f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            UdmBrandLogo(
                size = logoSize,
                showGlow = true,
                animateGlow = false
            )

            Column(verticalArrangement = Arrangement.Center) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = AppMetadata.APP_SHORT_NAME,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "v${AppMetadata.APP_VERSION}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = AppMetadata.APP_NAME,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Crafted by ${AppMetadata.DEVELOPER_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(name = "UDM Brand Logo - Dark", showBackground = true, backgroundColor = 0xFF0B132B)
@Composable
fun UdmBrandLogoPreviewDark() {
    UDMTheme(darkTheme = true) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            UdmBrandLogo(size = 96.dp, showGlow = true)
            Spacer(modifier = Modifier.height(8.dp))
            UdmHeroHeader()
        }
    }
}

@Preview(name = "UDM Brand Logo - Light", showBackground = true, backgroundColor = 0xFFF0F4F8)
@Composable
fun UdmBrandLogoPreviewLight() {
    UDMTheme(darkTheme = false) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            UdmBrandLogo(size = 96.dp, showGlow = true)
            Spacer(modifier = Modifier.height(8.dp))
            UdmHeroHeader()
        }
    }
}
