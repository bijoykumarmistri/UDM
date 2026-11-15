package com.universal.dm.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium Apple-grade Floating Capsule Navigation Bar for UDM V2.
 *
 * Implements GPU-accelerated, high-refresh-rate 120Hz smooth page switching:
 * - Static screen positions: Each option has a fixed 1/3 slot width with zero horizontal jumping.
 * - Full touch targets: 100% of the dock surface is clickable with zero dead zones.
 * - Hardware-accelerated sliding indicator: Rendered via graphicsLayer translationX on GPU.
 * - Micro-interaction feedback: Low-latency spring response, tactile press bounce, and haptics.
 */
@Composable
fun UdmBottomNav(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Dock styling colors matching reference design, adapting to light and dark themes
    val dockBackground = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(Color(0xF809141D), Color(0xF8050C13))
        } else {
            listOf(Color(0xF2FFFFFF), Color(0xF2F0F5F8))
        }
    )
    val dockBorderColor = if (isDark) Color(0xFF142B39) else Color(0xFFD4E2EB)
    val activeBorderColor = if (isDark) Color(0xFF00C4D6) else MaterialTheme.colorScheme.primary
    val activeFillColor = if (isDark) Color(0x6600E5FF).copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val activeIconColor = if (isDark) Color(0xFF00E5FF) else MaterialTheme.colorScheme.primary
    val inactiveIconColor = if (isDark) Color(0xFF758E9E) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    val activeTextColor = if (isDark) Color(0xFFFFFFFF) else MaterialTheme.colorScheme.primary
    val shadowSpotColor = if (isDark) Color(0x99000000) else Color(0x22000000)
    val shadowAmbientColor = if (isDark) Color(0x3300C4D6) else Color(0x10006874)

    var dockWidthPx by remember { mutableIntStateOf(0) }
    val screens = Screen.entries
    val screenCount = screens.size
    val selectedIndex = screens.indexOf(currentScreen).coerceAtLeast(0)

    // Calculate slot width in pixels and Dp
    val slotWidthPx = if (dockWidthPx > 0) dockWidthPx.toFloat() / screenCount else 0f
    val slotWidthDp = with(density) {
        if (slotWidthPx > 0) slotWidthPx.toDp() else 0.dp
    }

    // High-precision GPU translation spring (buttery smooth 120Hz motion)
    val targetOffsetPx = slotWidthPx * selectedIndex
    val animatedOffsetPxState = animateFloatAsState(
        targetValue = targetOffsetPx,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = 420f
        ),
        label = "NavIndicatorTranslationX"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Floating Capsule Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    spotColor = shadowSpotColor,
                    ambientColor = shadowAmbientColor
                )
                .clip(CircleShape)
                .background(dockBackground)
                .border(
                    width = 1.dp,
                    color = dockBorderColor,
                    shape = CircleShape
                )
                .padding(all = 4.dp)
                .onSizeChanged {
                    if (it.width > 0) {
                        dockWidthPx = it.width
                    }
                }
        ) {
            // 1. Creative Sliding Glowing Active Indicator Pill (Rendered on GPU layer without layout passes)
            if (slotWidthPx > 0) {
                Box(
                    modifier = Modifier
                        .width(slotWidthDp)
                        .height(48.dp)
                        .graphicsLayer {
                            translationX = animatedOffsetPxState.value
                        }
                        .padding(horizontal = 3.dp, vertical = 2.dp)
                        .clip(CircleShape)
                        .background(activeFillColor)
                        .border(
                            width = 1.2.dp,
                            color = activeBorderColor,
                            shape = CircleShape
                        )
                )
            }

            // 2. Static Equal Slots Row - 100% full-width touch area for every option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                screens.forEach { screen ->
                    val isSelected = currentScreen == screen
                    StaticBottomNavItem(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        screen = screen,
                        isSelected = isSelected,
                        badgeCount = badgeCount,
                        activeIconColor = activeIconColor,
                        inactiveIconColor = inactiveIconColor,
                        activeTextColor = activeTextColor,
                        onSelect = {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onScreenSelected(screen)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StaticBottomNavItem(
    screen: Screen,
    isSelected: Boolean,
    badgeCount: Int,
    activeIconColor: Color,
    inactiveIconColor: Color,
    activeTextColor: Color,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Tactile press micro-interaction with quick feedback
    val pressScaleState = animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = if (isPressed) {
            tween(durationMillis = 60, easing = LinearOutSlowInEasing)
        } else {
            spring(dampingRatio = 0.72f, stiffness = 420f)
        },
        label = "PressScale"
    )

    // Snappy icon scale synchronized with active state
    val iconScaleState = animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 0.95f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 420f),
        label = "IconScale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) activeIconColor else inactiveIconColor,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "IconColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) activeTextColor else inactiveIconColor,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "TextColor"
    )

    // Entire slot is clickable - zero dead space, permanently fixed touch position
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onSelect()
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer {
                val s = pressScaleState.value
                scaleX = s
                scaleY = s
            }
        ) {
            if (screen.hasBadge && (badgeCount > 0)) {
                BadgedBox(
                    badge = {
                        Badge(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color(0xFF001F24)
                        ) {
                            Text(
                                text = badgeCount.toString(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = screen.title,
                        tint = iconColor,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer {
                                val s = iconScaleState.value
                                scaleX = s
                                scaleY = s
                            }
                    )
                }
            } else {
                Icon(
                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                    contentDescription = screen.title,
                    tint = iconColor,
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer {
                            val s = iconScaleState.value
                            scaleX = s
                            scaleY = s
                        }
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = screen.title,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.5.sp,
                letterSpacing = 0.2.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1415)
@Composable
fun UdmBottomNavBrowserPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        UdmBottomNav(
            currentScreen = Screen.Browser,
            onScreenSelected = {},
            badgeCount = 3
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1415)
@Composable
fun UdmBottomNavDownloadPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        UdmBottomNav(
            currentScreen = Screen.Downloads,
            onScreenSelected = {},
            badgeCount = 2
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0E1415)
@Composable
fun UdmBottomNavSettingsPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        UdmBottomNav(
            currentScreen = Screen.Settings,
            onScreenSelected = {},
            badgeCount = 0
        )
    }
}
