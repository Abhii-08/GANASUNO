package com.abhi.ganasuno.ui
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.abhi.ganasuno.data.Batch10
import java.io.ByteArrayOutputStream
object PlaylistCover {
    fun decode(c:Context,uri:Uri):Bitmap {
        val opts=BitmapFactory.Options().apply{inJustDecodeBounds=true}
        (c.contentResolver.openInputStream(uri)?:error("Cannot open image")).use{BitmapFactory.decodeStream(it,null,opts)}
        require(opts.outWidth>0&&opts.outHeight>0);var sample=1
        while(maxOf(opts.outWidth,opts.outHeight)/sample>1600)sample*=2
        return c.contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply{inSampleSize=sample})}?:error("Invalid image")
    }
    fun crop(bitmap:Bitmap,anchor:Float):Bitmap{val box=Batch10.crop(bitmap.width,bitmap.height,anchor,anchor);return Bitmap.createScaledBitmap(Bitmap.createBitmap(bitmap,box[0],box[1],box[2],box[2]),384,384,true)}
    fun encode(bitmap:Bitmap):String=Base64.encodeToString(ByteArrayOutputStream().apply{check(bitmap.compress(Bitmap.CompressFormat.JPEG,82,this))}.toByteArray(),Base64.NO_WRAP).also{require(it.length<=300000)}
    fun fromStored(raw:String):Bitmap?=runCatching{if(raw.isBlank()||raw.length>300000)return null;val b=Base64.decode(raw,Base64.DEFAULT);BitmapFactory.decodeByteArray(b,0,b.size)}.getOrNull()
}
