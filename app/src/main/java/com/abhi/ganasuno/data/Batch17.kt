package com.abhi.ganasuno.data

import java.util.Locale
import kotlin.math.sqrt

object Batch17 {
    fun upcoming(count:Int,current:Int,next:(Int)->Int):List<Int>{
        if(current !in 0 until count)return emptyList()
        val seen=mutableSetOf(current);val result=mutableListOf<Int>();var i=current
        while(result.size<count-1){val n=next(i);if(n !in 0 until count||!seen.add(n))break;result.add(n);i=n};return result
    }
    fun queueOrder(count:Int,current:Int,next:(Int)->Int):List<Int>{if(current !in 0 until count)return (0 until count).toList();val first=listOf(current)+upcoming(count,current,next);val seen=first.toSet();return first+(0 until count).filterNot{it in seen}}
    fun nextPosition(index:Int,current:Int,count:Int):Int?{if(index !in 0 until count||current !in 0 until count||index==current)return null;return if(index<current)current else (current+1).coerceAtMost(count-1)}
    private fun normalized(s:String)=s.lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{M}\\p{N}]+")," ").trim()
    private val qualifiers=setOf("official","audio","video","lyrics","lyric","live","acoustic","remix","cover","version","remastered","remaster","instrumental","karaoke","radio","edit","extended","mix","slowed","reverb","sped","up","hd","4k")
    private fun base(s:String):List<String>{val words=normalized(s).split(' ').filter{it.isNotBlank()};return words.dropLastWhile{it in qualifiers}.ifEmpty{words}}
    fun isVersion(original:String,candidate:String):Boolean{val x=base(original);val y=base(candidate);if(x.isEmpty()||y.isEmpty())return false;return x==y}
    fun related(artist:String,groups:List<List<String>>):List<String>{val target=normalized(artist);if(target.isBlank())return emptyList();val names=linkedMapOf<String,String>();val sizes=mutableMapOf<String,Int>();val both=mutableMapOf<String,Int>();var targetCount=0
        groups.forEach{g->val ids=g.map{it.trim()}.filter{normalized(it).isNotBlank()}.associateBy(::normalized);ids.forEach{(k,v)->names.putIfAbsent(k,v);sizes[k]=(sizes[k]?:0)+1};if(target in ids){targetCount++;ids.keys.filter{it!=target}.forEach{k->both[k]=(both[k]?:0)+1}}};if(targetCount==0)return emptyList()
        return both.keys.sortedWith(compareByDescending<String>{both.getValue(it)/sqrt(targetCount.toDouble()*sizes.getValue(it))}.thenByDescending{both.getValue(it)}.thenBy{it}).take(30).map{names.getValue(it)}
    }
    /** Seed-based candidates, favor exact/related artists and avoid duplicate/current IDs. */
    fun radio(seed:Song,candidates:List<Song>,related:List<String>,excluded:Set<String>,limit:Int=20):List<Song>{
        val artist=normalized(seed.artist);val album=normalized(seed.album);val ranks=related.map(::normalized).distinct().withIndex().associate{it.value to it.index}
        return candidates.filter{it.id!=seed.id&&it.id !in excluded&&!it.live}.distinctBy{it.id}.filter{normalized(it.artist)==artist&&artist.isNotBlank()||normalized(it.artist) in ranks||album.isNotBlank()&&normalized(it.album)==album}.sortedWith(compareByDescending<Song>{(if(artist.isNotBlank()&&normalized(it.artist)==artist)100 else 0)+(if(album.isNotBlank()&&normalized(it.album)==album)25 else 0)+(ranks[normalized(it.artist)]?.let{(50-it).coerceAtLeast(1)}?:0)}.thenBy{normalized(it.title)}).take(limit.coerceIn(0,100))
    }

}
