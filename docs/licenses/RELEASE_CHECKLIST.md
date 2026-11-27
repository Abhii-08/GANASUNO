# Public release checklist — v1.1

- [x] Maintainer approved GPL v3 for original software contributions.
- [x] Full GPL v3 text and available Apache/MPL/jsoup texts added.
- [x] User's debug dependency inventory retained and categorized.
- [x] Pinned GPL license files verified for BravePipeExtractor and HlsDownloader; NanoJSON source header verified.
- [ ] Review original/modified source against upstream and preserve copyright and modification notices. Confirm rights to all assets, including developer photo (separately owned), bundled fonts, icons and media. Do not assume GPL covers third-party artwork.
- [ ] Check exact release variant runtime and desugaring artifacts (debug inventory is not sufficient), server and desktop if distributed; collect complete version-specific licenses, NOTICE files, copyright and any special exceptions, including protobuf and Rhino additional notices.
- [ ] Supply exact Corresponding Source for APK, including required libraries, generated/build scripts and modifications; archive exact source snapshots or ensure equivalent free access and maintain availability under GPLv3 section 6(d). A root LICENSE or moving upstream link alone is not enough.
- [ ] Ensure notices and license access accompany the APK; verify the in-app legal notice screen. This documentation patch does not alter executable code.
- [ ] Verify rights/terms for third-party music sources, images and external services; GPL does not grant media distribution rights.
- [ ] Check source for private credentials and machine-local paths before public push. Never commit signing keys/passwords.
- [ ] Test signed release and update migration; keep `RELEASE_APPROVED` unset until complete. Do not publish an APK yet.

Official references: https://www.gnu.org/licenses/gpl-3.0.html ; https://www.gnu.org/licenses/gpl-faq.html ; https://www.apache.org/licenses/GPL-compatibility ; https://www.mozilla.org/en-US/MPL/2.0/FAQ/ .
