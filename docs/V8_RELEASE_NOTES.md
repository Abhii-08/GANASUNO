# GANASUNO 8.0 — discovery, collections and player polish

Based on 7.0; package com.abhi.ganasuno, Android 8+ / SDK 36, JDK 17 unchanged. 12 previously missing catalogue entries now have source coverage: 4, 23, 27, 50, 54, 89, 96, 112, 122, 172, 186, 199. Of these, one is marked added source, eleven are partial. Overall: 50 added source, 10 existing source, 90 partial, 50 not implemented. This is 150/200 full-or-partial source coverage, not 150 complete tested features.

## Usage
- Settings → New features (v8): audio preference, authorized direct file save-and-play, manually tagged explicit filter, artwork queue, cover rotation, discovery, local artist connections, collection bookmarks, RTL layout and support links.
- Song long-press menu → Mark / unmark explicit or Related artists locally. Explicit classification is manual and is not parental-control-grade. Provider restrictions are not bypassed.
- Library → Saved collections: store named HTTPS album/playlist/podcast links locally. YouTube playlist links/IDs can also be bookmarked in v8 tools. No online account sync or cached remote contents.
- Now playing → Queue: artwork-based queue. Enable rotation in v8 tools. Actual cover animation is only a rotation of a still image.
- Android launcher → Widgets → GANASUNO Turntable. Graphic and track metadata, not a live animated vinyl widget.
- RTL preview: set direction in v8 tools. System Arabic or Hebrew locale selects translated resource labels. Most programmatic UI text is still English.
- Desktop companion: use Album button next to each track to assign matching album groups. Keep albums continuous prevents crossfade between matching groups. Saved desktop playlists retain album tags. Browser switching is not sample-accurate gapless playback.
- Support links open the documented upstream services in a browser; there is no supplied GANASUNO blog or support site.

## Verification
- 13 standalone compiled Kotlin core assertions: playlist ID validation, HTTPS validation, artist co-occurrence ranking, bitrate selection.
- Kotlin PSI syntax parsing (47 app source files); XML well-formedness; desktop JavaScript syntax and same-album unit checks; prior room server regression tests; archive integrity.
- Android SDK unavailable: no Android dependency type-check, APK compilation, emulator, network/stream testing, UI layout verification or widget runtime testing. No desktop browser runtime verification. Existing features' status is carried forward from v7, not newly verified. Source licensing remains unresolved (see THIRD_PARTY.md).
