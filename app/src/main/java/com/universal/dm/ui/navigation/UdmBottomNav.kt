package com.universal.dm.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.universal.dm.ui.theme.UDMTheme

/**
 * Clean Material 3 Expressive Navigation Bar with tonal surface,
 * crisp active pill indicator, and unobtrusive active download badge.
 */
@Composable
fun UdmBottomNav(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0
) {
    val haptic = LocalHapticFeedback.current
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = modifier) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
        NavigationBar(
            containerColor = colorScheme.surfaceContainer,
            tonalElevation = 0.dp
        ) {
            Screen.entries.forEach { screen ->
                val isSelected = currentScreen == screen
                NavigationBarItem(
                    selected = isSelected,
                    onClick = {
                        if (!isSelected) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onScreenSelected(screen)
                        }
                    },
                    icon = {
                        val iconVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon
                        if (screen.hasBadge && badgeCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(
                                        containerColor = colorScheme.primary,
                                        contentColor = colorScheme.onPrimary
                                    ) {
                                        Text(
                                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = screen.title
                                )
                            }
                        } else {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = screen.title
                            )
                        }
                    },
                    label = {
                        Text(
                            text = screen.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            ),
                            maxLines = 1
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colorScheme.onPrimaryContainer,
                        selectedTextColor = colorScheme.onSurface,
                        indicatorColor = colorScheme.primaryContainer,
                        unselectedIconColor = colorScheme.onSurfaceVariant,
                        unselectedTextColor = colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Preview(name = "Bottom Nav - Dark", showBackground = true)
@Composable
fun UdmBottomNavDarkPreview() {
    UDMTheme(darkTheme = true, dynamicColor = false) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(top = 16.dp)) {
            UdmBottomNav(
                currentScreen = Screen.Downloads,
                onScreenSelected = {},
                badgeCount = 3
            )
        }
    }
}

@Preview(name = "Bottom Nav - Light", showBackground = true)
@Composable
fun UdmBottomNavLightPreview() {
    UDMTheme(darkTheme = false, dynamicColor = false) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background).padding(top = 16.dp)) {
            UdmBottomNav(
                currentScreen = Screen.Downloads,
                onScreenSelected = {},
                badgeCount = 3
            )
        }
    }
}
