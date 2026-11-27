# GANASUNO 26.0 — Account and blog tools

Five previously missing feature areas now have source implementations. #93 and #111 are classified as source-complete for the documented scope; #110, #113 and #114 remain partial. Totals: 137 implemented/existing in source, 24 partial, 39 missing. No prior partial entries were reclassified. Android APK, OAuth and scheduled notification runtime tests remain pending.

## Use
Settings → More features → Batch 26 · account and blog.
- **Playlist append (#110):** select local playlist containing 1–100 eligible non-local, non-live YouTube IDs. Confirm before entering your own authorized short-lived Google OAuth token. Creates a private playlist on first sync and saves the association; checks the remote playlist owner and existing items before appending. At most 1000 remote items are inspected; incomplete listing aborts before insertion. Local-only audio is excluded. No automatic refresh, two-way sync, removal or reorder. A write that returns an error can still have succeeded remotely; check your account before retrying. API quota applies. Creation with an ambiguous failure can leave a remote playlist that must be managed in YouTube.
- **Disconnect (#111):** removes the local association without deleting either playlist. Rename follows the association; local delete removes the association. A future upload creates a new private playlist. No remote delete operation.
- **Rating (#114):** read current video rating, explicitly confirm like/dislike/remove and verify with `getRating`. Does not claim to influence YouTube Music recommendations.
- **Liked videos (#113):** fetch up to 100 official YouTube liked video metadata entries. Open via official service or save explicitly to local favourites. No permanent login or automatic replacement of local favourites.
- **Blog notifications (#93):** supply a public HTTPS RSS/Atom feed URL; first fetch sets a baseline. Persisted Android JobScheduler checks roughly daily when network is available; new entries generate notifications if permission is enabled. Click to open the first new post, check manually to browse current items, or disable. Feed size capped at 1 MiB, 100 posts, 500 remembered IDs; redirects, DTDs and entities rejected. Nested markup and relative URLs may not be supported. No default developer URL or credentials fabricated.

## Security and requirements
OAuth access tokens must be issued by the user's own authorized API client, with applicable read/write scopes, and are retained in memory only for the active operation. Do not enter passwords or API keys in token fields. Requests are directed only to Google's official API endpoint; redirects and automatic retries are disabled. No automatic mutations, account cookies or credential backups. Network failures and quota errors are possible; inspect remote state before repeating writes. Blog notifications are opt-in. No additional library dependencies added.

Official API references:
- https://developers.google.com/youtube/v3/docs/playlists/insert
- https://developers.google.com/youtube/v3/docs/playlistItems/insert
- https://developers.google.com/youtube/v3/docs/playlistItems/list
- https://developers.google.com/youtube/v3/docs/videos/rate
- https://developers.google.com/youtube/v3/docs/videos/getRating
- https://developers.google.com/youtube/v3/docs/videos/list

## Validation in this environment
30 standalone Kotlin assertions passed for eligibility, deduplication, rating validation, HTTPS validation and blog baseline/deduplication. 94 Android Kotlin files parsed for syntax, 32 XML files parsed, 38 backend tests and 10 desktop Python tests passed. This is not Android type checking, APK verification or live API testing. No Android SDK available.

## Device acceptance checklist
1. Build `gradlew.bat assembleFossDebug assembleFullDebug --stacktrace` in Android Studio with SDK 36 and JDK 17.
2. Use a test account and test playlist with properly scoped short-lived OAuth token; verify private playlist creation, duplicate-free second append, wrong-account rejection, quota/network failure reporting, rename and unlink. Confirm neither playlist is deleted.
3. Confirm rating changes and remove rating for a test video. Verify liked videos and local saving. Revoke test token afterward.
4. Configure a controlled RSS/Atom feed, confirm no notification for baseline, add a post, trigger manual check and verify notification, duplicate suppression and disabling. Check notification permissions and scheduled delivery on real Android.
