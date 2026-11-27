# GANASUNO 14.0 — Batch 4 follow-up + Batch 5

Base: 13.0. Fifteen existing partial catalogue entries upgraded. Nine promoted to source implementation; six still partial. Source status: 104 complete, 46 partial, 50 not implemented. These are not runtime-verified counts.

## Batch 4 follow-up
- #34, #130, #162: online profile lookup, opt-in media cache, user-confirmed online release years.
- #47 and #91: remote waveform attempt and bounded search response remain partial.

## Batch 5
- #8, #25, #84, #105, #131, #132: video playback mode, adaptive RMS normalization, podcast RSS, integrated podcast library, combined archive, scheduled audio archive.
- #58, #60, #121, #199 remain partial; provider metadata, verified alternate versions, hosted playlist URLs and official blog URLs remain incomplete.

## Where to find the new tools
Settings → More features (v5–14) → Batch 4–5 tools. AutoEq online search is in 10-band software EQ. Track menu includes Play video, Find release year online and Online stream info. Library includes Podcast RSS. Audio and video settings retains Normalize volume. Waveform style offers explicit online analysis for non-live tracks. Storage → Thumbnail storage and cache → Playback media cache. Automatic backups includes optional audio archives.

Network features are opt-in where indicated and depend on available provider data, rights, and endpoints. Public AutoEq GitHub and MusicBrainz integrations are not live-tested; read their documentation: https://github.com/jaakkopasanen/AutoEq and https://musicbrainz.org/doc/MusicBrainz_API. Podcast feeds require public direct HTTPS audio the user has permission to save. Transient media cache is off by default and uses resolved URLs as keys, so expiry can reduce cache reuse; no live caching or offline playback guarantee. The media cache can be cleared separately. RMS is not LUFS normalization. External URI grants, secrets, linked external audio and account data are not backed up. Provider data and API availability are not verified. The existing unofficial extractor is unchanged. No official app blog URL was invented.

## Tests
- 26 pure Kotlin assertions for catalogue search, query escaping, alternate filtering, byte budget and adaptive gain.
- 6 archive assertions with filesystem and Android/library stubs; settings validation precedes restore.
- Syntax parsing is not Android compilation. No Android SDK available in this environment; APK build, emulator, provider integration, XML runtime and audio behavior on real devices are not verified.
