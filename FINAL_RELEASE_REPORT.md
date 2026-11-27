# GANASUNO v1.1 final release report

**Public release blocked.** GitHub authenticated verification confirmed a private repository, zero published releases and `RELEASE_APPROVED` disabled or unset. The v1.1 signed candidate was built from production commit `d787f15c0dc1f63db1966aace75b3576c30cc1b8`. The follow-up documentation records the evidence for that APK.

| Release requirement | Status | Evidence |
| --- | --- | --- |
| GPL corresponding source | BLOCKED | Exact app source and candidates for all 84 dependencies are in the local review ZIP. A fresh extracted FOSS build passed with the pinned binary cache. Exact generation inputs, notice coverage and upstream file lineage remain open. |
| Service and asset compliance | BLOCKED | Current provider terms and project integration paths are recorded in `docs/SERVICE_COMPLIANCE.md`. The owner and provider evidence below is outstanding. |
| Permanent signer upgrade | PASS | Installed historical code 111 and upgraded in place to code 112 on the isolated API 37 emulator. Verified retained playlist, favorite, history, preferences and private audio in separate app exports. |
| Security verification | PASS | Reachable production history, signed candidate, filtered source inputs and all 10 available CI artifacts were checked. Repeat the distribution-input scan when the approved source set changes. |
| Final signed APK | PASS | Private Actions signing passed for exact main. Verified `com.abhi.ganasuno`, version `1.1`, code `112` and the permanent signing certificate. |
| GitHub code push | PASS | The signed production commit is on `main`. The cleanup and compliance follow-up changes documentation only. |
| Automatic release workflow | Ready for gated verification | Main CI and private signing passed. Publishing still requires a public repo, `RELEASE_APPROVED=true`, an exact source manifest and the reviewed tag. |
| Public release and direct APK download | BLOCKED | The repository is private and has zero releases. The stable URL will resolve anonymously after publication. |

| Private artifact | Location and SHA-256 |
| --- | --- |
| Signed candidate | `build/signed-candidate/GANASUNO.apk` and byte-identical `build/signed-candidate/GANASUNO-v1.1-universal.apk`. Both `823acb7d1f27ad73737d44e91f6a759de84aca34a7abd8c4b64ec85cbda516f0`. |
| Candidate checksum file | `build/signed-candidate/SHA256SUMS.txt` covers both APK aliases. |
| Source review ZIP | `build/corresponding-source-audit/Corresponding-Source-v1.1-REVIEW.zip`, SHA-256 `d041f97c66b5d26a8e259ce789a2ec4362a23212ef9e912e75592e529b982ec5`. The final release ZIP and manifest are pending. |
| Historical signed fixture | `build/permanent-upgrade/signed-111/GANASUNO.apk`, SHA-256 `20d1b8949a3fa6e7bf2372127ddac8ce3971099cbeae08ae861d48abba02f9ff`. Reconstructed from the original version 45.0 source at code 111. |

