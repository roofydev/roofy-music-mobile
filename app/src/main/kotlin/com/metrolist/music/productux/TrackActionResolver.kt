/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.productux

/**
 * Canonical track menu actions — mirrors desktop `track-action-resolver.ts`
 * and product-ux-integration-plan.md §12.
 */
enum class TrackActionId {
    PLAY,
    PLAY_NEXT,
    ADD_TO_QUEUE,
    ADD_TO_PLAYLIST,
    SAVE_OFFLINE,
    ADD_TO_LIBRARY,
}

enum class TrackItemState {
    ONLINE,
    IN_LIBRARY,
    OFFLINE,
    SAVING,
}

object TrackActionResolver {
  val actionOrder: List<TrackActionId> =
      listOf(
          TrackActionId.PLAY,
          TrackActionId.PLAY_NEXT,
          TrackActionId.ADD_TO_QUEUE,
          TrackActionId.ADD_TO_PLAYLIST,
          TrackActionId.SAVE_OFFLINE,
          TrackActionId.ADD_TO_LIBRARY,
      )

  fun resolveForYouTubeTrack(
      isPersonalLibraryTrack: Boolean,
      isSaving: Boolean,
      hasVideo: Boolean,
  ): List<TrackActionId> {
    val base =
        mutableListOf(
            TrackActionId.PLAY,
            TrackActionId.PLAY_NEXT,
            TrackActionId.ADD_TO_QUEUE,
            TrackActionId.ADD_TO_PLAYLIST,
        )
    if (isSaving) return base
    if (isPersonalLibraryTrack) {
      base.add(TrackActionId.SAVE_OFFLINE)
      return base
    }
    // Streaming: offline cache (Exo) and permanent library (desktop import) are separate actions.
    base.add(TrackActionId.SAVE_OFFLINE)
    base.add(TrackActionId.ADD_TO_LIBRARY)
    return base
  }

  fun shouldShowSaveOffline(actions: List<TrackActionId>): Boolean =
      TrackActionId.SAVE_OFFLINE in actions

  fun shouldShowAddToLibrary(actions: List<TrackActionId>): Boolean =
      TrackActionId.ADD_TO_LIBRARY in actions
}
