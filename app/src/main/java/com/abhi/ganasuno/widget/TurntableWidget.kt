package com.abhi.ganasuno.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.abhi.ganasuno.MainActivity
import com.abhi.ganasuno.R
import com.abhi.ganasuno.data.LibraryStore
import com.abhi.ganasuno.player.PlayerHub

class TurntableWidget : AppWidgetProvider() {
    override fun onUpdate(context:Context,manager:AppWidgetManager,ids:IntArray) = update(context,manager,ids)
    companion object {
        fun refresh(context:Context) { val manager=AppWidgetManager.getInstance(context);update(context,manager,manager.getAppWidgetIds(ComponentName(context,TurntableWidget::class.java))) }
        private fun update(context:Context,manager:AppWidgetManager,ids:IntArray) {
            if(ids.isEmpty())return
            val current=PlayerHub.player?.currentMediaItem?.mediaMetadata
            val recent=if(current==null)LibraryStore.history().lastOrNull()?.song else null
            for(id in ids){val view=RemoteViews(context.packageName,R.layout.widget_turntable)
                view.setTextViewText(R.id.record_title,current?.title?:recent?.title?:"Choose a song")
                view.setTextViewText(R.id.record_artist,current?.artist?:recent?.artist?:"GANASUNO")
                val intent=Intent(context,MainActivity::class.java).setAction("com.abhi.ganasuno.TURNTABLE")
                view.setOnClickPendingIntent(R.id.record_root,PendingIntent.getActivity(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                manager.updateAppWidget(id,view)
            }
        }
    }
}
