# AGENTS.md

## Product Intent

Roofy Music Mobile is the Android half of a unified Roofy Music product. It is a GPLv3 Android YouTube Music client forked from MetroList. It keeps streaming, caching, search, lyrics, playlists, account sync, widgets, Android Auto, listen-together, recognition, desktop pairing, and compact Retro Monochrome styling.

The mobile app must feel like the same product as Roofy Music Desktop: premium, intuitive, Retro Monochrome, music-first, and powerful without exposing technical complexity by default.

Keep downloader and cache behavior user-directed. Do not add telemetry.

## Required UX Documents

Before any UI, UX, navigation, styling, settings, player, search, library, Android Auto, widget, or route work, read:

- `PRODUCT_UX_SPEC.md`
- `docs/UX_AUDIT_UNIFIED_ROOFY_MUSIC.md`
- `docs/ROOFY_MUSIC_DESIGN_PHILOSOPHY.md`
- `docs/SHARED_DESIGN_SYSTEM.md`
- `docs/MOBILE_UX_UI_SPEC.md`

If you change UI behavior or information architecture, update the relevant doc in the same change.

## Product Quality Bar

Treat Roofy Music as a consumer music app with Apple/Google-level polish:

- Main UI must be clear to non-technical users.
- Playback, search, library, downloads/offline, Android Auto, and settings must be easy to understand.
- Advanced functionality stays available but belongs in Settings, Advanced, overflow menus, or contextual flows.
- Do not add visible complexity without a strong user-facing reason.
- Do not create orphaned routes, hidden screens, or one-off components.
- Every new UI state needs default, pressed, focused, selected, disabled, loading, empty, and error behavior where relevant.

## Retro Monochrome Direction

Preserve the retro CRT design direction, but keep it mature:

- Near-black surfaces, square shapes, gray borders, monospace typography, restrained accents, subtle scanline/noise treatment.
- Retro effects must not reduce readability or touch clarity.
- Avoid generic gradients, colorful SaaS styling, or unrelated design languages.
- Keep accents rare and purposeful.
- Mobile can use stronger monospace and sharper shapes than desktop, but shared token roles must remain aligned.

## Unified Desktop/Mobile Rules

Use consistent product language across repos:

- Home
- Search
- Library
- Now Playing
- Settings
- Downloads
- Imports
- Listen Together / Together
- Devices and Integrations
- Advanced

Mobile can use bottom navigation, sheets, Android Auto, widgets, notifications, and system intents. Desktop can use sidebar, tables, hover controls, command palette, and resizable panels. Do not force identical layouts, but keep labels, mental models, and player anatomy aligned.

## Current Architecture

- `app/` is the Android application.
- Kotlin package names currently remain `com.metrolist.music` to avoid a risky namespace rewrite.
- Application id is `app.roofymusic.mobile`.
- `MainActivity.kt` owns the single-activity Compose shell, nav host, top bar, player sheet, and global overlays.
- `ui/screens/NavigationBuilder.kt` owns route registration.
- `ui/screens/Screens.kt` and `ui/component/AppNavigation.kt` own top-level navigation.
- `ui/player/**` owns mini player, expanded player, queue, and playback error UI.
- `ui/screens/search/**`, `ui/screens/library/**`, and `ui/screens/settings/**` own major product flows.
- `ui/theme/RetroTheme.kt`, `Theme.kt`, and `Type.kt` own the mobile visual system.
- `playback/MusicService.kt` and `MediaLibrarySessionCallback.kt` own playback service and Android Auto browse behavior.
- `innertube/`, `kugou/`, `lrclib/`, `betterlyrics/`, `lastfm/`, `discordrpc/`, `shazamkit/`, and `paxsenix/` are supporting modules.
- `metroproto/` is a required source dependency copied from MetroList's submodule.

## Known UX Debt

Documented in `docs/UX_AUDIT_UNIFIED_ROOFY_MUSIC.md` and `docs/MOBILE_UX_UI_SPEC.md`:

- Listen Together is hard to discover from primary navigation.
- `settings/integrations` hub is registered but not clearly reachable.
- `ExploreScreen` appears unused.
- Library active-tab behavior may reset unexpectedly.
- Account route and account dialog duplicate mental models.
- Retro controls can be below 48dp and often suppress visual indication.
- Muted gray contrast needs an accessibility pass.
- Android Auto real-car visibility depends on build/distribution and should be tested.

Do not paper over these with more top-bar icons or menu items. Resolve through information architecture and progressive disclosure.

## UI Change Rules

- Prefer existing Compose patterns and shared components before adding new variants.
- Do not add one-off styling unless it becomes a documented design-system variant.
- Do not put technical source, stream, cache, or debug controls in primary UI.
- Use bottom sheets for contextual actions, not complex settings pages.
- Icon-only controls need content descriptions.
- Primary touch targets should be at least 48dp.
- Empty states must explain the state and offer one useful next action.
- Playback and network errors must be friendly first and technical only behind disclosure.
- Android Auto browse trees must be simple, stable, and non-technical.

## Development Rules

- Preserve GPLv3 license notices and source availability obligations.
- Brand visible UI as Roofy Music, not MetroList.
- Prefer narrow changes over broad package renames unless the user explicitly asks for a namespace migration.
- If changing UI/UX, update `PRODUCT_UX_SPEC.md` or the docs in `docs/` as needed.

## Verification

Build with Java 21:

```powershell
.\gradlew.bat :app:assembleFossDebug
```

Expected APK:

```text
app/build/outputs/apk/foss/debug/app-foss-debug.apk
```

