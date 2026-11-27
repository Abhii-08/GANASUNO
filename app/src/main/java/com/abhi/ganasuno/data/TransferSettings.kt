package com.abhi.ganasuno.data

import org.json.JSONObject

/** Portable non-secret settings only. No account, network, history or file permissions. */
object TransferSettings {
    private val limits = linkedMapOf<String, Iterable<Int>>(
        "theme" to setOf(-1,1,2), "palette" to (0..6), "custom_color" to (0xff000000.toInt()..-1),
        "player_style" to (0..3), "skip_explicit" to (0..1), "animated_art" to (0..2), "rtl_preview" to (0..2), "country" to (0..5),
        "home_limit" to (10..100), "home_source" to (0..1), "video_mode" to (0..1), "media_cache" to (0..1), "media_cache_mib" to Batch25.cacheChoices, "sponsor_enabled" to (0..1), "sponsor_categories" to (0..511), "backup_audio" to (0..1), "repeat" to (0..2),
        "crossfade_seconds" to (0..15), "keep_album_continuous" to (0..1), "dj_transition" to (0..1), "shuffle" to setOf(0,1), "endless" to setOf(0,1), "preload" to (0..2),
        "preamp" to (-12..6), "lyric_offset" to (-30000..30000),
        "manual_fade" to (100..30000), "sleep_fade" to (2000..15000), "stop_on_exit" to setOf(0,1),
        "rotate" to setOf(0,1), "rotate_period" to (3..30), "rotate_direction" to setOf(-1,1), "delay_ms" to (0..500), "delay_feedback" to (0..65), "delay_mix" to (0..45),
        "lyrics_provider" to (0..2), "reverb" to (0..7), "reverb_mix" to (0..100), "dsp_eq" to (0..1), "dsp_headroom" to (-24..0),
        *Array(10){"dsp_band_$it" to (-12..12)}
    )
    private val defaults = mapOf("keep_album_continuous" to 1,"custom_color" to 0xff7c3aed.toInt(),"dsp_headroom" to -6,"theme" to -1,"home_limit" to 30,"media_cache_mib" to 64,"sponsor_categories" to 1,"manual_fade" to 1000,"sleep_fade" to 8000,"reverb_mix" to 25,"rotate_period" to 8,"rotate_direction" to 1,"delay_feedback" to 20,"delay_mix" to 20)
    private fun allowed(n:Int,valid:Iterable<Int>)=if(valid is IntRange)n in valid else n in valid
    fun export(): String {
        val options=JSONObject()
        limits.forEach { (k,valid) -> val n=LibraryStore.option(k,defaults[k]?:0); if(allowed(n,valid)) options.put(k,n) }
        return JSONObject().put("schema",1).put("options",options).put("quality",Prefs.audioQuality)
            .put("language",Prefs.audioLang).put("videoHeight",Prefs.videoHeight)
            .put("skipSilence",Prefs.skipSilence).put("normalize",Prefs.normalize).toString(2)
    }
    fun restore(raw:String, apply:Boolean=true) {
        require(raw.length<=50000) { "Settings document too large" }
        val o=JSONObject(raw);require(o.getInt("schema")==1)
        val options=o.getJSONObject("options")
        val values=limits.mapNotNull { (k,valid) ->
            if(!options.has(k)) null else { val n=options.getInt(k);require(allowed(n,valid)){"Invalid $k"};k to n }
        }
        val quality=o.getInt("quality");require(quality in 0..3)
        val language=o.getString("language");require(language.matches(Regex("[A-Za-z-]{0,20}")))
        val height=o.getInt("videoHeight");require(height in setOf(360,480,720,1080))
        val silence=o.getBoolean("skipSilence");val normalize=o.getBoolean("normalize")
        if(!apply)return
        values.forEach{(k,v)->LibraryStore.option(k,v,true)}
        Prefs.audioQuality=quality;Prefs.audioLang=language;Prefs.videoHeight=height;Prefs.skipSilence=silence;Prefs.normalize=normalize
    }
}
