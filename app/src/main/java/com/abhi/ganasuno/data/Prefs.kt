package com.abhi.ganasuno.data

import android.content.Context
import android.content.SharedPreferences

/** All user settings in one place (SharedPreferences). Call [init] once from the Application. */
object Prefs {
    const val Q_LOW = 0
    const val Q_NORMAL = 1
    const val Q_HIGH = 2
    const val Q_320 = 3

    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.applicationContext.getSharedPreferences("ganasuno_prefs", Context.MODE_PRIVATE)
    }

    /** 0 = low (smallest bitrate), 1 = normal (AAC 128k), 2 = high (best bitrate). */
    var audioQuality: Int
        get() = sp.getInt("audio_quality", Q_NORMAL)
        set(value) = sp.edit().putInt("audio_quality", value).apply()

    /** "" = original track, otherwise a language code like "hi" or "en". */
    var audioLang: String
        get() = sp.getString("audio_lang", "") ?: ""
        set(value) = sp.edit().putString("audio_lang", value).apply()

    /** Max video height for the video player: 360, 480, 720 or 1080. */
    var videoHeight: Int
        get() = sp.getInt("video_height", 1080)
        set(value) = sp.edit().putInt("video_height", value).apply()

    var skipSilence: Boolean
        get() = sp.getBoolean("skip_silence", false)
        set(value) = sp.edit().putBoolean("skip_silence", value).apply()

    var normalize: Boolean
        get() = sp.getBoolean("normalize", false)
        set(value) = sp.edit().putBoolean("normalize", value).apply()

    var queueJson: String?
        get() = sp.getString("queue_json", null)
        set(value) = sp.edit().putString("queue_json", value).apply()
}
