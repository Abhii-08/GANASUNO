package com.abhi.ganasuno

import android.app.Application
import com.abhi.ganasuno.data.NpDownloader
import com.abhi.ganasuno.data.Prefs
import org.schabi.newpipe.extractor.NewPipe

class GanasunoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        com.abhi.ganasuno.data.CrashRecord.install(this)
        Prefs.init(this)
        com.abhi.ganasuno.data.LibraryStore.init(this)
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(com.abhi.ganasuno.data.LibraryStore.option("theme", 2))
        NewPipe.init(NpDownloader())
        runCatching {
            val updates=com.abhi.ganasuno.data.UpdateScheduler.prefs(this)
            if (!updates.contains("url")) updates.edit().putString("url",com.abhi.ganasuno.data.ReleaseCheck.OFFICIAL_URL).apply()
            com.abhi.ganasuno.data.UpdateScheduler.schedule(this)
        }
        runCatching { com.abhi.ganasuno.data.BlogWatch.schedule(this) }
        runCatching { com.abhi.ganasuno.data.SmartOffline.schedule(this) }
        runCatching { com.abhi.ganasuno.data.VideoOffline.purge(this) }
        runCatching { com.abhi.ganasuno.data.ArtistReleaseWatch.schedule(this) }
        runCatching { com.abhi.ganasuno.data.PublicCatalogue.purge(this) }
    }
}
