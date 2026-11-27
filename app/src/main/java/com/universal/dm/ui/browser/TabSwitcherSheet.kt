package com.universal.dm.ui.browser

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universal.dm.ui.components.UdmIconSquircle
import com.universal.dm.ui.components.UdmStatusPill
import com.universal.dm.ui.theme.UDMTheme

/**
 * M3 Expressive Tab Switcher Modal Bottom Sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabSwitcherSheet(
    tabs: List<BrowserTab>,
    activeTabId: String,
    onDismiss: () -> Unit,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onOpenNewTab: (Boolean) -> Unit,
    onCloseAllTabs: (Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialCategory = remember(tabs, activeTabId) {
        if (tabs.find { it.id == activeTabId }?.isIncognito == true) 1 else 0
    }
    var selectedCategory by remember { mutableIntStateOf(initialCategory) }
    val colorScheme = MaterialTheme.colorScheme

    val filteredTabs = tabs.filter { if (selectedCategory == 0) !it.isIncognito else it.isIncognito }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UdmIconSquircle(
                        icon = Icons.Default.Tab,
                        containerColor = colorScheme.primaryContainer,
                        contentColor = colorScheme.onPrimaryContainer,
                        size = 40.dp,
                        iconSize = 20.dp,
                        cornerRadius = 13.dp
                    )
                    Column {
                        Text(
                            text = "Open Tabs (${tabs.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Tap a card to switch, or open a parallel session",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (filteredTabs.isNotEmpty()) {
                        Surface(
                            onClick = { onCloseAllTabs(selectedCategory == 1) },
                            shape = CircleShape,
                            color = colorScheme.errorContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Close All Tabs",
                                    tint = colorScheme.onErrorContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onDismiss,
                        shape = CircleShape,
                        color = colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Sheet",
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            PrimaryTabRow(
                selectedTabIndex = selectedCategory,
                modifier = Modifier.padding(horizontal = 16.dp),
                containerColor = Color.Transparent,
                divider = { HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.45f)) }
            ) {
                Tab(
                    selected = selectedCategory == 0,
                    onClick = { selectedCategory = 0 },
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Standard (${tabs.count { !it.isIncognito }})")
                        }
                    }
                )

                Tab(
                    selected = selectedCategory == 1,
                    onClick = { selectedCategory = 1 },
                    text = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.VpnLock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Incognito (${tabs.count { it.isIncognito }})")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (filteredTabs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        UdmIconSquircle(
                            icon = if (selectedCategory == 0) Icons.Default.Language else Icons.Default.VpnLock,
                            containerColor = colorScheme.surfaceContainerHigh,
                            contentColor = colorScheme.onSurfaceVariant,
                            size = 56.dp,
                            iconSize = 26.dp,
                            cornerRadius = 18.dp
                        )
                        Text(
                            text = if (selectedCategory == 0) "No standard tabs open" else "No incognito tabs open",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTabs, key = { it.id }) { tab ->
                        val isActive = tab.id == activeTabId
                        Card(
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .aspectRatio(1.22f)
                                .clickable { onSelectTab(tab.id) },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (tab.isIncognito) {
                                    if (isActive) colorScheme.tertiaryContainer.copy(alpha = 0.55f) else colorScheme.surfaceContainer
                                } else {
                                    if (isActive) colorScheme.primaryContainer.copy(alpha = 0.55f) else colorScheme.surfaceContainer
                                }
                            ),
                            border = if (isActive) {
                                BorderStroke(
                                    1.5.dp,
                                    if (tab.isIncognito) colorScheme.tertiary else colorScheme.primary
                                )
                            } else null
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (tab.isIncognito) Icons.Default.VpnLock else if (tab.url != null) Icons.Default.Lock else Icons.Default.Language,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (tab.isIncognito) colorScheme.tertiary else colorScheme.primary
                                        )
                                        Text(
                                            text = tab.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    IconButton(
                                        onClick = { onCloseTab(tab.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close Tab",
                                            modifier = Modifier.size(14.dp),
                                            tint = colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = tab.url ?: "Start Page (Speed Dial)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.5.sp,
                                        color = colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (tab.blockedAdsCount > 0) {
                                        UdmStatusPill(
                                            text = "${tab.blockedAdsCount} blocked",
                                            containerColor = colorScheme.surfaceContainerHigh,
                                            contentColor = colorScheme.primary,
                                            icon = Icons.Default.Shield
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { onOpenNewTab(selectedCategory == 1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedCategory == 1) "New Incognito Tab" else "New Standard Tab",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TabSwitcherSheetPreview() {
    val sampleTabs = listOf(
        BrowserTab(id = "1", title = "YouTube - Watch", url = "https://m.youtube.com/watch?v=123", blockedAdsCount = 14),
        BrowserTab(id = "2", title = "Vimeo Stream", url = "https://vimeo.com/456", blockedAdsCount = 3),
        BrowserTab(id = "3", title = "Private Search", url = "https://duckduckgo.com", isIncognito = true)
    )
    UDMTheme(darkTheme = true) {
        TabSwitcherSheet(
            tabs = sampleTabs,
            activeTabId = "1",
            onDismiss = {},
            onSelectTab = {},
            onCloseTab = {},
            onOpenNewTab = {},
            onCloseAllTabs = {}
        )
    }
}
