/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.screens.Screens
import com.metrolist.music.ui.theme.RetroTokens

@Stable
private fun isRouteSelected(currentRoute: String?, screenRoute: String, navigationItems: List<Screens>): Boolean {
    if (currentRoute == null) return false
    if (currentRoute == screenRoute) return true
    if (navigationItems.any { it.route == screenRoute } &&
        currentRoute.startsWith("$screenRoute/")) return true

    // Fix: match the route template, not the resolved route
    if (screenRoute == "search_input" &&
        (currentRoute.startsWith("search/") || currentRoute == "search/{query}")) return true

    if (screenRoute == "now_playing_tab") return false

    return false
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppNavigationRail(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null
) {
    val containerColor = if (pureBlack) Color.Black else RetroTokens.Background2
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .width(84.dp)
            .fillMaxHeight()
            .background(containerColor)
            .border(1.dp, RetroTokens.Border),
    ) {
        Spacer(modifier = Modifier.weight(1f))

        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val currentIsSelected by rememberUpdatedState(isSelected)
            val iconRes = remember(isSelected, screen) {
                if (isSelected) screen.iconIdActive else screen.iconIdInactive
            }

            val isSearchItem = screen == Screens.Search && onSearchLongClick != null
            val interactionSource = remember { MutableInteractionSource() }

            RetroNavItem(
                screen = screen,
                selected = isSelected,
                iconRes = iconRes,
                showLabel = false,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                interactionSource = interactionSource,
                onClick = { onItemClick(screen, currentIsSelected) },
                onLongClick = if (isSearchItem) {
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSearchLongClick?.invoke()
                    }
                } else {
                    null
                },
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    onItemClick: (Screens, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    pureBlack: Boolean = false,
    slimNav: Boolean = false,
    onSearchLongClick: (() -> Unit)? = null
) {
    val containerColor = if (pureBlack) Color.Black else RetroTokens.Background2
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .background(containerColor)
            .border(1.dp, RetroTokens.Border)
            .padding(bottom = bottomInset),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigationItems.forEach { screen ->
            val isSelected = remember(currentRoute, screen.route) {
                isRouteSelected(currentRoute, screen.route, navigationItems)
            }
            val currentIsSelected by rememberUpdatedState(isSelected)
            val iconRes = remember(isSelected, screen) {
                if (isSelected) screen.iconIdActive else screen.iconIdInactive
            }

            val isSearchItem = screen == Screens.Search && onSearchLongClick != null
            val interactionSource = remember { MutableInteractionSource() }

            RetroNavItem(
                screen = screen,
                selected = isSelected,
                iconRes = iconRes,
                showLabel = !slimNav,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                interactionSource = interactionSource,
                onClick = { onItemClick(screen, currentIsSelected) },
                onLongClick = if (isSearchItem) {
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSearchLongClick?.invoke()
                    }
                } else {
                    null
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RetroNavItem(
    screen: Screens,
    selected: Boolean,
    iconRes: Int,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val pressed by interactionSource.collectIsPressedAsState()
    val color = if (selected) RetroTokens.Active else RetroTokens.TextSoft
    Column(
        modifier = modifier
            .background(
                when {
                    selected -> RetroTokens.Panel2
                    pressed -> RetroTokens.Panel
                    else -> Color.Transparent
                },
            )
            .border(1.dp, if (selected || pressed) RetroTokens.BorderBright else RetroTokens.BorderMuted)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = stringResource(screen.titleId),
            tint = color,
            modifier = Modifier.size(26.dp),
        )
        if (showLabel) {
            Text(
                text = stringResource(screen.titleId).uppercase(),
                color = color,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
