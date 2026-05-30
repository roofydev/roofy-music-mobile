/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.productux

import android.content.Context
import com.metrolist.music.R

/**
 * Maps technical failures to friendly copy — mirrors desktop `user-error-messages.ts`.
 */
object UserFacingErrors {
  fun playbackMessage(context: Context, cause: Throwable?): String {
    val raw = cause?.message?.lowercase().orEmpty()
    return when {
      raw.contains("403") || raw.contains("blocked") ->
          context.getString(R.string.product_ux_error_playback_source_blocked)
      raw.contains("expired") || raw.contains("token") ->
          context.getString(R.string.product_ux_error_playback_stream_expired)
      raw.contains("sign in") || raw.contains("login") ->
          context.getString(R.string.product_ux_error_playback_sign_in)
      raw.contains("unavailable") || raw.contains("not found") ->
          context.getString(R.string.product_ux_error_playback_unavailable)
      else -> context.getString(R.string.product_ux_error_playback_generic)
    }
  }

  fun importMessage(context: Context): String =
      context.getString(R.string.product_ux_error_import_failed)

  fun libraryMessage(context: Context): String =
      context.getString(R.string.product_ux_error_library_unreachable)

  fun desktopImportMessage(context: Context, cause: Throwable?): String {
    val raw = cause?.message?.lowercase().orEmpty()
    return when {
      raw.contains("configure") || raw.contains("endpoint") || raw.isBlank() ->
          context.getString(R.string.desktop_import_not_configured)
      else -> importMessage(context)
    }
  }
}
