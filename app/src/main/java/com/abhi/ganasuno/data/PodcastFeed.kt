package com.abhi.ganasuno.data

import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/** Public HTTPS RSS 2.0 feeds. Only audio enclosures are exposed; no authentication. */
object PodcastFeed {
    data class Episode(val title:String,val url:String,val published:String)
    fun fetch(raw:String):List<Episode>{val url=raw.trim().toHttpUrlOrNull()?:error("Invalid RSS URL");require(url.isHttps&&url.username.isEmpty()&&url.password.isEmpty());val client=YtApi.http.newBuilder().followRedirects(false).followSslRedirects(false).build();return client.newCall(Request.Builder().url(url).build()).execute().use{r->check(r.isSuccessful){"RSS HTTP ${r.code}"};val source=(r.body?:error("No feed")).source();require(!source.request(2*1024*1024L+1)){"Feed too large"};parse(source.readUtf8())}}
    fun parse(raw:String):List<Episode>{require(raw.length<=2*1024*1024&&!raw.contains("<!DOCTYPE",true)&&!raw.contains("<!ENTITY",true)){"Unsupported XML"};val parser=XmlPullParserFactory.newInstance().apply{isNamespaceAware=true}.newPullParser();parser.setInput(raw.reader());var item=false;var title="";var date="";var audio="";val out=mutableListOf<Episode>();var event=parser.eventType;while(event!=XmlPullParser.END_DOCUMENT&&out.size<100){if(event==XmlPullParser.START_TAG){when(parser.name){"item"->{item=true;title="";date="";audio=""};"title"->if(item)title=parser.nextText().take(300);"pubDate"->if(item)date=parser.nextText().take(100);"enclosure"->if(item){val type=parser.getAttributeValue(null,"type").orEmpty();val u=parser.getAttributeValue(null,"url")?.toHttpUrlOrNull();if(type.startsWith("audio/")&&u?.isHttps==true&&u.username.isEmpty()&&u.password.isEmpty())audio=u.toString()}}}else if(event==XmlPullParser.END_TAG&&parser.name=="item"){if(item&&title.isNotBlank()&&audio.isNotBlank())out.add(Episode(title,audio,date));item=false};event=parser.next()};return out.distinctBy{it.url}}
}
