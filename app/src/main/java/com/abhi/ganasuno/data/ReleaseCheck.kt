package com.abhi.ganasuno.data

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.json.JSONObject

/** Opt-in metadata checks. Never downloads or installs an APK automatically. */
object ReleaseCheck {
    const val OFFICIAL_URL = "https://api.github.com/repos/Abhii-08/GANASUNO/releases/latest"
    data class Release(val versionCode:Int,val versionName:String,val url:String) {
        fun isNewerThan(installedVersionCode: Long): Boolean = versionCode.toLong() > installedVersionCode
    }

    internal fun officialApkUrl(tag: String, assets: Sequence<Pair<String, String>>): String {
        require(Regex("v[0-9]+(\\.[0-9]+)+").matches(tag)) { "Invalid release tag" }
        val expected = "https://github.com/Abhii-08/GANASUNO/releases/download/$tag/GANASUNO.apk"
        require(assets.any { (name, url) -> name == "GANASUNO.apk" && url == expected }) {
            "Release APK missing"
        }
        return expected
    }
    fun check(raw:String,channel:Int):Release {
        val url=raw.trim().toHttpUrlOrNull()?:error("Invalid manifest URL")
        require(url.isHttps && url.username.isEmpty() && url.password.isEmpty()){"HTTPS manifest required"}
        val client=YtApi.http.newBuilder().followRedirects(false).followSslRedirects(false).build()
        client.newCall(Request.Builder().url(url).build()).execute().use{r->
            check(r.isSuccessful){"Update server HTTP ${r.code}"};val body=r.body?:error("No update data");val source=body.source();source.request(65537);require(source.buffer.size<=65536){"Manifest too large"}
            val json=JSONObject(source.readUtf8())
            if (url.toString() == OFFICIAL_URL) {
                require(channel == 0) { "Official nightly channel is not published" }
                require(!json.optBoolean("draft") && !json.optBoolean("prerelease"))
                val code=Regex("(?m)^versionCode: ([0-9]+)\\s*$").find(json.optString("body"))?.groupValues?.get(1)?.toIntOrNull() ?: error("Missing release version code")
                require(code > 0)
                val tag=json.getString("tag_name")
                val assets=json.getJSONArray("assets")
                val links = (0 until assets.length()).asSequence().map { index ->
                    val asset = assets.getJSONObject(index)
                    asset.optString("name") to asset.optString("browser_download_url")
                }
                return Release(code, tag.removePrefix("v"), officialApkUrl(tag, links))
            }
            val o=json.getJSONObject(if(channel==1)"nightly" else "stable")
            val link=o.getString("url").toHttpUrlOrNull()?:error("Invalid release link");require(link.isHttps && link.username.isEmpty() && link.password.isEmpty())
            val code=o.getInt("versionCode");require(code>0)
            return Release(code,o.getString("versionName").take(80),link.toString())
        }
    }
}
