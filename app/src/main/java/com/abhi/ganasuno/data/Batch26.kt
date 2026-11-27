package com.abhi.ganasuno.data

import java.net.URI
import java.security.MessageDigest

object Batch26 {
    fun eligible(songs:List<Song>)=songs.filter{it.localUri.isBlank()&&!it.live&&it.id.matches(Regex("[A-Za-z0-9_-]{11}"))}.map{it.id}.distinct()
    fun missing(local:List<String>,remote:Collection<String>):List<String>{val present=remote.toHashSet();return local.distinct().filterNot{it in present}}
    fun rating(value:String)=value in setOf("like","dislike","none")
    fun https(raw:String):String?=runCatching{val u=URI(raw.trim());require(u.scheme=="https"&&!u.host.isNullOrBlank()&&u.rawUserInfo==null&&u.rawFragment==null&&u.port in listOf(-1,443));u.toASCIIString()}.getOrNull()
    data class Post(val id:String,val title:String,val link:String)
    fun post(id:String,title:String,link:String):Post? {
        val url=https(link)?:return null;val t=title.filterNot{it.isISOControl()}.trim().take(300);if(t.isBlank())return null
        val key=MessageDigest.getInstance("SHA-256").digest(id.trim().ifBlank{url}.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it.toInt() and 255)}
        return Post(key,t,url)
    }
    fun newPosts(rows:List<Post>,seen:Set<String>)=if(seen.isEmpty())emptyList()else rows.distinctBy{it.id}.filterNot{it.id in seen}
    fun seen(rows:List<Post>,old:List<String>)=(rows.map{it.id}+old).distinct().take(500)
}
