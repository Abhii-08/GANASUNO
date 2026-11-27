# GANASUNO v1.1 final release report

**Public release blocked.** The review started from commit `ae901a9` on `main`. GitHub reports the repository as private with zero releases. Its only remote branch or tag ref was `main` at that commit. The candidate changes are prepared for private main. The authenticated preflight confirmed that the approval variable is disabled or unset. Repository visibility, release tags and GitHub Secrets are outside the candidate push. Keep `RELEASE_APPROVED` disabled until the required evidence and explicit launch approval are in place.

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

Scanned 452 unique historical blob/path pairs reachable from locally available refs and HEAD. The redacted checks covered private key headers, common GitHub, Google, AWS, Slack and OpenAI credential formats, quoted credential assignments and machine-local path patterns. There were no credential matches and two documentation path examples. No tracked files matched the checked signing-key, local configuration or APK patterns. `.gitignore` excludes checked examples of SDK configuration, keystores, environment files and Google service configuration. Checksum-verified Gitleaks 8.30.0 found zero matches in the reachable history and the tracked and intended new files, with full output redaction. A follow-up scan of the changed files also found zero matches. The remote branch and tag listing matched the locally audited history. A redacted pattern review covered seven accessible Actions job logs from the latest attempts of seven listed runs, with zero matches. Historical artifact contents, older job attempts and internal GitHub refs remain outside scope. [Scanner release](https://github.com/gitleaks/gitleaks/releases/tag/v8.30.0) The GANASUNO signing key and passwords were never accessed. A separate archive scan reported 1,853 matches across the staged BravePipeExtractor, Desugar and NanoJSON ZIPs, including recorded request mocks, crypto test fixtures and one provider-key pattern. Counts include repeated matches and duplicate archive mappings. Deduplication by archive member, rule and line yields 152 BravePipeExtractor matches in test mocks, 2 NanoJSON matches in the issue-38 test fixture, and 31 desugaring matches including test fixtures and 9 generic signals in runtime crypto/SSL source. The original archives are quarantined outside Git. Before distribution, replace or sanitize credential-bearing fixtures, document every exclusion against the exact build inputs and verify the reviewed source manifest. The redacted report is at `build/release-secret-audit/source-candidates-redacted.json`.

Release-critical lint review retained the exported Media3 service for Android integration and added a connection policy admitting the app UID, trusted Android media controllers and the Media3 notification controller. The pinned 1.8.0 source previously accepted connections by default. Android 12+ extraction rules now exclude all documented storage domains for both cloud backup and device transfer. This preserves the existing OS backup opt-out and leaves in-app export/import available. YouTube URL filters remain opt-in handlers rather than verified links to domains the project does not control. Platform controllers beyond the emulator and manufacturer-specific data transfer still need device verification. [Media3 controller guidance](https://developer.android.com/media/media3/session/background-playback) ? [Android backup rules](https://developer.android.com/identity/data/autobackup)

The updater requires HTTPS metadata without URL credentials, rejects HTTP redirects and constrains the official stable APK to the tag's exact expected asset URL on this repository. The foreground automatic check now rechecks opt-in before acting on a late response. The UI compares numeric version codes and asks before opening the browser. Android verifies compatible package identity and signing for an in-place upgrade. The app itself delegates APK download and installation to the browser and Android. Configured custom manifests remain separate trust decisions.

## 4. License verification

The recorded FOSS runtime and desugaring scan lists 84 resolved entries, with 75 entries lacking embedded notices in the bounded scan. The review sheet now includes every recorded coordinate. Existing bundled GPL v3, Apache, MPL, MIT, BSD, Rhino, Guava and desugaring notices were preserved. Added four exact source texts covering Rhino's two NOTICE files, OkHttp's Public Suffix List notice and a shared Apache license recovered from six AndroidX source coordinates. The debug and unsigned release APKs include the exact notice asset, and the existing license-text body was retained and the four recovered texts were appended. The app's Documentation and external support menu points to that asset. Exact component NOTICE and copyright coverage, SimpMusic file attribution and modifications, MPL-covered source, exceptions and original asset rights remain open. Maintainer records are needed for the launcher logo, the README developer image at `docs/assets/developer-circle.png` and any distributed testing audio. The tracked file list contained no common audio fixtures. See the [license checklist](docs/licenses/RELEASE_CHECKLIST.md).

## 5. Corresponding source

All five local source archives match the existing recorded SHA-256 values and passed full ZIP CRC checks. Their recorded revisions are in the license checklist. Staged candidates cover 83 exact coordinates in local `build/release-source-review/`, with archive checksums and notice filenames in `SOURCE-CANDIDATES.tsv`. The remaining Guava marker JAR contains metadata and no classes, and its POM was staged. Candidate sources include cached Maven source JARs and seven source JARs downloaded from Google Maven and Maven Central. Source JARs can omit required build input, patches or notices. The packager was exercised in a disposable fixture. It reproduced identical output and rejected a changed checksum and tracked local configuration. The actual release correctly stops while the reviewed source manifest is absent. The dependency archive revisions still need binary-build provenance and the remainder of the required sources and notices. Generated a deterministic local `build/release-source-review/App-Source-Review-v1.1.zip` covering 286 Android application, build and notice files, with a SHA-256 sidecar. A fresh extraction built FOSS debug under JDK 17, passed the six Kotlin tests and verified all 84 dependency hashes. Documentation refreshed after the build, and app and build-input bytes stayed identical. This review snapshot excludes unapproved dependency archives and is not the GPL release deliverable. Final `Corresponding-Source-v1.1.zip` remains blocked by exact source and notice review and upstream archive findings. [GPLv3 §§1, 5, 6](https://www.gnu.org/licenses/gpl-3.0.html)

## 6. Build and test results

| Check | Result |
| --- | --- |
| FOSS debug, Full debug and unsigned FOSS release | JDK 17 build passed. APK manifests confirm `com.abhi.ganasuno` / `1.1` / `112` / min SDK 26 / target SDK 36. Both debug APK signatures verified. |
| Android lint | Passed with zero errors after fixing 105 initial findings. 290 warnings remain after the manifest and session review. |
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
| Permanent release signing | Both successful historical signing runs report the same public certificate SHA-256, `d57b60ca7a0fd76e4eca506d0ffe21d303a803b321d264e8259053d7e296b84b`. Both were built with versionCode 112. Installed code 112 will see the first code-112 release as equal and receive no newer-version prompt. The modified candidate needs a private signing run, and the permanent-signer upgrade needs an authentic lower-code APK. [Prior signing run](https://github.com/Abhii-08/GANASUNO/actions/runs/46042269013) |

### Root causes of the previous Python failures

| Previous cases | Cause and repair |
| --- | --- |
| Three version assertions | Feature-era tests expected internal version 45.0/code 111. They now cross-check the Gradle version and code against the current changelog and release notes. |
| Five player layout cases, including both errors | The former compact branch was absent, so the two string splits raised `IndexError`. All action rows had moved into the scrolling region behind the artwork. Transport is now pinned in a bounded panel, with secondary controls pinned in normal layouts and placed before the artwork in compact layouts. Tests also follow the current transport calls and artwork refresh target. |
| Three endless queue cases | The service only attempted refill after running out of successors, blocked repeated attempts for the same seed and stopped at 500 items. In-app Next had no refill request. The restored route allows an explicit refill at the boundary, coalesces repeated pending requests, rejects results after queue changes and retries failed automatic attempts after a cooldown. Automatic refill respects the existing opt-in, live, repeat, sleep timer and room state. Sequential mode trims old history and shuffle keeps its current order. |
| Three Settings assertions | Category labels and catalogue contents had changed. Tests now check the current seven categories, unique coverage, icon routes and an explicit expected map of every Settings action. App Settings routes were preserved. |
| Three additional Windows temp-path discrepancies on the original unrestricted run | The desktop queue resolves paths and Windows temp paths can use 8.3 aliases. Fixture paths now match the queue normalization. Desktop production code was untouched. |

## 7. Remaining blockers

- Document authorization for YouTube/YouTube Music access, background playback and cache use, and separately for lyrics and any redistributed media, or approve compliant alternatives for public distribution. Review remaining integrations and privacy disclosures against actual behavior.
- Verify exact dependency licenses, notices and source provenance, upstream file attribution and artwork, photo and audio rights.
- Review historical Actions artifact contents before making them visible, and resolve the staged source archive scan findings before committing the exact distribution inputs. Device-check the Media3 trusted-controller behavior and OEM-specific data extraction.
- Verify a privately signed final candidate against the established public certificate SHA-256, test clean install and in-place upgrade, and rebuild from the final extracted corresponding source.
- Review and approve public launch explicitly.

## 8. Public release readiness

The tag workflow requires both a public repository and `RELEASE_APPROVED=true`. The existing Git authentication confirmed that the approval variable is disabled or unset. Its value was left untouched. The manual signing workflow checks APK identity and matches the expected public certificate when configured, with artifact-only output. The release workflow is prepared to publish `GANASUNO.apk`, `GANASUNO-v1.1-universal.apk`, `SHA256SUMS.txt` and `Corresponding-Source-v1.1.zip` after review. Both APK filenames contain the same signed APK bytes. The permanent download link becomes usable once a stable release with that asset is publicly accessible. [Direct APK download](https://github.com/Abhii-08/GANASUNO/releases/latest/download/GANASUNO.apk)

## 9. Remaining commands

Run the private verification commands after committing the reviewed patch and source inputs. Use an authenticated GitHub CLI for private CI.

```powershell
.\gradlew.bat :app:assembleFossDebug :app:assembleFossRelease :app:lintFossRelease :app:testFossDebugUnitTest :app:verifyReleaseDependencies :app:generateDependencyInventory
python -m unittest discover -s tests
python -m unittest discover -s server
python tools/package_release_source.py (git rev-parse HEAD) 1.1
$repo = "Abhii-08/GANASUNO"
$approval = gh variable list --repo $repo --json name,value --jq '.[] | select(.name=="RELEASE_APPROVED") | .value'
if ($LASTEXITCODE -ne 0 -or ($approval -and $approval -ne "false")) { throw "Verify approval is disabled" }
$reviewedCommit = git rev-parse HEAD
git push origin main
gh workflow run signing-test.yml --repo $repo --ref main
gh run list --repo $repo --workflow signing-test.yml --commit $reviewedCommit --limit 5
$signingRunId = Read-Host "Matching signing run ID"
gh run watch $signingRunId --repo $repo --exit-status
gh run download $signingRunId --repo $repo --name GANASUNO-Signed-APK --dir build/signed-candidate
```

Before testing the permanent-signer upgrade on a dedicated device, compare both APK signatures and their badging. Use a genuinely older permanent-signed APK with code below 112. The two known GitHub artifacts are both code 112.

```powershell
$oldApk = Read-Host "Path to the older permanent-signed APK"
$newApk = "build/signed-candidate/GANASUNO.apk"
$androidTools = Join-Path $env:LOCALAPPDATA "Android/Sdk/build-tools/36.0.0"
$adb = Join-Path $env:LOCALAPPDATA "Android/Sdk/platform-tools/adb.exe"
$digest = "d57b60ca7a0fd76e4eca506d0ffe21d303a803b321d264e8259053d7e296b84b"
foreach ($apk in @($oldApk, $newApk)) {
    $cert = & (Join-Path $androidTools "apksigner.bat") verify --print-certs $apk
    if ($LASTEXITCODE -ne 0 -or -not ($cert -contains "Signer #1 certificate SHA-256 digest: $digest")) {
        throw "Permanent signer verification failed"
    }
    & (Join-Path $androidTools "aapt.exe") dump badging $apk | Select-String "^package:"
}
$serial = Read-Host "Dedicated test device serial"
& $adb -s $serial install $oldApk
# Seed the test library and settings, then install the verified code-112 candidate in place.
& $adb -s $serial install -r $newApk
```

The certificate for the release variable is public. Only after explicit approval and closure of the blockers, prepare the README launch links and use the following commands for the reviewed commit. GitHub can make earlier Actions history and logs visible when visibility changes. [GitHub CLI visibility options](https://cli.github.com/manual/gh_repo_edit)

```powershell
gh variable set RELEASE_CERT_SHA256 --repo Abhii-08/GANASUNO --body d57b60ca7a0fd76e4eca506d0ffe21d303a803b321d264e8259053d7e296b84b
gh repo edit Abhii-08/GANASUNO --visibility public --accept-visibility-change-consequences
gh variable set RELEASE_APPROVED --repo Abhii-08/GANASUNO --body true
$reviewedCommit = Read-Host "Exact reviewed commit SHA"
git tag -a v1.1 $reviewedCommit -m "GANASUNO v1.1"
git push origin refs/tags/v1.1
gh release view v1.1 --repo Abhii-08/GANASUNO
gh release download v1.1 --repo Abhii-08/GANASUNO --pattern SHA256SUMS.txt --dir build/release-verification
Invoke-WebRequest "https://github.com/Abhii-08/GANASUNO/releases/latest/download/GANASUNO.apk" -OutFile build/release-verification/GANASUNO.apk
$expected = (Select-String -Path build/release-verification/SHA256SUMS.txt -Pattern "^[0-9a-f]{64}  GANASUNO\.apk$").Line.Split(" ")[0]
if ((Get-FileHash build/release-verification/GANASUNO.apk -Algorithm SHA256).Hash -ne $expected) { throw "APK checksum mismatch" }
```

## 10. Maintainer approval checklist

- [ ] Exact app/source commit reviewed, license and asset evidence recorded and service permissions resolved.
- [ ] Clean CI build, lint and tests reviewed, dependency hashes verified and credential audit complete.
- [ ] Candidate identity, certificate and source ZIP verified. Extracted source rebuilds.
- [ ] Install a same-signer older build on a device. Add favorites, playlists and history, set preferences and import authorized local media. Exercise disabled and enabled update checks, equal and older version metadata, malformed metadata, wrong release asset and cancellation. Test a valid newer candidate through the browser and Android prompt and verify data remains after upgrade. Confirm Android refuses a mismatched certificate for the installed package.
- [ ] Open bundled legal notices on-device and check screen access. Verify compact/large-font playback layout, notification controls and the preserved features allowed for distribution.
- [ ] Maintainer expressly approves repository visibility and publication. Verify the stable release checksum and direct link from a signed-out session after publication.
