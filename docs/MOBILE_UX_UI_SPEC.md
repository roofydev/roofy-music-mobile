# Mobile UX/UI Specification: Roofy Music Mobile

Repository: `C:\roofy-music-projects\roofy-music-mobile`

This document supplements `PRODUCT_UX_SPEC.md` and is the working spec for redesigning the Android app as part of one Roofy Music product.

## Current Mobile App Structure

Primary implementation:
- App shell: `app/src/main/kotlin/com/metrolist/music/MainActivity.kt`.
- Route registration: `ui/screens/NavigationBuilder.kt`.
- Top-level nav: `ui/screens/Screens.kt`, `ui/component/AppNavigation.kt`.
- Player: `ui/player/Player.kt`, `MiniPlayer.kt`, `Queue.kt`, `PlaybackError.kt`.
- Search: `ui/screens/search/SearchScreen.kt`, `OnlineSearchScreen.kt`, `OnlineSearchResult.kt`, `LocalSearchScreen.kt`.
- Library: `ui/screens/library/**`, `ui/component/Library.kt`, `LibrarySearchHeader.kt`.
- Settings: `ui/screens/settings/**`.
- Menus/sheets: `ui/menu/**`, `ui/component/BottomSheetMenu.kt`, `BottomSheetPage.kt`.
- Theme: `ui/theme/RetroTheme.kt`, `Theme.kt`, `Type.kt`.
- Android Auto: `playback/MediaLibrarySessionCallback.kt`, `AndroidManifest.xml`, `res/xml/automotive_app_desc.xml`, `AndroidAutoSettings.kt`.

## Current Main Screens

| Screen / Route | Purpose | Current quality | Redesign direction |
| --- | --- | --- | --- |
| Home `home` | Discovery, personalized shelves, contextual Together entry | Strong, with a calmer Together surface after the first polish pass. | Continue reducing persistent top-bar actions and keep contextual entries lightweight. |
| Search `search_input`, `search/{query}` | Search and URL handling | Useful, but local/online split may confuse. | Group results in plain language and avoid focus/player conflict. |
| Library `library` | Saved/local music categories | Broad and useful; active tab taps now keep context. | Make Downloads/Saved/Playlists hierarchy clearer. |
| Player sheet | Playback, lyrics, queue | Mature and central. | Align with desktop player anatomy; keep advanced controls in overflow. |
| Settings `settings/**` | Preferences/integrations | Re-grouped into shared settings IA with reachable Integrations and Account settings. | Continue moving rare technical controls into Advanced. |
| Listen Together | Social listening | Contextual Home entry and Devices & Integrations entry; not a fifth bottom tab. | Keep contextual unless product maturity justifies primary navigation. |
| Recognition | Music recognition | Useful but partly hidden. | Visible search affordance plus widget/quick settings; long-press only as shortcut. |
| Android Auto | Car browse/playback | Manifest and service present. | Test real car/release path; keep car settings plain and minimal. |
| Wrapped | Seasonal story | Distinct but can feel separate. | Keep seasonal, ensure visual system still feels Roofy. |
| Equalizer/EQ wizard | Audio tuning | Power feature. | Player overflow and Settings Playback entry; wizard CTA in EQ screen. |

## Current User Flows

### Play from search

Current:
- Search accepts plain text or YouTube URLs.
- Video URL can immediately call `playQueue`.
- Results route has filters.

Problems:
- URL parsing is helpful but invisible.
- Local results above online results can read as two different search systems.

Required direction:
- Use result grouping labels: Local library, YouTube Music, Songs, Albums, Artists, Playlists.
- Keep URL paste as a smart behavior with friendly feedback if unsupported.

### Play from library

Current:
- Library has Mix, playlists, songs, albums, artists, podcasts.
- Multiple auto/cache/top playlist routes exist.

Problems:
- Some destinations are more implementation categories than user categories.
- Downloads/offline hierarchy still needs clearer user-facing grouping.

Required direction:
- Keep tab behavior stable.
- Make Downloads/Offline a clear library section.
- Hide cache implementation labels from primary IA where possible.

### Player and queue

Current:
- Mini player plus expanded bottom-sheet player.
- Queue is a bottom-sheet sub-page.
- Playback errors use friendly copy with technical disclosure.
- Eligible YouTube tracks can switch between Audio and Video inside the expanded player without leaving the queue, using compact headphones/video icon controls near the media surface.

Problems:
- Sheet layering can hide where the user is.
- Technical actions in player menus can grow.

Required direction:
- Primary controls remain stable in mini and expanded states.
- Audio/Video remains a one-tap player mode; video must render in the regular player, open fullscreen as a top-level video overlay independent of the mini-player sheet, preserve queue/position, hide stale loading overlays once playback is visible, place fullscreen exit in the bottom-right control row, and fall back to audio with friendly copy when unavailable.
- Save offline asks for Audio only or Audio + video instead of hiding the storage tradeoff.
- Queue, lyrics, devices, and overflow use consistent icon placement.
- Debug/source details stay behind disclosure or Advanced.

### Settings and integrations

