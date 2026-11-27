# GANASUNO 44.0 — Smart Settings Indicators

Base: GANASUNO 43.0 Grouped Library & Player. Android versionCode 110 (incremented above the base to avoid downgrade errors), versionName 44.0. No preference schema or player/service changes.

## Implementation
- `SmartSettingIndicators.kt`: centralized real saved values and current Media3 state. Recognized binary Off/On menus (stereo rotation and endless mix) use a native accessible Switch on layered glass. Recognized choice menus mark the selected item with a tick and a Current label. Mixed action menus do not invent selection. A nonmatching custom setting is explicitly identified, with no checked preset.
- `MainActivity.kt`: common choose helper delegates to this component; Settings cards have live status labels updated by the existing foreground ticker. DSP, preload, sleep fade and media cache menu entries expose values. Grouped Library and player action routes remain intact.
- `GlassFeatureCard.kt`: current status line and accessibility description, no relayout if unchanged.
- `AdvancedTools.kt`: player style, reverb, preamp, release and stop-on-exit choice states; effect menu summaries.
- `SettingsDialog.kt`: existing radio selections remain; silence skip and normalization show explicit On/Off labels.
- `EqualizerDialog.kt`, `TenBandDialog.kt`: explicit switch state; software EQ presets are checked only when the actual saved 10-band curve exactly matches.
- `CommunityTools.kt`, `Batch28Tools.kt`: SponsorBlock status and preferred direct-file bitrate selection. Existing download automation state labels retained.

## Limitations
Not every action dialog is a setting. One-time operations (import, delete, reset, search), external service/device availability and arbitrary custom values are not assigned invented On/Off states. Hardware output routing and hardware EQ preset recognition are not added. Some settings are stored preferences and may require a supported audio session, source or service restart; a saved state is not proof of effect activation. Existing native radio buttons, sliders, and multi-choice controls are retained. The new UI uses lightweight layered glass, not live backdrop blur.

## Verification
106 Python project/source checks, 38 backend tests, 119 Kotlin files parsed (syntax only), 100 XML files parsed. Android Gradle compilation attempt stopped before compiling at UnknownHostException services.gradle.org. Device interaction, actual Android build, and online streaming are not verified. Manual test: enable/disable Rotation and reopen; choose player style, reverb, theme, speed, and verify current marking; modify audio quality/silence/normalize and return to Settings; test custom EQ curve, app restart, grouped Library and playback.
