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

/** Tap to open the local playlist library; refreshed periodically by the widget host. */
class LibraryWidget: AppWidgetProvider() {
    companion object {fun refresh(context:Context){val manager=AppWidgetManager.getInstance(context);val ids=manager.getAppWidgetIds(android.content.ComponentName(context,LibraryWidget::class.java));if(ids.isNotEmpty())LibraryWidget().onUpdate(context,manager,ids)}}
    override fun onUpdate(context:Context, manager:AppWidgetManager, ids:IntArray) {
        val names=LibraryStore.names()
        val minutes=LibraryStore.history(7).sumOf{it.ms}/60000
        for(id in ids){
            val views=RemoteViews(context.packageName,R.layout.widget_library)
            views.setTextViewText(R.id.widget_summary,"${names.size} playlists · $minutes min this week")
            views.setTextViewText(R.id.widget_names,if(names.isEmpty())"Create your first playlist" else "Tap a playlist to play")
            val slots=intArrayOf(R.id.widget_playlist1,R.id.widget_playlist2,R.id.widget_playlist3)
            slots.forEachIndexed{index,slot->
                val name=names.getOrNull(index)
                views.setViewVisibility(slot,if(name==null)android.view.View.GONE else android.view.View.VISIBLE)
                if(name!=null){views.setTextViewText(slot,"▶ $name");val play=Intent(context,MainActivity::class.java).setAction("com.abhi.ganasuno.PLAY_WIDGET_PLAYLIST").setData(android.net.Uri.parse("ganasuno-widget://playlist/$id/$index")).putExtra("playlist",name).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP);views.setOnClickPendingIntent(slot,PendingIntent.getActivity(context,id,play,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))}
            }
            val intent=Intent(context,MainActivity::class.java).setAction("com.abhi.ganasuno.LIBRARY")
            views.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            manager.updateAppWidget(id,views)
        }
    }
}
