/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

enum class PlaybackVariant {
    AUDIO,
    VIDEO,
}

private const val VIDEO_CACHE_KEY_PREFIX = "video:"

fun playbackVariantCacheKey(
    mediaId: String,
    variant: PlaybackVariant,
): String =
    when (variant) {
        PlaybackVariant.AUDIO -> mediaId
        PlaybackVariant.VIDEO -> "$VIDEO_CACHE_KEY_PREFIX$mediaId"
    }

fun playbackVariantFromCacheKey(cacheKey: String): PlaybackVariant =
    if (cacheKey.startsWith(VIDEO_CACHE_KEY_PREFIX)) {
        PlaybackVariant.VIDEO
    } else {
        PlaybackVariant.AUDIO
    }

fun sourceMediaIdFromCacheKey(cacheKey: String): String =
    cacheKey.removePrefix(VIDEO_CACHE_KEY_PREFIX)
