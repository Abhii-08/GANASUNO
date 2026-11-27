# Public release checklist — v1.1

- [x] Original contributions carry the maintainer's GPL v3 grant. The root COPYING text is in `LICENSE`.
- [x] Review sheet covers the 84 entries in the recorded FOSS release runtime and core library desugaring inventory, including `desugar_jdk_libs_configuration_nio`.
- [x] Matched 84 release and desugaring binary archives to public repository checksums and pinned the local artifact SHA-256 values in [RELEASE-DEPENDENCY-SHA256SUMS.txt](RELEASE-DEPENDENCY-SHA256SUMS.txt).
- [x] Matched the five downloaded archive hashes to [SOURCE-SHA256SUMS.txt](SOURCE-SHA256SUMS.txt) and checked every ZIP entry's CRC. Archive comments record these revisions.

| Archive | Recorded revision |
| --- | --- |
| BravePipeExtractor | `105aad96c7a496c7089f8fe3b1904de7082afc42` |
| Desugar 2.1.5 | `73170c345e6a762fc6a1f0301bb15218850023ef` |
| HlsDownloader 1.0.0 | `f79e5e3b8c140db0958aeec6799fe768a5f0a1f1` |
| NanoJSON | `e9d656ddb49a412a5a0a5d5ef20ca7ef09549996` |
| Rhino 1.8.1 | `f2d1ffef9dafbd625ca1ea79b8893ecbaee07e61` |

- [x] Staged source candidates for 83 coordinates and recorded the metadata-only Guava marker. The candidate-to-coordinate manifest is in local `build/release-source-review/SOURCE-CANDIDATES.tsv`.
- [ ] Confirm the archive revisions against the resolved artifact builds, including Rhino and Rhino Engine, the desugaring configuration and any required upstream build inputs. Verify exact notices, copyrights and changes for the entire graph. The inventory scan is bounded and 75 entries lack embedded notices in its output.
- [ ] Record SimpMusic and other upstream paths and revisions for any copied code. Retain original copyright and identify modifications and dates where GPL §5 applies. The historical notes document API inspiration but do not establish individual file origins.
- [ ] Record rights evidence for the AI logo, developer photograph and any testing audio or artwork distributed in Git or the APK. Check source image inputs and screenshots as well as the final assets.
- [ ] Retain GPL texts and notices, Apache 2.0 license and applicable upstream NOTICE contents, MIT and BSD copyright/permission notices, and precise MPL source and notices for covered files. Check each desugaring component's GPLv2 Classpath or assembly exception and included third-party material. Preserve those exceptions with the covered sources and verify their application. [GPLv3](https://www.gnu.org/licenses/gpl-3.0.html) · [Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0) · [MPL 2.0](https://www.mozilla.org/en-US/MPL/2.0/)
- [ ] Collect the exact source and build scripts required for the released APK, including dependency sources, patches and required generated inputs. Follow [release setup](../RELEASE_SETUP.md) and verify a build from the extracted archive. Keep source available without charge for as long as the APK is offered under GPLv3 §6(d). Record the mapping from each APK checksum to its commit and source ZIP.
- [x] Opened bundled notices and GPL text on the API 37 emulator. Verified the exact text is inside all three rebuilt APKs.
- [ ] Verify completeness against the reviewed upstream notices.
- [ ] Resolve [service permissions](../SERVICE_COMPLIANCE.md).
- [ ] Review historical Actions logs and artifacts, triage the redacted dependency archive scan findings before committing source inputs and finish signed device update tests. Obtain explicit approval before enabling `RELEASE_APPROVED`, pushing a release tag or making the repository public.

- [x] Recovered and bundled Rhino 1.8.1's two NOTICE files, OkHttp 4.12.0's Public Suffix List NOTICE, and the shared Apache license from six exact AndroidX source coordinates.


## v1.1 source-review evidence

- [x] Rechecked all 85 staged source records against public upstream evidence. All 78 Maven source deliveries matched published checksums and all seven GitHub archive mappings matched the upstream ZIP SHA-256. There are 83 code-bearing coordinates plus the metadata-only Guava marker.
- [x] Mapped all 84 resolved coordinates into a local review ZIP and verified each reviewed input's SHA-256. The excluded test-file list is included. The nine remaining redacted scanner matches in the Desugar tree are verified crypto algorithm or key-exchange identifiers. Original flagged archives were not shipped in the review ZIP.
- [x] Built the FOSS unsigned APK from a fresh extraction using JDK 17 and the existing pinned Maven artifact cache. This demonstrates the application rebuild path.
- [x] Verified that the OkHttp 4.12.0 Public Suffix List resource in the signed APK byte-matches the tagged upstream resource. The tagged MPL 2.0 source list independently regenerates the same uncompressed rule data. The review ZIP includes that list, the tagged generator and exact notices. [Pinned OkHttp source](https://github.com/square/okhttp/tree/4984568367caaf359b82c452bd28b5e192824d1c)
- [x] Recovered the exact R8 BSD 3-Clause text from the NIO configuration JAR. It matches the R8 2.1.5 release preparation commit. Staged that commit's human-readable NIO configuration and packaging scripts. [R8 source](https://r8.googlesource.com/r8/+/c331a820ddae7dea41f41a04375b784486d0f705)
- [ ] Establish that each source publication includes the preferred form, build and generation inputs, and changes needed for the exact packaged components. The extracted build consumes the pinned dependency binaries. Still verify the human-to-machine NIO configuration conversion, the exact conversion build and the OpenJDK-to-DEX release inputs.
- [ ] Complete the remaining per-artifact copyright and NOTICE review and original SimpMusic file-lineage evidence. Record Apache NOTICE as applicable, MIT and BSD notices, complete MPL covered source and notices, and the exact desugaring exceptions. Existing evidence includes the root GPL text, the pinned upstream GPL licenses, Rhino notices, the OkHttp Public Suffix List notice and six AndroidX source license matches.
- [ ] After completing those checks, commit the distribution-ready source archives and the exact workflow manifest at `docs/licenses/CORRESPONDING-SOURCE-SHA256SUMS.txt`. The review checksum list under `build/corresponding-source-audit/` is deliberately separate from the workflow manifest.
