# GANASUNO licensing and third-party notices

Copyright (C) 2026 Abhishek (original GANASUNO contributions).
The original GANASUNO software contributions that the maintainer has authority to license are offered under GNU GPL version 3 (GPL-3.0-only). No warranty is provided, to the extent permitted by law. Third-party works retain their original copyright and applicable terms. Developer portrait and other separately owned artwork are not relicensed by the software license.

Full GPL text: https://www.gnu.org/licenses/gpl-3.0.html
Project source: https://github.com/Abhii-08/GANASUNO (planned public source location; not yet verified as populated).

Credits and upstream source references:
- SimpMusic contributors — original project notes acknowledge API integration information; GPLv3 license: https://github.com/maxrave-dev/SimpMusic/blob/dev/LICENSE. Verify copied/modified files and retain original notices.
- BravePipeExtractor contributors — GPLv3 license at pinned revision: https://github.com/maxrave-dev/BravePipeExtractor/blob/105aad96c7/LICENSE
  Source: https://github.com/maxrave-dev/BravePipeExtractor/tree/105aad96c7
- HlsDownloader contributors — GPLv3 license at pinned tag: https://github.com/evermind-zz/HlsDownloader/blob/1.0.0/LICENSE
  Source: https://github.com/evermind-zz/HlsDownloader/tree/1.0.0
- NanoJSON — Copyright 2011 The nanojson Authors; Apache-2.0 header verified at https://github.com/TeamNewPipe/nanojson/blob/e9d656ddb49a412a5a0a5d5ef20ca7ef09549996/src/main/java/com/grack/nanojson/JsonParser.java
- AndroidX and Media3, Material, Kotlin, OkHttp, Okio, Wire, Guava and other components: see docs/licenses/DEPENDENCY_REVIEW.csv for versions, preliminary licenses and upstream references.
- Rhino 1.8.1 — MPL 2.0 primary license: https://central.sonatype.com/artifact/org.mozilla/rhino/1.8.1 ; see https://github.com/mozilla/rhino/blob/master/LICENSE.txt for additional notices. Exact release terms and source still to be reviewed.
- OkHttp Public Suffix List notice: https://publicsuffix.org/list/public_suffix_list.dat ; MPL 2.0 https://mozilla.org/MPL/2.0/. Exact corresponding snapshot/source required.
- jsoup — MIT, Copyright (c) 2009-2026 Jonathan Hedley. Complete embedded text below.
- Android core library desugaring — desugar_jdk_libs_nio 2.1.5 declares GPLv2 with Classpath Exception; obtain exact license/exception, additional notices and corresponding source before APK release.
- LRCLIB external service: https://lrclib.net/ (not a bundled software library).

REVIEW STATUS: 82 runtime entries from the user's debug inventory plus one separately declared desugaring dependency. The inventory contains 74 missing embedded notices. A missing notice does not mean no license. This is a partial review, not legal clearance. Exact release dependencies, upstream notices, original code/artwork rights, corresponding source and separate server/desktop distributions remain to be checked. Do not enable RELEASE_APPROVED or publish APK until resolved. GPL licensing does not grant rights to third-party music, lyrics, branding or service access.

See [review sheet](docs/licenses/DEPENDENCY_REVIEW.csv), [release checklist](docs/licenses/RELEASE_CHECKLIST.md) and [app notices](app/src/main/assets/dependency_notices.txt).
