/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.productux

import android.content.Context
import androidx.media3.common.PlaybackException
import com.metrolist.music.R

/**
 * Maps technical failures to friendly copy — mirrors desktop `user-error-messages.ts`.
 */
object UserFacingErrors {
  fun playbackMessage(context: Context, error: PlaybackException): String {
    playbackMessageFromCause(context, error.cause)?.let { return it }
    return when (error.errorCode) {
      PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
      PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
      PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
      PlaybackException.ERROR_CODE_REMOTE_ERROR ->
          context.getString(R.string.product_ux_error_playback_source_blocked)
      PlaybackException.ERROR_CODE_TIMEOUT ->
          context.getString(R.string.product_ux_error_playback_stream_expired)
      PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND ->
          context.getString(R.string.product_ux_error_playback_unavailable)
      else -> context.getString(R.string.product_ux_error_playback_generic)
    }
  }

  fun playbackMessageFromCause(context: Context, cause: Throwable?): String? {
    val raw = cause?.message?.lowercase().orEmpty()
    if (raw.isEmpty()) return null
    return when {
      raw.contains("403") || raw.contains("blocked") ->
          context.getString(R.string.product_ux_error_playback_source_blocked)
      raw.contains("expired") || raw.contains("token") ->
          context.getString(R.string.product_ux_error_playback_stream_expired)
      raw.contains("sign in") || raw.contains("login") ->
          context.getString(R.string.product_ux_error_playback_sign_in)
      raw.contains("unavailable") || raw.contains("not found") ->
          context.getString(R.string.product_ux_error_playback_unavailable)
      else -> null
    }
  }

  fun importMessage(context: Context): String =
      context.getString(R.string.product_ux_error_import_failed)

  fun libraryMessage(context: Context): String =
      context.getString(R.string.product_ux_error_library_unreachable)

  fun searchMessage(context: Context): String =
      context.getString(R.string.product_ux_error_search_unavailable)

  fun desktopImportMessage(context: Context, cause: Throwable?): String {
    val raw = cause?.message?.lowercase().orEmpty()
    return when {
      raw.contains("configure") || raw.contains("endpoint") || raw.isBlank() ->
          context.getString(R.string.desktop_import_not_configured)
      else -> context.getString(R.string.desktop_import_failed)
    }
  }
}
