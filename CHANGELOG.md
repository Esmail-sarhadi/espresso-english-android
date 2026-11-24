# Changelog

All notable changes to this project are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [5.4.1] — 2026-11-24

### Fixed
- The floating bubble was invisible on Android 11+ because overlay windows were added through the
  service's own `WindowManager` (not a visual context). It now derives a display context and a
  `TYPE_APPLICATION_OVERLAY` window context, which is the documented path.

### Added
- **Drag to delete**: while dragging the bubble a red ✕ target fades in at the bottom of the
  screen; dropping the bubble on it removes the bubble and stops the service. The ✕ target grows
  and turns solid red when the bubble hovers over it.
- The lyrics sheet is wrapped in a **rotating rainbow frame with spectrum bars** on all four
  edges that pulse with the track.

## [5.4.0] — 2026-11-24

### Added
- **Floating translate bubble** — a draggable bubble that sits over any app; tap to OCR the
  screen and translate it in an overlay panel. Drag it onto the red ✕ target at the bottom to
  remove it.
- **RGB equaliser frame** around the lyrics sheet: a rotating rainbow border with spectrum bars
  that dance with the track.
- **Onboarding tour** — a four-page Persian walkthrough with an animated aurora backdrop, shown
  once on first launch and replayable from Settings.
- Store-ready marketing site (`site/`), launch content (`marketing/`) and a publishing playbook
  (`docs/PUBLISHING-FA.md`).

### Changed
- **New visual identity**: "midnight espresso" palette (violet-charcoal canvas, amber primary,
  teal/violet/coral/sky secondaries) with gradient helpers; light theme rebuilt as warm cream.
- **App name is now English** — *Espresso English* in the launcher, splash, header and media
  metadata.
- Lyrics are **left-aligned for English and right-aligned for Persian** instead of inheriting the
  RTL container direction.
- Lyric highlight now leads the audio by 350 ms, with «زودتر / دیرتر» drift controls (±250 ms).
- Translation engine rewritten around Google's `gtx` endpoint: a whole song in one or two
  requests instead of one per line, and **no more 24-line cap**.

### Fixed
- `ForegroundServiceDidNotStartInTimeException` crash on screen scan: `Activity.RESULT_OK` is
  `-1`, which collided with the "missing extra" default, so `startForeground()` was never called.
  The service now promotes itself to foreground before any validation.
- Song search reported "no song found" when the catalogue was merely unreachable; errors are now
  surfaced honestly and three catalogues are queried in parallel.
- OCR noise (clock digits, battery percentages, single letters) is filtered out.

## [5.3.0] — 2026-11-24
### Added
- Karaoke-style lyric following driven by the live media-session position.
### Fixed
- Screen capture crash; LRCLIB search parameter (`q`, not `query_string`).

## [5.2.0] — 2026-11-24
### Added
- Multi-catalogue lyrics search (LRCLIB · iTunes · Deezer) with `lyrics.ovh` fallback and a
  connectivity diagnostic in Settings.
- Screenshot-import and paste-text fallbacks for the screen translator.

## [5.0.0] — 2026-11-23
### Added
- 208 lessons, bundled neural TTS, offline playback, spaced repetition, pronunciation scoring.