Current:
- Settings uses shared groups: Account, Playback, Library and Downloads, Appearance, Devices and Integrations, Privacy, Advanced, About.
- `settings/integrations` is reachable from Settings.
- Account preferences live at `settings/account`; the older `account` route is treated as YouTube Library content.

Problems:
- Users must know where a feature lives.
- Advanced integrations compete with ordinary playback/settings.

Required direction:
- Keep advanced integrations from competing with ordinary playback/settings.
- Preserve the account preference vs account library distinction in labels and routes.
- Keep integration hub reachable.

### Android Auto

Current:
- `MusicService` exports Media3 media library/media browser actions.
- `automotive_app_desc.xml` declares media support.
- Manifest includes car app metadata and tintable attribution icon.

Problems:
- Real cars filter debug/sideloaded apps unless Android Auto unknown sources is enabled or the app is distributed through trusted testing.
- Car UX quality depends on media library hierarchy, not phone UI.

Required direction:
- Keep Android Auto browse tree simple: Recently played, Playlists, Albums, Artists, Downloads, Search/voice where supported.
- Test release/internal-sharing builds in a real car.

## Mobile-Specific Interaction Rules

- Minimum target size is 48dp for primary controls.
- Do not rely only on long-press.
- Bottom sheets are for contextual actions, not complex settings.
- Back behavior must be predictable: collapse player, close sheet, then navigate.
- Search keyboard focus must not fight player expansion.
- Android Auto and widgets must use plain labels and stable actions.

## Layout Recommendations

- Keep portrait bottom navigation.
- Keep landscape rail, but mirror bottom nav exactly.
- Reduce top bar clutter. Move history/stats/settings/account into consistent menu or appropriate tabs if needed.
- Player should reserve stable space and avoid shifting navigation unexpectedly.
- Lists should use consistent artwork, title, subtitle, trailing action pattern.

## Component-Level Recommendations

| Component | Issue | Recommendation |
| --- | --- | --- |
| `RetroButton`, `RetroIconButton` | First polish pass added 48dp targets and pressed feedback. | Continue TalkBack/contrast verification on real devices. |
| `AppNavigation` | Now Playing opens sheet, Together remains contextual. | Keep Play tab behavior clear; do not add Together as a fifth tab unless product maturity changes. |
| `SearchScreen` | Aggressive focus and mixed local/online model. | Stabilize focus; label result groups. |
| `LibraryScreen` | Active tab reset fixed. | Re-tap should remain stable or scroll to top, not reset. |
| `Player.kt` / `MiniPlayer.kt` | Needs cross-platform anatomy alignment. | Standardize player control clusters. |
| `PlaybackError` | Good progressive disclosure. | Continue this pattern for all technical errors. |
| `SettingsScreen` | Re-grouped under shared settings model. | Keep rare technical controls in Advanced. |
| `BottomSheetMenu` | Can absorb too many actions. | Prioritize contextual actions, move rare actions deeper. |

## What Must Stay Consistent With Desktop

- Product terms and settings groups.
- Player anatomy and queue model.
- Retro Monochrome token roles.
- Friendly error/empty/loading copy.
- Advanced-feature disclosure rules.
- Downloads/offline naming.
- Listen Together/Together naming.

## What May Differ From Desktop

- Bottom navigation and sheets.
- Android widgets, Android Auto, notifications, system intents, quick settings.
- Touch-first lists rather than dense tables.
- Route depth should be shallower than desktop.
- Long-running background work uses Android notifications.

## Known Mobile UX Debt

| Issue | Severity | Fix path |
| --- | --- | --- |
| Listen Together not in primary nav | Addressed | Contextual Home entry and Devices & Integrations entry; still not a fifth tab. |
| Widget search/library shortcut bug noted in spec | Addressed | `ACTION_SEARCH` maps to Search; `ACTION_LIBRARY` maps to Library. |
| `settings/integrations` hub unreachable | Addressed | Linked from Settings under Devices & Integrations. |
| `ExploreScreen` unused | Addressed | Removed. |
| Active library tab resets to Mix | Addressed | Active tab taps no-op instead of resetting. |
| Low contrast muted text | Partially addressed | Muted token raised; still needs runtime contrast/TalkBack validation. |
| Retro controls lack indication | Partially addressed | Shared retro controls and nav rows now show pressed feedback; broader component audit remains. |
| Account route/dialog duplication | Addressed | Account preferences moved to `settings/account`; `account` route is YouTube Library. |
| Android Auto visibility depends on build/distribution | Medium | Document debug testing and verify release path. |

## Implementation Notes

- Navigation changes start in `Screens.kt`, `NavigationBuilder.kt`, `MainActivity.kt`, and `AppNavigation.kt`.
- Player changes start in `ui/player/**` and `playback/PlayerConnection.kt`.
- Settings IA changes start in `ui/screens/settings/**`.
- Retro token changes start in `ui/theme/RetroTheme.kt`, `Theme.kt`, and `Type.kt`.
- Android Auto changes must consider `MediaLibrarySessionCallback.kt`, `MusicService.kt`, manifest metadata, and real-device testing.
- Update `PRODUCT_UX_SPEC.md` and this document after route or screen behavior changes.
