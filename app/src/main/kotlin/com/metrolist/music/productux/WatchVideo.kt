/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.productux

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.net.toUri
import androidx.navigation.NavController
import com.metrolist.music.R

object WatchVideo {
    private const val PREFS_NAME = "roofy_product_ux"
    private const val ROADMAP_HINT_SHOWN = "video_roadmap_hint_shown"

    fun videoUrlForTrack(
        trackId: String,
        shareLink: String = "",
    ): String =
        shareLink.ifBlank { "https://music.youtube.com/watch?v=$trackId" }

    fun open(
        context: Context,
        navController: NavController,
        videoUrl: String,
        showRoadmapHint: Boolean = true,
    ) {
        if (showRoadmapHint) {
            maybeShowRoadmapHint(context)
        }
        val encoded = Uri.encode(videoUrl)
        navController.navigate("video_watch?url=$encoded") {
            launchSingleTop = true
        }
    }

    fun openExternal(context: Context, videoUrl: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, videoUrl.toUri()))
    }

    private fun maybeShowRoadmapHint(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(ROADMAP_HINT_SHOWN, false)) return
        prefs.edit().putBoolean(ROADMAP_HINT_SHOWN, true).apply()
        Toast
            .makeText(
                context,
                context.getString(R.string.product_ux_video_mobile_roadmap),
                Toast.LENGTH_LONG,
            )
            .show()
    }
}
