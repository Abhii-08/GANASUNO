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
- Rhino 1.8.1 — MPL 2.0 primary license: https://central.sonatype.com/artifact/org.mozilla/rhino/1.8.1 ; see https://github.com/mozilla/rhino/blob/master/LICENSE.txt for additional notices. See docs/licenses/Rhino-1.8.1-LICENSE.txt and the Rhino source archive evidence in the release checklist. Verify coverage for both rhino and rhino-engine and preserve third-party notices embedded in the upstream license.
- OkHttp Public Suffix List notice: https://publicsuffix.org/list/public_suffix_list.dat ; MPL 2.0 https://mozilla.org/MPL/2.0/. Exact corresponding snapshot/source required.
- jsoup — MIT, Copyright (c) 2009-2026 Jonathan Hedley. See the bundled notice and docs/licenses/jsoup-MIT.txt.
- Android core library desugaring — desugar_jdk_libs_nio 2.1.5 declares GPLv2 with Classpath Exception; see the checked-in Desugar license and exception files. Confirm the matching source for both the runtime library and NIO configuration and preserve applicable notices.
- The separate desugar_jdk_libs_configuration_nio 2.1.5 build input carries the [R8 BSD 3-Clause text](docs/licenses/Desugar-NIO-Configuration-R8-BSD-3-Clause.txt), verified byte for byte against its Maven JAR and the upstream R8 release preparation commit.
- LRCLIB external service: https://lrclib.net/ (not a bundled software library).

The bundled notices now include [Rhino 1.8.1 NOTICE](docs/licenses/Rhino-1.8.1-NOTICE.txt), [Rhino tools NOTICE](docs/licenses/Rhino-1.8.1-NOTICE-tools.txt), [OkHttp 4.12.0's Public Suffix List notice](docs/licenses/OkHttp-4.12.0-Public-Suffix-NOTICE.txt) and the [matching Apache license from six exact AndroidX source coordinates](docs/licenses/AndroidX-source-APACHE-2.0.txt). The source copies in these links retain upstream bytes. The in-app copy normalizes trailing whitespace.

The FOSS release inventory contains 84 resolved entries across the runtime and desugaring configurations. The source review covers 83 code-bearing coordinates and one metadata-only Guava marker. It matched 78 Maven source deliveries to published checksums and seven source mappings to exact upstream GitHub ZIPs. The review archive records per-coordinate notice paths and test-only exclusions. Exact-version notices, preferred-form build inputs, upstream changes and asset rights still require evidence. The [release checklist](docs/licenses/RELEASE_CHECKLIST.md) records the review results and gaps.

The bundled text is available in the app under Documentation and external support ? Read bundled dependency notices. Public releases must provide free access to Corresponding Source alongside the APK. GPL covers the relevant software. Music, lyrics, artwork and provider services require their own permissions.

See [review sheet](docs/licenses/DEPENDENCY_REVIEW.csv), [release checklist](docs/licenses/RELEASE_CHECKLIST.md) and [app notices](app/src/main/assets/dependency_notices.txt).
