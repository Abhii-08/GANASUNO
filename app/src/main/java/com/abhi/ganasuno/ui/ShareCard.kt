package com.abhi.ganasuno.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.File

object ShareCard {
    fun create(c: Context, title: String, text: String, palette: Int): android.net.Uri {
        val colors = when(palette) { 1 -> intArrayOf(0xff164e63.toInt(),0xff0f172a.toInt()); 2 -> intArrayOf(0xff7c2d12.toInt(),0xff292524.toInt()); else -> intArrayOf(0xff312e81.toInt(),0xff111827.toInt()) }
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.WHITE; textSize=35f }
        val body=StaticLayout.Builder.obtain(text.take(1600),0,text.take(1600).length,paint,936).setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(10f,1f).setMaxLines(42).setEllipsize(android.text.TextUtils.TruncateAt.END).build()
        val height=(body.height+290).coerceIn(720,3200)
        val bitmap=Bitmap.createBitmap(1080,height,Bitmap.Config.ARGB_8888)
        try {
            val canvas=Canvas(bitmap);val bg=Paint().apply{shader=LinearGradient(0f,0f,1080f,height.toFloat(),colors,null,Shader.TileMode.CLAMP)};canvas.drawRect(0f,0f,1080f,height.toFloat(),bg)
            val heading=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.WHITE;textSize=47f;typeface=Typeface.DEFAULT_BOLD};canvas.drawText(title.take(35),72f,100f,heading)
            canvas.save();canvas.translate(72f,155f);body.draw(canvas);canvas.restore()
            heading.textSize=24f;canvas.drawText("GANASUNO  •  MUSIC",72f,height-55f,heading)
            val dir=File(c.cacheDir,"share-cards").apply{mkdirs()};dir.listFiles()?.filter{System.currentTimeMillis()-it.lastModified()>86400000L}?.forEach{it.delete()}
            val file=File(dir,java.util.UUID.randomUUID().toString()+".png");file.outputStream().use{check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}
            return FileProvider.getUriForFile(c,"${c.packageName}.offline",file)
        } finally { bitmap.recycle() }
    }
    fun send(c:Context,uri:android.net.Uri){ c.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),"Share image")) }
}
