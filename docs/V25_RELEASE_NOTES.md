# GANASUNO 25.0 — Community tools and regional discovery

Six previously missing catalogue entries receive source code. Four (#129, #146, #147, #148) are classified as source implementations for their documented scope; two (#59, #82) are partial. Totals: 135 implemented/existing source, 21 partial, 44 missing. No pre-existing partial entry is reclassified. Android APK and live network tests remain pending.

## Features and use
Open Settings → More features → Batch 25 · community and charts.
- #129: opt-in transient player cache, 16/32/64/128/256/512 MiB. Clear separately from downloaded files. Changing size applies when the playback service releases and recreates the cache (force-stop/reopen after stopping playback if needed). Existing active limit shown if different. Progressive online playback only; not an offline download manager. Cache keys follow resolved URLs and may miss when those URLs change.
- #146/#147: SponsorBlock lookup of eight skippable categories plus highlight, nine independent settings. Off by default; enables sending public video IDs/categories to SponsorBlock. Main audio and video player poll on the main looper; network work runs separately; live/unseekable items are excluded; invalid, out-of-range and stale results are ignored. Highlight uses explicit manual timestamp selection rather than automatic skipping. No community submissions or votes; disable if shared-room timing conflicts. Very short segments can be missed by the 250 ms polling interval. Community labels may be incorrect.
- #148: explicitly requested Return YouTube Dislike data for current video. Values are estimates, not official counts; missing values shown as unavailable. Source link in dialog. No voting.
- #59: public comments and replies through official YouTube Data API, key entered in session only. Pagination and bounded text. Read-only; account actions are not implemented.
- #82: regional popular music-video results in 15 selectable countries with API order numbering and video details. Not a global music chart, nor an official chart of music plays. API may return no results for some countries/categories. API key required.

## API references
- https://wiki.sponsor.ajay.app/w/API_Docs
- https://www.returnyoutubedislike.com/docs/fetching
- https://developers.google.com/youtube/data/reference/rest/v3/commentThreads/list
- https://developers.google.com/youtube/data/reference/rest/v3/comments/list

## Validation
30 standalone Kotlin assertions for cache limits, masks, IDs, segment bounds/merging, highlight behavior, counts, text and ranking. 92 Android Kotlin files parsed for syntax; 32 XML documents parsed. 38 Python backend tests passed. Android semantic compilation, APK, emulator, cache behavior on-device, SponsorBlock timing, and external API live responses are unverified.

## Device acceptance
Build both `gradlew.bat assembleFossDebug assembleFullDebug --stacktrace` with JDK 17 and SDK 36. Verify cache limit after service restart and verify no offline files disappear. Test an online seekable track/video with known community segments, categories off/on and highlight manual jumps; test live/local media remain unaffected. Test network failures and rapid track changes. Fetch public comments/replies and regional lists with a valid key, and check dislike estimates with a known public video. Test disabling SponsorBlock during shared rooms. No credentials are shipped.
