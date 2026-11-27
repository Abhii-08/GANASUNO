# Android release setup

Use the existing permanent signing configuration and the four GitHub Actions signing secrets. Keep `RELEASE_APPROVED` unset or false while resolving the [release checklist](licenses/RELEASE_CHECKLIST.md). Set `RELEASE_CERT_SHA256` to the SHA-256 public certificate digest from the previously verified signed APK. A public certificate digest is safe to store as an Actions variable. Package ID `com.abhi.ganasuno` and the permanent certificate must continue across updates.

## Build and private testing

Use JDK 17, the Gradle wrapper and Android SDK 36. The signing-test workflow produces a private artifact when manually dispatched. Run the device checks using the exact candidate commit and signature.

```powershell
.\gradlew.bat :app:assembleFossDebug :app:assembleFossRelease :app:lintFossRelease :app:testFossDebugUnitTest :app:verifyReleaseDependencies
python -m unittest discover -s tests
python -m unittest discover -s server
```

The stable API uses a standalone `versionCode: N` release-note line and the exact `GANASUNO.apk` asset. Official nightly releases require their own publication and manifest. Browser handoff requires a user action. Android enforces the signing rules for an in-place update and presents its installation confirmation. Verify user data on a device using a lower version code and the same certificate.

## Corresponding source

1. For every required component, record resolved artifact, source revision, archive digest and included notices in the release review. Include project code, build and installation scripts, exact source dependencies, generated inputs and required modifications. Use the original dependency archives in the maintainer's source directory and the staged Maven sources under `build/release-source-review/` as candidate inputs.
2. After reviewing archives for provenance, redistribution rights and sensitive content, commit the reviewed source ZIPs and JARs in `third-party-source/`. Commit `docs/licenses/CORRESPONDING-SOURCE-SHA256SUMS.txt` with lines of SHA-256, two spaces and an archive basename. The existing `SOURCE-SHA256SUMS.txt` records five checked downloads and serves as evidence for that subset.
3. Run the packager on the exact candidate commit. It verifies the tracked archive hashes, checks archive integrity and generates a deterministic ZIP with the commit SHA in its comment. This verifies packaging mechanics. Completeness and artifact-to-source correspondence require the review in step 1.
4. Extract the source ZIP to a clean folder and build the FOSS release from that tree with JDK 17. Verify that a user can build and sign their own install without the private signing configuration. Publish the source file alongside the exact APK without charge and retain availability while offering that APK.

```powershell
python tools/package_release_source.py (git rev-parse HEAD) 1.1
```

## Publication

The publication job requires a public repository and an explicitly enabled approval variable. After the maintainer expressly approves both changes, push the reviewed stable tag. The workflow validates the tag, runs the release build, lint and Gradle unit-test task, packages source, verifies APK signature, certificate and manifest identity, and publishes both APK names, the source ZIP and `SHA256SUMS.txt`. See [final release report](../FINAL_RELEASE_REPORT.md) for the remaining commands and launch approval checklist.
