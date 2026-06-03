/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.net.Uri
import timber.log.Timber

/**
 * Release-visible playback diagnostics.
 *
 * Every line is emitted through Timber (a [Timber.DebugTree] is planted unconditionally in
 * `App.onCreate`, so these survive in release builds) under a single tag so a failing release can
 * be captured with:
 *
 *     adb logcat -s PlaybackDiag:V
 *
 * Stream URLs are logged host+path only — the query string carries `pot`, `sig` and other tokens
 * we don't want in logs. Use [host] / [hostPath] to sanitise before logging.
 */
object PlaybackDiagnostics {
    const val TAG = "PlaybackDiag"

    fun i(message: String) = Timber.tag(TAG).i(message)

    fun w(message: String) = Timber.tag(TAG).w(message)

    fun e(message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Timber.tag(TAG).e(throwable, message)
        } else {
            Timber.tag(TAG).e(message)
        }
    }

    /** Host only, e.g. `rr3---sn-abc.googlevideo.com`. */
    fun host(url: String?): String =
        runCatching { Uri.parse(url).host ?: "?" }.getOrDefault("?")

    /** Host + path, query stripped (query carries pot/sig tokens). */
    fun hostPath(url: String?): String =
        runCatching {
            val uri = Uri.parse(url)
            "${uri.host ?: "?"}${uri.path ?: ""}"
        }.getOrDefault("?")
}
