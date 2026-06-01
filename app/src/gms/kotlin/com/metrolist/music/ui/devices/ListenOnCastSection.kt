/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.devices

import android.widget.Toast
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.framework.CastContext
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.EnableGoogleCastKey
import com.metrolist.music.ui.component.CastPickerSheet
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.utils.rememberPreference
import timber.log.Timber

@Composable
fun ColumnScope.ListenOnCastSection(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val menuState = LocalMenuState.current

    var castAvailable by remember { mutableStateOf(false) }
    var mediaRouter by remember { mutableStateOf<MediaRouter?>(null) }
    var routeSelector by remember { mutableStateOf<MediaRouteSelector?>(null) }
    var availableRoutes by remember { mutableStateOf<List<MediaRouter.RouteInfo>>(emptyList()) }

    val (enableGoogleCast) = rememberPreference(key = EnableGoogleCastKey, defaultValue = true)

    val castHandler = playerConnection?.service?.castConnectionHandler
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(false) }
    val isConnecting by castHandler?.isConnecting?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(false) }
    val castDeviceName by castHandler?.castDeviceName?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf<String?>(null) }

    val currentMetadata by playerConnection?.mediaMetadata?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(null) }

    LaunchedEffect(enableGoogleCast) {
        if (!enableGoogleCast) {
            if (isCasting) {
                playerConnection?.service?.castConnectionHandler?.disconnect()
            }
            castAvailable = false
            mediaRouter = null
            routeSelector = null
            availableRoutes = emptyList()
            return@LaunchedEffect
        }
        try {
            CastContext.getSharedInstance(context)
            mediaRouter = MediaRouter.getInstance(context)
            routeSelector =
                MediaRouteSelector.Builder()
                    .addControlCategory(
                        CastMediaControlIntent.categoryForCast(
                            CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID,
                        ),
                    )
                    .build()
            playerConnection?.service?.castConnectionHandler?.initialize()
            castAvailable = true
        } catch (e: Exception) {
            Timber.d("Cast not available: ${e.message}")
            castAvailable = false
        }
    }

    DisposableEffect(mediaRouter, routeSelector) {
        val callback =
            object : MediaRouter.Callback() {
                override fun onRouteAdded(router: MediaRouter, route: MediaRouter.RouteInfo) {
                    updateCastRoutes(router, routeSelector) { availableRoutes = it }
                }

                override fun onRouteRemoved(router: MediaRouter, route: MediaRouter.RouteInfo) {
                    updateCastRoutes(router, routeSelector) { availableRoutes = it }
                }

                override fun onRouteChanged(router: MediaRouter, route: MediaRouter.RouteInfo) {
                    updateCastRoutes(router, routeSelector) { availableRoutes = it }
                }
            }

        routeSelector?.let { selector ->
            mediaRouter?.addCallback(selector, callback, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY)
            updateCastRoutes(mediaRouter, selector) { availableRoutes = it }
        }

        onDispose {
            mediaRouter?.removeCallback(callback)
        }
    }

    if (!enableGoogleCast || !castAvailable) {
        return
    }

    Spacer(modifier = Modifier.height(8.dp))

    val subtitle =
        when {
            isConnecting -> stringResource(R.string.devices_cast_connecting)
            isCasting && castDeviceName != null ->
                stringResource(R.string.devices_cast_playing_here, castDeviceName!!)
            availableRoutes.isEmpty() -> stringResource(R.string.devices_cast_no_devices)
            else -> stringResource(R.string.devices_cast_pick_device)
        }

    ListenOnDeviceRow(
        title = stringResource(R.string.devices_speakers_and_tvs),
        subtitle = subtitle,
        iconRes = if (isCasting) R.drawable.cast_connected else R.drawable.cast,
        isActive = isCasting,
        enabled = !isConnecting,
        onClick = {
            if (currentMetadata == null && !isCasting) {
                Toast.makeText(context, R.string.devices_cast_play_song_first, Toast.LENGTH_SHORT)
                    .show()
                return@ListenOnDeviceRow
            }

            val currentRoute =
                if (isCasting) {
                    mediaRouter?.routes?.find { route ->
                        routeSelector?.let { selector ->
                            route.matchesSelector(selector) && route.isSelected
                        } == true
                    }
                } else {
                    null
                }

            menuState.show {
                CastPickerSheet(
                    routes = availableRoutes,
                    isConnecting = isConnecting,
                    currentlyConnectedRoute = currentRoute,
                    onRouteSelected = { route ->
                        castHandler?.connectToRoute(route)
                        menuState.dismiss()
                        onDismiss()
                    },
                    onDisconnect = {
                        castHandler?.disconnect()
                        menuState.dismiss()
                    },
                )
            }
        },
    )
}

private fun updateCastRoutes(
    router: MediaRouter?,
    selector: MediaRouteSelector?,
    onUpdate: (List<MediaRouter.RouteInfo>) -> Unit,
) {
    if (router == null || selector == null) {
        onUpdate(emptyList())
        return
    }
    onUpdate(
        router.routes.filter { route ->
            route.matchesSelector(selector) && !route.isDefault
        },
    )
}
