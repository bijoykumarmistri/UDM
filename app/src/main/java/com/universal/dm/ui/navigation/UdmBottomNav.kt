package com.universal.dm.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium Apple-grade Floating Capsule Navigation Bar for UDM V2.
 *
 * Implements the exact design shown in reference:
 * - Floating dark pill dock with subtle frosted glow and slate cyan hairline border.
 * - Active tab: Expanding pill indicator with vibrant cyan stroke, dark cyan translucent fill,
 *   bright cyan icon, and crisp typography.
 * - Inactive tabs: Minimalist muted slate icons without text.
 * - Fluid spring micro-interactions, scale bounce, and tactile haptic feedback.
 */
@Composable
fun UdmBottomNav(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    badgeCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Dock styling colors matching reference design
    val dockBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xF809141D),
            Color(0xF8050C13)
        )
    )
    val dockBorderColor = Color(0xFF142B39)
    val activeBorderColor = Color(0xFF00A8C6)
    val activeFillColor = Color(0x80072834)
    val activeIconColor = Color(0xFF00E5FF)
    val inactiveIconColor = Color(0xFF637C8E)
    val activeTextColor = Color(0xFFFFFFFF)

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
                .widthIn(max = 440.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = CircleShape,
                    spotColor = Color(0x99000000),
                    ambientColor = Color(0x3300C4D6)
                )
                .clip(CircleShape)
                .background(dockBackground)
                .border(
                    width = 1.dp,
                    color = dockBorderColor,
                    shape = CircleShape
                )
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Screen.entries.forEach { screen ->
                    val isSelected = currentScreen == screen
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    // Apple-style micro-bounce and press physics
                    val pressScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.92f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "PressScale"
                    )

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.06f else 0.98f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "IconScale"
                    )

                    val itemBorderColor by animateColorAsState(
                        targetValue = if (isSelected) activeBorderColor else Color.Transparent,
                        animationSpec = spring(dampingRatio = 0.85f),
                        label = "ItemBorderColor"
                    )

                    val itemBackgroundColor by animateColorAsState(
                        targetValue = if (isSelected) activeFillColor else Color.Transparent,
                        animationSpec = spring(dampingRatio = 0.85f),
                        label = "ItemBackgroundColor"
                    )

                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) activeIconColor else inactiveIconColor,
                        animationSpec = tween(durationMillis = 200),
                        label = "IconColor"
                    )

                    // Individual Tab Pill Container
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = pressScale
                                scaleY = pressScale
                            }
                            .clip(CircleShape)
                            .background(itemBackgroundColor)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.dp,
                                color = itemBorderColor,
                                shape = CircleShape
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                if (!isSelected) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onScreenSelected(screen)
                                }
                            }
                            .padding(
                                horizontal = if (isSelected) 16.dp else 14.dp,
                                vertical = 8.dp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (screen.hasBadge && badgeCount > 0 && !isSelected) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = Color(0xFF00E5FF),
                                            contentColor = Color(0xFF001F24)
                                        ) {
                                            Text(text = "$badgeCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = screen.unselectedIcon,
                                        contentDescription = screen.title,
                                        tint = iconColor,
                                        modifier = Modifier
                                            .size(21.dp)
                                            .graphicsLayer {
                                                scaleX = iconScale
                                                scaleY = iconScale
                                            }
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title,
                                    tint = iconColor,
                                    modifier = Modifier
                                        .size(21.dp)
                                        .graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        }
                                )
                            }

                            // Fluid expansion of label text only when selected
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = fadeIn(
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                ) + expandHorizontally(
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ),
                                exit = fadeOut(
                                    animationSpec = tween(durationMillis = 120)
                                ) + shrinkHorizontally(
                                    animationSpec = spring(
                                        dampingRatio = 0.82f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = screen.title,
                                        color = activeTextColor,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp,
                                        letterSpacing = 0.2.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip
                                    )

                                    if (screen.hasBadge && badgeCount > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(Color(0xFF00E5FF))
                                                .padding(horizontal = 6.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "$badgeCount",
                                                color = Color(0xFF001F24),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF0E1415)
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF0E1415)
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF0E1415)
@Composable
fun UdmBottomNavFinishedPreview() {
    Box(modifier = Modifier.padding(16.dp)) {
        UdmBottomNav(
            currentScreen = Screen.Finished,
            onScreenSelected = {},
            badgeCount = 0
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF0E1415)
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
