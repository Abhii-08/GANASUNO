# Release setup

1. Confirm original source and third-party redistribution rights, preserve original notices and applicable licenses. See THIRD_PARTY.md. Do not publish until resolved.
2. In Android Studio build and device-test the candidate (including playback and queue edge cases). The current candidate has not been compiled here.
3. Generate a permanent release keystore in Android Studio (Build > Generate Signed Bundle / APK). Back it up securely outside the repository. The public release must always use the same application ID and signing certificate. Existing debug installs may require migration.
4. Repository Settings > Secrets and variables > Actions: add SIGNING_KEYSTORE_B64 (base64-encoded keystore), SIGNING_STORE_PASSWORD, SIGNING_KEY_ALIAS, SIGNING_KEY_PASSWORD. Never commit or share these values. Add Actions variable RELEASE_APPROVED=true only after licensing and testing review.
5. Push code to main, check the debug build. Create and push a tag matching versionName, e.g. v1.1. Each future release must increase versionCode and versionName.
6. The tag workflow builds a FOSS release APK, signs it, verifies it and publishes GANASUNO.apk and SHA256SUMS.txt. If the workflow fails no APK is published. Check the release and test an update from a previous same-key release before distributing.
7. Official update checks use the public GitHub API; the release notes must contain a standalone `versionCode: N` line and the APK asset must be GANASUNO.apk. Automatic checks are opt-in and run when Android permits. Download opens the browser; Android requires user approval to install. Nightly requires a separately configured custom manifest.

PowerShell: to base64 encode a local keystore for the secret (do not send it to anyone), use `[Convert]::ToBase64String([IO.File]::ReadAllBytes('C:\secure\ganasuno.jks')) | Set-Clipboard`. Paste into the GitHub secret field and clear the clipboard afterwards.