Production CI and private signing succeeded for the same main SHA. The verified permanent certificate SHA-256 is `d57b60ca7a0fd76e4eca506d0ffe21d303a803b321d264e8259053d7e296b84b`. [Production CI](https://github.com/Abhii-08/GANASUNO/actions/runs/46062245292) · [Candidate signing](https://github.com/Abhii-08/GANASUNO/actions/runs/46062253496)

The follow-up modified this report, `THIRD_PARTY.md`, `docs/SERVICE_COMPLIANCE.md`, `docs/licenses/DEPENDENCY_REVIEW.csv` and `docs/licenses/RELEASE_CHECKLIST.md`, and recovered the R8 notice at `docs/licenses/Desugar-NIO-Configuration-R8-BSD-3-Clause.txt`. Candidate APKs, source ZIPs, checksums and test fixtures are stored under ignored `build/` paths. The APK application sources are unchanged from the verified build, and earlier completed test suites were reused.

## 1. Work completed

- Reviewed the 447 tracked paths by project area, the Android sources and assets, Gradle setup, manifest, tools, tests, documentation and GitHub workflows. The app declares `com.abhi.ganasuno`, version `1.1`, code `112`, min SDK 26, target and compile SDK 36, JVM target 17 and FOSS/Full flavors. The application ID, version and feature set were preserved. Android lint fixes update Media3 opt-ins, existing animated-art API guards, AppCompat seek-bar integration and image-button tint attributes. Existing About, diagnostics and release-note titles now use the build version. This follow-up restored bounded player controls and in-app Next at the opt-in endless queue boundary.
- Confirmed active README links and local images resolve. The two pre-existing untracked inventory files are byte-identical to their checked-in copies under `docs/licenses/` at the start of the review. They were retained.
- Updated push/PR CI to build both debug flavors and run FOSS lint, Gradle unit tests and both Python suites. The tag workflow also runs both Python suites before signing. Preserved manual signing. The tag workflow retains the approval gate, requires reviewed source, checks all pinned release dependency digests, verifies the signed APK's package and certificate, creates all four requested assets and passes release metadata to the existing updater.
- Preserved the README's branding, developer section, installation and build guidance, and added real emulator screenshots and a privacy summary. Live build, version and download badges are prepared in its launch comment for the first public release.
- Documented unresolved service permissions and alternatives in [SERVICE_COMPLIANCE.md](docs/SERVICE_COMPLIANCE.md).

## 2. Files modified

- `.gitignore` and workflows under `.github/workflows/` for CI, manual signing and tag releases.
- `CHANGELOG.md`, `README.md`, `THIRD_PARTY.md`, `docs/THIRD_PARTY.md`, `docs/RELEASE_SETUP.md`, `docs/SERVICE_COMPLIANCE.md`, `docs/releases/v1.1.md` and this report. The final technical review also changed the Android manifest and `data_extraction_rules.xml`, playback controller checks, the updater opt-out recheck, and both signing/release workflows.
- `docs/licenses/DEPENDENCY_REVIEW.csv`, `docs/licenses/RELEASE_DEPENDENCY-SHA256SUMS.txt`, `docs/licenses/RELEASE_CHECKLIST.md`, `docs/screenshots/` and `app/src/main/assets/dependency_notices.txt` and `app/src/main/assets/release_notes.txt`.
- `tools/package_release_source.py`, the official Gradle 8.13 wrapper JAR and distribution checksum, and `app/build.gradle.kts`.
- Android lint adjustments in `MainActivity.kt`, `VideoActivity.kt`, `PlaybackService.kt`, `PlayerHub.kt`, `SleepTimer.kt`, `AnimatedCover.kt`, `EqualizerDialog.kt`, `TenBandDialog.kt`, `WavyProgress.kt` and `app/src/main/res/layout/activity_main.xml`. Updater decision helpers in `ReleaseCheck.kt` and `AdvancedTools.kt`, with tests in `app/src/test/java/com/abhi/ganasuno/data/ReleaseCheckTest.kt`.

- Playback follow-up in `MainActivity.kt`, `PlaybackService.kt` and `PlayerHub.kt`, and the new Gradle queue policy tests in `EndlessQueuePolicyTest.kt`.
- Python fixture fixes and assertions in `tests/release_metadata.py`, `test_primary_color_fix.py`, `test_v39_glass_header_search.py`, `test_v41_titanium.py`, `test_v431_player_layout.py`, `test_v44_grouped_settings.py`, `test_v441_endless_queue.py`, `test_v45_player_settings.py`, `test_native_queue.py`, `test_native_session.py` and `test_v27_m3u.py`.

## 3. Security verification

Scanned 452 unique historical blob/path pairs reachable from locally available refs and HEAD. The redacted checks covered private key headers, common GitHub, Google, AWS, Slack and OpenAI credential formats, quoted credential assignments and machine-local path patterns. There were no credential matches and two documentation path examples. No tracked files matched the checked signing-key, local configuration or APK patterns. `.gitignore` excludes checked examples of SDK configuration, keystores, environment files and Google service configuration. Checksum-verified Gitleaks 8.30.0 found zero matches in the reachable history and the tracked and intended new files, with full output redaction. A follow-up scan of the changed files also found zero matches. The remote branch and tag listing matched the locally audited history. A redacted pattern review covered seven accessible Actions job logs from the latest attempts of seven listed runs, with zero matches. This initial scan did not cover artifact contents. The follow-up below covers all 10 available archives. Older job attempts and internal GitHub refs were outside the reviewed scope. [Scanner release](https://github.com/gitleaks/gitleaks/releases/tag/v8.30.0) The GANASUNO signing key and passwords were never accessed. A separate archive scan reported 1,853 matches across the staged BravePipeExtractor, Desugar and NanoJSON ZIPs, including recorded request mocks, crypto test fixtures and one provider-key pattern. Counts include repeated matches and duplicate archive mappings. Deduplication by archive member, rule and line yields 152 BravePipeExtractor matches in test mocks, 2 NanoJSON matches in the issue-38 test fixture, and 31 desugaring matches including test fixtures and 9 generic signals in runtime crypto/SSL source. The original archives are quarantined outside Git. Before distribution, replace or sanitize credential-bearing fixtures, document every exclusion against the exact build inputs and verify the reviewed source manifest. The redacted report is at `build/release-secret-audit/source-candidates-redacted.json`.

Release-critical lint review retained the exported Media3 service for Android integration and added a connection policy admitting the app UID, trusted Android media controllers and the Media3 notification controller. The pinned 1.8.0 source previously accepted connections by default. Android 12+ extraction rules now exclude all documented storage domains for both cloud backup and device transfer. This preserves the existing OS backup opt-out and leaves in-app export/import available. YouTube URL filters remain opt-in handlers rather than verified links to domains the project does not control. Platform controllers beyond the emulator and manufacturer-specific data transfer still need device verification. [Media3 controller guidance](https://developer.android.com/media/media3/session/background-playback) ? [Android backup rules](https://developer.android.com/identity/data/autobackup)

The updater requires HTTPS metadata without URL credentials, rejects HTTP redirects and constrains the official stable APK to the tag's exact expected asset URL on this repository. The foreground automatic check now rechecks opt-in before acting on a late response. The UI compares numeric version codes and asks before opening the browser. Android verifies compatible package identity and signing for an in-place upgrade. The app itself delegates APK download and installation to the browser and Android. Configured custom manifests remain separate trust decisions.

Ten available GitHub Actions artifacts downloaded through the private API passed published digest checks where provided and ZIP integrity checks. The redacted nested-archive scan and the follow-up scan of the six release documents reported zero findings. The source review ZIP excludes 158 suspect test-only files across three upstream archives and records those paths. The nine retained scanner hits were verified as crypto algorithm or key-exchange identifiers. The APK contains none of the suspect provider-key, JWT or private-key values from the original upstream test fixtures.

## 4. License verification

The recorded FOSS runtime and desugaring scan lists 84 resolved entries, with 75 entries lacking embedded notices in the bounded scan. The review sheet now includes every recorded coordinate. Existing bundled GPL v3, Apache, MPL, MIT, BSD, Rhino, Guava and desugaring notices were preserved. Added four exact source texts covering Rhino's two NOTICE files, OkHttp's Public Suffix List notice and a shared Apache license recovered from six AndroidX source coordinates. The debug and unsigned release APKs include the exact notice asset, and the existing license-text body was retained and the four recovered texts were appended. The app's Documentation and external support menu points to that asset. Exact component NOTICE and copyright coverage, SimpMusic file attribution and modifications, MPL-covered source, exceptions and original asset rights remain open. Maintainer records are needed for the launcher logo, the README developer image at `docs/assets/developer-circle.png` and any distributed testing audio. The tracked file list contained no common audio fixtures. See the [license checklist](docs/licenses/RELEASE_CHECKLIST.md).

## 5. Corresponding source

Published source checksums verified 85 staged records, and the packed review manifest maps all 84 resolved coordinates to 84 distinct inputs. It also lists the independently verified OkHttp Public Suffix List source supplement. The signed APK's `publicsuffixes.gz` matches the resource at OkHttp commit `4984568367caaf359b82c452bd28b5e192824d1c` byte for byte. Offline regeneration from the tagged MPL-licensed list reproduces its uncompressed binary rule data. The source supplement includes the exact list snapshot, original generator and notices. [OkHttp 4.12.0 source](https://github.com/square/okhttp/tree/4984568367caaf359b82c452bd28b5e192824d1c)

Staged the upstream 2.1.5 R8 release preparation scripts and human-readable NIO specification. The NIO Maven JAR contains generated machine-readable configuration and conversion classes. Its embedded BSD 3-Clause text matches that upstream revision byte for byte, and the local documentation retains it. Exact configuration conversion and the DEX generation input chain remain open. [R8 2.1.5 release preparation](https://r8.googlesource.com/r8/+/c331a820ddae7dea41f41a04375b784486d0f705)

All five local upstream source archives matched the recorded hashes and passed full ZIP integrity checks. The license checklist records their revisions. The review ZIP contains a per-coordinate provenance ledger, notice paths, a test-fixture exclusion ledger and a SHA-256 manifest. All 85 packaged source inputs matched the manifest, including the OkHttp supplement. The production packager already reproduced identical output and rejected modified checksums and tracked local configuration in its disposable test fixture. The publication workflow still requires the approved source manifest and reviewed inputs. The final `Corresponding-Source-v1.1.zip` remains blocked by exact build-input and notice review. [GPLv3 §§1, 5, 6](https://www.gnu.org/licenses/gpl-3.0.html)

## 6. Build and test results

The extracted build used JDK 17 and the pinned binary cache and passed `:app:verifyReleaseDependencies :app:assembleFossRelease` offline. Compared to the signed candidate, 1,349 common APK entries matched byte for byte. Five text entries differed only in line endings, and the build provenance text differed. DEX and native entries matched. This check verifies the application build. The final corresponding source still needs the third-party generation inputs and notice evidence listed above.

| Check | Result |
| --- | --- |
| FOSS debug, Full debug and unsigned FOSS release | JDK 17 build passed. APK manifests confirm `com.abhi.ganasuno` / `1.1` / `112` / min SDK 26 / target SDK 36. Both debug APK signatures verified. |
| Android lint | The existing report has zero errors and 290 warnings. Rechecked the exported Media3 service finding against the UID, trusted-controller and system-notification checks in `onConnect`. The static-context findings refer to the stored application context in `LibraryStore`. Physical OEM media controls and backup-transfer behavior remain untested. |
| Gradle updater unit tests | 3 passed. Tested newer, equal and older version codes, a long installed code, the exact official asset and malformed tag, name, host and query cases. |
| Gradle queue policy tests | 3 passed. Covered the refill threshold, playback and opt-in requirements, explicit Next while paused, live exclusion and safe sequential history trimming. |
| Python server suite | 38 passed. |
| Python project suite | All 126 passed under ordinary Windows temp settings. Existing test methods remain. |
| Dependency verification | All 84 cached resolved binary archives matched public upstream checksums over HTTPS, 61 with published SHA-256 and 23 with SHA-1. Local bytes were pinned by SHA-256 for strict FOSS runtime and desugaring verification. The regenerated notices inventory matches the committed file. Build plugins and dependency metadata are outside this artifact check. |
| Gradle wrapper | Replaced a wrapper JAR that differed from Gradle's published 8.13 checksum with an official hash-matching 8.13 JAR from the installed distribution. Pinned the official binary ZIP digest for fresh wrapper downloads. [Gradle checksums](https://gradle.org/release-checksums/#8.13) |
| Emulator smoke test | FOSS debug installed and launched on a clean API 37 emulator. Captured Library, Settings and the update dialog. Verified the official stable source and automatic checks off by default, exercised the manual check while the project was private, and opened bundled notices and GPL text. A same-code debug reinstall preserved update preferences and a test file. Testing an update with the permanent signer remains pending. |
| Signed debug upgrade on API 37 emulator | Installed a code-111 fixture, seeded a synthetic favorite, playlist, theme, update preferences and private file, then installed code 112 without uninstalling. Both APKs verified with the same existing debug certificate and the seeded data survived. |
| Playback regression on API 37 emulator | Imported two synthesized WAV files with network disabled. Transport stayed visible at the same bounds after swiping in Now Playing. With just the first track queued and paused, Next appended and played the second track. Android's media session reported the second track as playing and the in-app queue listed it as current. |
| Automatic updater on API 37 | An opt-in launch registered periodic job 8722. An injected newer stable result brought up the consent dialog. Later dismissed it and cleared the pending metadata. Turning automatic checks off left no scheduled job. The fixture exercised the device scheduler and UI, and the six extracted-source Kotlin tests cover the existing updater and queue policies. Live GitHub release metadata, browser download and installation still require a published stable asset. |
| Permanent release signing | The exact production commit and historical code-111 fixture both verified with the established certificate. Historical source backup `GANASUNO-45.0-Smart-Player-Categorized-Settings.zip` has SHA-256 `3ccdf5854b2984066cd89116ae9f1cf07b57a02330d576632beefd68d601bc2b`. Fixture commit `db408d5e96adc1d475128fce0e3e004ea20b4bb6` uses the original version and app source with the verified Gradle wrapper and the private signing job. It was reconstructed for testing. [Fixture signing run](https://github.com/Abhii-08/GANASUNO/actions/runs/46065495227) |
| Permanent signer data retention | On the isolated API 37 emulator, verified both APK signer pins and package identifiers, installed code 111, then restored a synthetic library and generated WAV through the app backup screen. Exported before the upgrade, ran `adb install -r` with verified code 112, confirmed the installed code and launched the app. A fresh export after the upgrade matched the fixture's playlist, favorite, playback history, theme, crossfade, endless-queue preference and private WAV digest. Android rejected an attempted update from the existing debug-signed APK with `INSTALL_FAILED_UPDATE_INCOMPATIBLE` and retained code 112. The two exports are in `build/permanent-upgrade/`. Live updater metadata, browser download and consent flow against the public asset await the first stable release. |

The synthetic pre-upgrade export `before-111.zip` has SHA-256 `ed26bcdbe6593df6867f54f6187af49904ae63fddd36321d1b49af14f5dc3c9c`. The separate post-upgrade export `after-112.zip` has SHA-256 `443c285052e6b3dc1a5063f0642ff12194cefafa42b34841f3d30394e018f993`. The retention comparison checked the contents of each export.

The temporary branch `private-upgrade-fixture-v45-111` was deleted from GitHub and the isolated local checkout. GitHub confirmed that the fixture signing run `46065495227` has zero artifacts after deleting artifact `15229943490`. The workflow run record and the local detached evidence checkout were retained. The production signing artifact `15229084157` remained available, and the two local v1.1 APK copies retained the recorded digest. The cleanup required no application rebuild, test rerun or signing run.

### Root causes of the previous Python failures

| Previous cases | Cause and repair |
| --- | --- |
| Three version assertions | Feature-era tests expected internal version 45.0/code 111. They now cross-check the Gradle version and code against the current changelog and release notes. |
| Five player layout cases, including both errors | The former compact branch was absent, so the two string splits raised `IndexError`. All action rows had moved into the scrolling region behind the artwork. Transport is now pinned in a bounded panel, with secondary controls pinned in normal layouts and placed before the artwork in compact layouts. Tests also follow the current transport calls and artwork refresh target. |
| Three endless queue cases | The service only attempted refill after running out of successors, blocked repeated attempts for the same seed and stopped at 500 items. In-app Next had no refill request. The restored route allows an explicit refill at the boundary, coalesces repeated pending requests, rejects results after queue changes and retries failed automatic attempts after a cooldown. Automatic refill respects the existing opt-in, live, repeat, sleep timer and room state. Sequential mode trims old history and shuffle keeps its current order. |
| Three Settings assertions | Category labels and catalogue contents had changed. Tests now check the current seven categories, unique coverage, icon routes and an explicit expected map of every Settings action. App Settings routes were preserved. |
| Three additional Windows temp-path discrepancies on the original unrestricted run | The desktop queue resolves paths and Windows temp paths can use 8.3 aliases. Fixture paths now match the queue normalization. Desktop production code was untouched. |

## 7. Remaining blockers

- Obtain the release-specific permissions and provenance listed below, or approve the documented compliant alternatives for the affected features.
- Complete the preferred-form and generated build input review, particularly desugaring DEX/NIO. Close upstream notice and copyright evidence, track the exact approved source inputs and manifest, and build from a freshly extracted final ZIP. Keep its source available alongside the APK. [GPLv3 §§1, 5 and 6](https://www.gnu.org/licenses/gpl-3.0.html)
- Verify production release metadata, the signed-out download and the updater against the first published stable release.
- Present the public launch approval request once the mandatory blockers close.

### Consolidated owner and provider evidence

1. Written authorization covering YouTube and YouTube Music search, stream extraction, background playback, cache and artwork use, or approval of the documented official playback handoff and local/licensed audio alternative. [YouTube terms](https://www.youtube.com/t/terms) · [YouTube API policies](https://developers.google.com/youtube/terms/developer-policies)
2. Provider and rights-holder evidence for displaying, caching and sharing lyrics and serving album art, or approval to substitute licensed or user-supplied content. [LRCLIB](https://lrclib.net/docs) · [lyrics.ovh](https://github.com/NTag/lyrics.ovh)
3. Logo generator, applicable terms and input-asset rights. Permission from the developer portrait rights holder. Source and redistribution rights for any testing audio or artwork committed or bundled. The upgrade fixture used an independently generated tone.
4. Enabled optional API accounts, their permitted uses and required credits, and operator, privacy, retention and contact details for any maintained backend. Provide only the account or provider identity and applicable terms. Keep credentials in their existing secure configuration.
5. Actual copied SimpMusic or other upstream file paths and revisions, original notices and modifications, or confirmation of the extent of independent implementation with supporting history.

## 8. Public release readiness

The tag workflow requires both a public repository and `RELEASE_APPROVED=true`. The existing Git authentication confirmed that the approval variable is disabled or unset. Its value was left untouched. The manual signing workflow checks APK identity and matches the expected public certificate when configured, with artifact-only output. The release workflow is prepared to publish `GANASUNO.apk`, `GANASUNO-v1.1-universal.apk`, `SHA256SUMS.txt` and `Corresponding-Source-v1.1.zip` after review. Both APK filenames contain the same signed APK bytes. The permanent download link becomes usable once a stable release with that asset is publicly accessible. [Direct APK download](https://github.com/Abhii-08/GANASUNO/releases/latest/download/GANASUNO.apk)

## 9. Remaining commands

After closing the source and permission blockers, stage the reviewed files and exact source inputs only. The existing candidate and test runs already cover commit `d787f15`. The commands verify the final manifest and any new commit intended for launch. Use an authenticated GitHub CLI for private verification.

```powershell
$repo = "Abhii-08/GANASUNO"
$approval = gh variable list --repo $repo --json name,value --jq '.[] | select(.name=="RELEASE_APPROVED") | .value'
if ($LASTEXITCODE -ne 0 -or ($approval -and $approval -ne "false")) { throw "Verify approval is disabled" }
$reviewedCommit = git rev-parse HEAD
python tools/package_release_source.py $reviewedCommit 1.1
if ($LASTEXITCODE -ne 0) { throw "Verify the approved source manifest and source package" }
git push origin main
if ($LASTEXITCODE -ne 0) { throw "Main push failed" }
$remoteCommit = (git ls-remote origin refs/heads/main) -split "\s+" | Select-Object -First 1
if ($remoteCommit -ne $reviewedCommit) { throw "Remote main differs from the reviewed commit" }
gh workflow run signing-test.yml --repo $repo --ref main
gh run list --repo $repo --workflow signing-test.yml --commit $reviewedCommit --limit 5
$signingRunId = Read-Host "Matching signing run ID"
gh run watch $signingRunId --repo $repo --exit-status
gh run download $signingRunId --repo $repo --name GANASUNO-Signed-APK --dir "build/final-signing-$signingRunId"
```

To confirm the two already-tested private APK identities without reinstalling, use the original historical fixture and the verified candidate.

```powershell
$oldApk = "build/permanent-upgrade/signed-111/GANASUNO.apk"
$newApk = "build/signed-candidate/GANASUNO.apk"
$androidTools = Join-Path $env:LOCALAPPDATA "Android/Sdk/build-tools/36.0.0"
$digest = "d57b60ca7a0fd76e4eca506d0ffe21d303a803b321d264e8259053d7e296b84b"
foreach ($apk in @($oldApk, $newApk)) {
    $cert = & (Join-Path $androidTools "apksigner.bat") verify --print-certs $apk
    if ($LASTEXITCODE -ne 0 -or -not ($cert -contains "Signer #1 certificate SHA-256 digest: $digest")) {
        throw "Permanent signer verification failed"
    }
    & (Join-Path $androidTools "aapt.exe") dump badging $apk | Select-String "^package:"
}
```

The certificate for the release variable is public. Only after explicit approval and closure of the blockers, prepare the README launch links and use the following commands for the reviewed commit. GitHub can make earlier Actions history and logs visible when visibility changes. [GitHub CLI visibility options](https://cli.github.com/manual/gh_repo_edit)

```powershell
gh variable set RELEASE_CERT_SHA256 --repo Abhii-08/GANASUNO --body d57b60ca7a0fd76e4eca506d0ffe21d303a803b321d264e8259053d7e296b84b
gh repo edit Abhii-08/GANASUNO --visibility public --accept-visibility-change-consequences
gh variable set RELEASE_APPROVED --repo Abhii-08/GANASUNO --body true
$reviewedCommit = Read-Host "Exact reviewed commit SHA"
git tag -a v1.1 $reviewedCommit -m "GANASUNO v1.1"
git push origin refs/tags/v1.1
if ($LASTEXITCODE -ne 0) { throw "Release tag push failed" }
gh run list --repo Abhii-08/GANASUNO --workflow release.yml --commit $reviewedCommit --event push --limit 5
$releaseRunId = Read-Host "Tag release workflow run ID for the reviewed commit"
gh run watch $releaseRunId --repo Abhii-08/GANASUNO --exit-status
if ($LASTEXITCODE -ne 0) { throw "Release workflow failed" }
gh release view v1.1 --repo Abhii-08/GANASUNO
gh release download v1.1 --repo Abhii-08/GANASUNO --pattern SHA256SUMS.txt --dir build/release-verification
$latest = Invoke-RestMethod "https://api.github.com/repos/Abhii-08/GANASUNO/releases/latest"
if ($latest.tag_name -ne "v1.1" -or $latest.body -notmatch "(?m)^versionCode: 112\s*$" -or $latest.draft -or $latest.prerelease) { throw "Updater metadata mismatch" }
$asset = $latest.assets | Where-Object { $_.name -eq "GANASUNO.apk" }
if ($asset.browser_download_url -ne "https://github.com/Abhii-08/GANASUNO/releases/download/v1.1/GANASUNO.apk") { throw "Stable APK asset mismatch" }
Invoke-WebRequest "https://github.com/Abhii-08/GANASUNO/releases/latest/download/GANASUNO.apk" -OutFile build/release-verification/GANASUNO.apk
$expected = (Select-String -Path build/release-verification/SHA256SUMS.txt -Pattern "^[0-9a-f]{64}  GANASUNO\.apk$").Line.Split(" ")[0]
if ((Get-FileHash build/release-verification/GANASUNO.apk -Algorithm SHA256).Hash -ne $expected) { throw "APK checksum mismatch" }
$cert = & (Join-Path $androidTools "apksigner.bat") verify --print-certs build/release-verification/GANASUNO.apk
if ($LASTEXITCODE -ne 0 -or -not ($cert -contains "Signer #1 certificate SHA-256 digest: $digest")) { throw "Published APK signer mismatch" }
```

## 10. Maintainer approval checklist

- [ ] Exact app/source commit reviewed, license and asset evidence recorded and service permissions resolved.
- [ ] Clean CI build, lint and tests reviewed, dependency hashes verified and credential audit complete.
- [ ] Candidate identity, certificate and source ZIP verified. Extracted source rebuilds.
- [x] Upgrade the permanent-signed historical fixture in place and verify the retention of favorites, playlists, history, preferences and private audio. Prior updater checks cover opt-in and cancellation, version comparison and malformed release metadata.
- [ ] Verify the live signed-out GitHub metadata, browser download and Android consent flow with the public signed asset.
- [ ] Open bundled legal notices on-device and check screen access. Verify compact/large-font playback layout, notification controls and the preserved features allowed for distribution.
- [ ] Maintainer expressly approves repository visibility and publication. Verify the stable release checksum and direct link from a signed-out session after publication.
