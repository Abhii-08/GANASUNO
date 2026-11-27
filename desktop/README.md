# GANASUNO desktop offline companion (preview)
Open `index.html` in a modern desktop browser on Windows, macOS or Linux. No install, network connection or server is needed. Choose local audio files that you own. Supports queue, reorder/remove, search, shuffle, repeat, speed, volume, seek, mini view and two-deck crossfade. Playback support depends on browser codecs. The queue is session-only. This is a browser-based local companion, not a native desktop application, and does not sync with Android, play online streams, or support accounts. Crossfade is implemented here only and is not Android crossfade or AutoMix. Browser execution was not tested in the build environment.

## v5 audio tools
Stereo balance rotation (3–30 seconds, direction), feedback delay presets, waveform seek from locally decoded audio (up to 60 MB input, browser codec support required). These are browser Web Audio features, not native desktop or full spatial audio. No network requests.

## v7 saved local playlists
The sidebar can save the current local queue, including audio file copies, via IndexedDB. Reopen or delete by name. Maximum 200 MB per playlist; browser storage quotas and file:// origin behavior vary. Browser data clearing removes saved files. If file:// storage is blocked, serve this directory locally with `python -m http.server 8000 --bind 127.0.0.1` and open `http://127.0.0.1:8000` using the same browser/profile each time. This feature has only been syntax-checked here, not browser runtime tested.

Version 8: Use the Album button for each queued file to assign a shared album label. The Keep albums continuous checkbox prevents crossfade between adjacent matching labels. Saved playlists retain labels. This is not sample-accurate gapless playback.

Version 17: Edit Album and Tags (artist and music/podcast type), then save the queue as a playlist. Browse saved files by album, artist or podcast with sidebar filtering; select a group to load its audio. The same file fingerprint (name, size, last-modified) is deduplicated within each group. Tags are manual, and the companion remains browser-based. JavaScript syntax and standalone grouping checks were run; IndexedDB/browser playback was not runtime-tested.

## Version 18 — optional desktop window
The same local player can be opened in an OS webview window. In this folder run `python -m pip install -r requirements.txt` then `python launcher.py` (Windows: `Open-Desktop-Window.bat` after installing requirements). Mini view resizes the window; Quit closes it after a confirmation. The webview dependency and OS backend may require additional packages or WebView2 on Windows. This does not share browser IndexedDB automatically, or guarantee codec support. No packaged executable, native audio engine or Android sync; the GUI was not run here. Python control logic was mock-tested only. Browser mode continues without the dependency.

## Version 22 — independent native local player
`python -m pip install -r requirements-native.txt` then `python native_player.py` (Windows: `Open-Native-Windows.bat`). PySide6 Qt Widgets and QtMultimedia provide native controls, local audio playback, queue/next, pause/stop, seek and volume. No WebView, online streaming, sync or packaged installer. GUI/codecs and OS compatibility are not tested. Existing browser player remains available.

### Native v23 controls
Set crossfade to 0–15 seconds (0 disables). Assign matching album tags to adjacent local tracks and enable album continuity to suppress their crossfade. The queue, selected track and album tags restore after restart; playback position does not. No Android crossfade or gapless playback guarantee. Qt GUI/audio runtime not verified here.

### Native v24 resume and optional packaging
The session records the last selected track, position, volume, crossfade duration and album-continuity option. Press Play after restart to resume a seekable local track. Version 1 session files remain readable. `.github/workflows/desktop.yml` is a manually dispatched PyInstaller packaging workflow for Windows, macOS and Linux; no packaged binaries were built or tested in this release.
