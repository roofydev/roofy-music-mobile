/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.PlaybackException
import com.metrolist.music.R
import com.metrolist.music.productux.UserFacingErrors
import com.metrolist.music.ui.theme.RetroButton
import com.metrolist.music.ui.theme.RetroTokens

@Composable
fun PlaybackError(
    error: PlaybackException,
    retry: () -> Unit,
) {
    val context = LocalContext.current
    var showTechnicalDetails by remember { mutableStateOf(false) }

    val rawErrorMessage =
        error.cause?.cause?.message
            ?: error.cause?.message
            ?: error.message
            ?: stringResource(R.string.error_unknown)

    val ageRestrictedSignals =
        listOf(
            "age-restricted",
            "age restricted",
            "AGE_CHECK_REQUIRED",
            "AGE_VERIFICATION_REQUIRED",
            "CONTENT_CHECK_REQUIRED",
            "Sign in to confirm your age",
            "confirm your age",
        )
    val isAgeRestricted = ageRestrictedSignals.any { rawErrorMessage.contains(it, ignoreCase = true) }

    val friendlyMessage =
        if (isAgeRestricted) {
            stringResource(R.string.product_ux_error_playback_age_restricted)
        } else {
            UserFacingErrors.playbackMessage(context, error)
        }

    val technicalSummary =
        buildString {
            append(rawErrorMessage)
            append('\n')
            append(
                stringResource(
                    R.string.product_ux_error_playback_code,
                    getErrorCodeName(error.errorCode),
                    error.errorCode,
                ),
            )
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.error),
            contentDescription = null,
            tint = RetroTokens.Text,
            modifier = Modifier.size(48.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = friendlyMessage,
            style = MaterialTheme.typography.titleMedium,
            color = RetroTokens.Text,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text =
                if (showTechnicalDetails) {
                    stringResource(R.string.product_ux_hide_technical_details)
                } else {
                    stringResource(R.string.product_ux_view_technical_details)
                },
            style = MaterialTheme.typography.labelMedium,
            color = RetroTokens.TextSoft,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable { showTechnicalDetails = !showTechnicalDetails },
        )

        AnimatedVisibility(visible = showTechnicalDetails) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = technicalSummary,
                    style =
                        MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                        ),
                    color = RetroTokens.TextMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        RetroButton(onClick = retry) {
            Icon(
                painter = painterResource(R.drawable.replay),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = stringResource(R.string.retry))
        }
    }
}

/**
 * Get human-readable error code name from PlaybackException error code
 */
private fun getErrorCodeName(errorCode: Int): String {
    return when (errorCode) {
        PlaybackException.ERROR_CODE_UNSPECIFIED -> "UNSPECIFIED"
        PlaybackException.ERROR_CODE_REMOTE_ERROR -> "REMOTE_ERROR"
        PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> "BEHIND_LIVE_WINDOW"
        PlaybackException.ERROR_CODE_TIMEOUT -> "TIMEOUT"
        PlaybackException.ERROR_CODE_FAILED_RUNTIME_CHECK -> "FAILED_RUNTIME_CHECK"
        PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> "IO_UNSPECIFIED"
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "IO_NETWORK_CONNECTION_FAILED"
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "IO_NETWORK_CONNECTION_TIMEOUT"
        PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE -> "IO_INVALID_HTTP_CONTENT_TYPE"
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "IO_BAD_HTTP_STATUS"
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "IO_FILE_NOT_FOUND"
        PlaybackException.ERROR_CODE_IO_NO_PERMISSION -> "IO_NO_PERMISSION"
        PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> "IO_CLEARTEXT_NOT_PERMITTED"
        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE -> "IO_READ_POSITION_OUT_OF_RANGE"
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> "PARSING_CONTAINER_MALFORMED"
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED -> "PARSING_MANIFEST_MALFORMED"
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> "PARSING_CONTAINER_UNSUPPORTED"
        PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED -> "PARSING_MANIFEST_UNSUPPORTED"
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "DECODER_INIT_FAILED"
        PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED -> "DECODER_QUERY_FAILED"
        PlaybackException.ERROR_CODE_DECODING_FAILED -> "DECODING_FAILED"
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES -> "DECODING_FORMAT_EXCEEDS_CAPABILITIES"
        PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> "DECODING_FORMAT_UNSUPPORTED"
        PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED -> "AUDIO_TRACK_INIT_FAILED"
        PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED -> "AUDIO_TRACK_WRITE_FAILED"
        PlaybackException.ERROR_CODE_DRM_UNSPECIFIED -> "DRM_UNSPECIFIED"
        PlaybackException.ERROR_CODE_DRM_SCHEME_UNSUPPORTED -> "DRM_SCHEME_UNSUPPORTED"
        PlaybackException.ERROR_CODE_DRM_PROVISIONING_FAILED -> "DRM_PROVISIONING_FAILED"
        PlaybackException.ERROR_CODE_DRM_CONTENT_ERROR -> "DRM_CONTENT_ERROR"
        PlaybackException.ERROR_CODE_DRM_LICENSE_ACQUISITION_FAILED -> "DRM_LICENSE_ACQUISITION_FAILED"
        PlaybackException.ERROR_CODE_DRM_DISALLOWED_OPERATION -> "DRM_DISALLOWED_OPERATION"
        PlaybackException.ERROR_CODE_DRM_SYSTEM_ERROR -> "DRM_SYSTEM_ERROR"
        PlaybackException.ERROR_CODE_DRM_DEVICE_REVOKED -> "DRM_DEVICE_REVOKED"
        PlaybackException.ERROR_CODE_DRM_LICENSE_EXPIRED -> "DRM_LICENSE_EXPIRED"
        else -> "UNKNOWN_ERROR_$errorCode"
    }
}
