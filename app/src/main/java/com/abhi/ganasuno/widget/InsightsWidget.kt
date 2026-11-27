package com.abhi.ganasuno.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.abhi.ganasuno.MainActivity
import com.abhi.ganasuno.R
import com.abhi.ganasuno.data.LibraryStore

class InsightsWidget: AppWidgetProvider() {
    override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray) {
        val history=LibraryStore.history(7)
        val top=history.groupBy{it.song.artist.ifBlank{"Unknown"}}.maxByOrNull{it.value.sumOf{entry->entry.ms}}?.key
        val summary="${history.sumOf{it.ms}/60000} min · ${history.map{it.song.id}.distinct().size} tracks"
        for(id in ids){
            val v=RemoteViews(context.packageName,R.layout.widget_library)
            v.setTextViewText(R.id.widget_summary,"Last 7 days · $summary")
            v.setTextViewText(R.id.widget_names,top?.let{"Top artist: $it"}?:"Play music to see insights")
            val i=Intent(context,MainActivity::class.java).setAction("com.abhi.ganasuno.HISTORY")
            v.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(context,id,i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            manager.updateAppWidget(id,v)
        }
    }
}
