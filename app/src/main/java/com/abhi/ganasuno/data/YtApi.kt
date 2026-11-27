package com.abhi.ganasuno.data

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Song search through the YouTube Music "innertube" API.
 * Endpoint, client (WEB_REMIX), headers and the "songs only" filter are the same ones SimpMusic uses.
 */
object YtApi {
    const val WEB_UA =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"

    private const val CLIENT_NAME = "WEB_REMIX"
    private const val CLIENT_VERSION = "1.20260304.03.00"
    private const val VISITOR_DATA = "CgtsZG1ySnZiQWtSbyiMjuGSBg%3D%3D"
    private const val FILTER_SONG = "EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D"
    private const val FILTER_VIDEO = "EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D"
    private const val SEARCH_URL = "https://music.youtube.com/youtubei/v1/search?prettyPrint=false"

    val http: OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .build()

    fun searchVideos(query:String):List<Song> = search(query,FILTER_VIDEO)

    /** Live broadcasts (24/7 radio, lofi...). YouTube marks them with a LIVE badge on video rows. */
    fun searchLive(): List<Song> {
        val seen = HashSet<String>()
        val out = ArrayList<Song>()
        for (q in listOf("live radio", "lofi live", "bollywood live radio", "bhajan live")) {
            val found =
                try {
                    search(q, FILTER_VIDEO)
                } catch (e: IOException) {
                    continue
                }
            for (song in found) {
                if (song.live && seen.add(song.id)) out.add(song)
            }
        }
        return out
    }

    @Throws(IOException::class)
    fun search(
        query: String,
        filter: String = FILTER_SONG,
        limit: Int = 200,
    ): List<Song> {
        val client =
            JSONObject()
                .put("clientName", CLIENT_NAME)
                .put("clientVersion", CLIENT_VERSION)
                .put("gl", listOf("IN", "US", "GB", "JP", "KR", "DE")[LibraryStore.option("country").coerceIn(0,5)])
                .put("hl", "en")
                .put("visitorData", VISITOR_DATA)
                .put("userAgent", WEB_UA)
        val body =
            JSONObject()
                .put("context", JSONObject().put("client", client))
                .put("query", query)
                .put("params", filter)

        val request =
            Request
                .Builder()
                .url(SEARCH_URL)
                .header("User-Agent", WEB_UA)
                .header("X-Goog-Api-Format-Version", "1")
                .header("X-YouTube-Client-Name", "1")
                .header("X-YouTube-Client-Version", CLIENT_VERSION)
                .header("x-origin", "https://music.youtube.com")
                .header("Referer", "https://music.youtube.com/")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val source = response.body?.source() ?: throw IOException("Empty response")
            val budget = Batch14.mediaBudget(limit)
            if (source.request(budget+1)) throw IOException("Search response exceeds configured safety limit")
            val text = source.readUtf8()
            return parse(JSONObject(text),limit.coerceIn(1,200))
        }
    }

    private fun parse(root: JSONObject, limit:Int): List<Song> {
        val out = ArrayList<Song>()
        val sections =
            root
                .optJSONObject("contents")
                ?.optJSONObject("tabbedSearchResultsRenderer")
                ?.optJSONArray("tabs")
                ?.optJSONObject(0)
                ?.optJSONObject("tabRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")
                ?: return out

        val explicitIds=mutableSetOf<String>()
        sectionsLoop@ for (i in 0 until sections.length()) {
            val items =
                sections
                    .optJSONObject(i)
                    ?.optJSONObject("musicShelfRenderer")
                    ?.optJSONArray("contents")
                    ?: continue
            for (j in 0 until items.length()) {
                val renderer =
                    items
                        .optJSONObject(j)
                        ?.optJSONObject("musicResponsiveListItemRenderer")
                        ?: continue
                toSong(renderer)?.let { song ->
                    out.add(song)
                    val badges=renderer.optJSONArray("badges")
                    for (n in 0 until (badges?.length()?:0)) {
                        val icon=badges?.optJSONObject(n)?.optJSONObject("musicInlineBadgeRenderer")?.optJSONObject("icon")?.optString("iconType").orEmpty()
                        if(Batch10.explicitBadge(icon))explicitIds.add(song.id)
                    }
                }
                if(out.size>=limit)break@sectionsLoop
            }
        }
        LibraryStore.providerExplicit(explicitIds)
        return out
    }

    private fun runsOf(
        renderer: JSONObject,
        column: Int,
    ): List<String> {
        val runs =
            renderer
                .optJSONArray("flexColumns")
                ?.optJSONObject(column)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
                ?: return emptyList()
        return (0 until runs.length()).mapNotNull { runs.optJSONObject(it)?.optString("text") }
    }

    private fun videoIdOf(renderer: JSONObject): String? {
        val direct = renderer.optJSONObject("playlistItemData")?.optString("videoId")
        if (!direct.isNullOrEmpty()) return direct
        val overlay =
            renderer
                .optJSONObject("overlay")
                ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")
                ?.optJSONObject("watchEndpoint")
                ?.optString("videoId")
        return overlay?.takeIf { it.isNotEmpty() }
    }

    private val durationRegex = Regex("^\\d+(:\\d{2}){1,2}$")

    private fun toSong(renderer: JSONObject): Song? {
        val id = videoIdOf(renderer) ?: return null
        val title = runsOf(renderer, 0).firstOrNull()?.takeIf { it.isNotBlank() } ?: return null

        // second line looks like: "Artist • Album • 4:28"
        val parts =
            runsOf(renderer, 1)
                .joinToString("")
                .split(" • ")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        val duration = parts.lastOrNull()?.takeIf { durationRegex.matches(it) } ?: ""
        val artist = parts.firstOrNull { it != duration && !it.equals("Song", ignoreCase = true) } ?: ""

        val thumbs =
            renderer
                .optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
        val thumb = thumbs?.optJSONObject(thumbs.length() - 1)?.optString("url").orEmpty()

        val badges = renderer.optJSONArray("badges")
        var live = false
        if (badges != null) {
            for (k in 0 until badges.length()) {
                if (badges.optJSONObject(k)?.has("liveBadgeRenderer") == true) live = true
            }
        }

        val runs=renderer.optJSONArray("flexColumns")?.optJSONObject(1)?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")?.optJSONObject("text")?.optJSONArray("runs")
        val album=(0 until (runs?.length()?:0)).mapNotNull{i->val run=runs?.optJSONObject(i)?:return@mapNotNull null;val page=run.optJSONObject("navigationEndpoint")?.optJSONObject("browseEndpoint")?.optJSONObject("browseEndpointContextSupportedConfigs")?.optJSONObject("browseEndpointContextMusicConfig")?.optString("pageType");if(page=="MUSIC_PAGE_TYPE_ALBUM")run.optString("text")else null}.firstOrNull().orEmpty()
        return Song(id, title, artist, duration, bigger(thumb), live, album=album)
    }

    private fun bigger(url: String): String = url.replace(Regex("=w\\d+-h\\d+"), "=w300-h300")
}
