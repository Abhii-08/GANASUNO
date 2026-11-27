package com.abhi.ganasuno.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Local, private library. Mutations are serialized; backup restores only validated schema. */
object LibraryStore {
    private lateinit var ctx: Context
    private val prefs get() = ctx.getSharedPreferences("library_v2", Context.MODE_PRIVATE)
    fun init(context: Context) { ctx = context.applicationContext }
    fun encode(s: Song) = JSONObject().put("id", s.id).put("t", s.title).put("a", s.artist)
        .put("d", s.duration).put("th", s.thumb).put("l", s.live).put("uri", s.localUri).put("album",s.album)
    fun decode(o: JSONObject): Song {
        val id=o.getString("id"); val uri=o.optString("uri")
        require(if(uri.isBlank()) id.matches(Regex("[A-Za-z0-9_-]{11}")) else uri.startsWith("content://") && id=="local:$uri") { "Invalid media reference" }
        val thumb=o.optString("th").takeIf { it.startsWith("https://") }.orEmpty()
        return Song(id,o.getString("t"),o.optString("a"),o.optString("d"),thumb,o.optBoolean("l"),uri,o.optString("album"))
    }
    private fun root(): JSONObject = runCatching { JSONObject(prefs.getString("data", "{}") ?: "{}") }.getOrDefault(JSONObject())
    private fun save(o: JSONObject) { prefs.edit().putString("data", o.toString()).apply() }
    private fun arr(o: JSONObject, key: String): JSONArray = o.optJSONArray(key) ?: JSONArray().also { o.put(key,it) }
    private fun songs(a: JSONArray): List<Song> = (0 until a.length()).mapNotNull { i -> runCatching { decode(a.getJSONObject(i)) }.getOrNull() }
    private fun encodeAll(s: List<Song>) = JSONArray().apply { s.forEach { put(encode(it)) } }
    @Synchronized fun list(key: String): List<Song> = songs(arr(root(), key))
    private fun markAdded(o:JSONObject,scope:String,id:String){val all=o.optJSONObject("added_v9")?:JSONObject().also{o.put("added_v9",it)};val scopeMap=all.optJSONObject(scope)?:JSONObject().also{all.put(scope,it)};if(!scopeMap.has(id))scopeMap.put(id,System.currentTimeMillis())}
    private fun removeAdded(o:JSONObject,scope:String,id:String){o.optJSONObject("added_v9")?.optJSONObject(scope)?.remove(id)}
    @Synchronized fun ordered(items:List<Song>,mode:Int,scope:String=""):List<Song>{val o=root();val all=o.optJSONObject("added_v9");val times=items.associate{song->song.id to (all?.optJSONObject(scope)?.optLong(song.id,0L)?:0L).let{t->if(scope.isNotBlank()||t!=0L)t else listOf("favorites","local").maxOf{all?.optJSONObject(it)?.optLong(song.id,0L)?:0L}}};val recent=history().groupBy{it.song.id}.mapValues{it.value.maxOf{v->v.at}};return BatchCore.order(items,mode,times,recent)}
    @Synchronized fun sortPlaylistMode(name:String,mode:Int){val o=root();val pl=playlists(o);if(!pl.has(name))return;pl.put(name,encodeAll(ordered(songs(arr(pl,name)),mode,"playlist:$name")));save(o)}
    @Synchronized fun toggleFavorite(s: Song): Boolean {
        val o = root(); val list = songs(arr(o,"favorites")); val adding = list.none { it.id == s.id }
        if(adding)markAdded(o,"favorites",s.id)else removeAdded(o,"favorites",s.id);o.put("favorites", encodeAll(if (adding) listOf(s)+list else list.filterNot { it.id == s.id })); save(o); if(adding)runCatching{SmartOffline.kick(ctx)}; return adding
    }
    @Synchronized fun addFavorites(items:List<Song>) { val o=root();items.forEach{markAdded(o,"favorites",it.id)};o.put("favorites",encodeAll((items+songs(arr(o,"favorites"))).distinctBy{it.id}));save(o);runCatching{SmartOffline.kick(ctx)} }
    @Synchronized fun importRemoteFavorites(remote:List<Song>):Int{val o=root();val old=songs(arr(o,"favorites"));val merged=Batch27.merge(old,remote);val ids=old.map{it.id}.toHashSet();merged.filterNot{it.id in ids}.forEach{markAdded(o,"favorites",it.id)};o.put("favorites",encodeAll(merged));save(o);runCatching{SmartOffline.kick(ctx)};return merged.size-old.size}
    @Synchronized fun addLocal(s: Song) { val o=root();markAdded(o,"local",s.id); o.put("local",encodeAll((listOf(s)+songs(arr(o,"local"))).distinctBy { it.id })); save(o) }
    @Synchronized fun removeLocal(s: Song) { val o=root();removeAdded(o,"local",s.id); o.put("local",encodeAll(songs(arr(o,"local")).filterNot { it.id==s.id })); save(o) }
    private fun playlists(o: JSONObject) = o.optJSONObject("playlists") ?: JSONObject().also { o.put("playlists",it) }
    @Synchronized fun names(): List<String> = playlists(root()).keys().asSequence().toList().sorted()
    @Synchronized fun create(name: String): Boolean { val n=name.trim(); if(n.isEmpty() || n.length>100) return false; val o=root(); val p=playlists(o); if(p.has(n)) return false; p.put(n,JSONArray());save(o);return true }
    @Synchronized fun remotePlaylist(name:String):String? = root().optJSONObject("remote_playlists_v26")?.optString(name)?.takeIf{Batch16.valid("playlist",it)}
    @Synchronized fun linkPlaylist(name:String,id:String?):Boolean {val o=root();if(!playlists(o).has(name))return false;require(id==null||Batch16.valid("playlist",id));val all=o.optJSONObject("remote_playlists_v26")?:JSONObject().also{o.put("remote_playlists_v26",it)};if(id==null)all.remove(name)else all.put(name,id);save(o);return true}
    @Synchronized fun playlist(name: String) = songs(arr(playlists(root()),name))
    @Synchronized fun add(name: String,s: Song) { val o=root();val p=playlists(o);markAdded(o,"playlist:$name",s.id);p.put(name,encodeAll((songs(arr(p,name))+s).distinctBy { it.id }));save(o) }
    @Synchronized fun mergeRemote(name:String,expectedId:String,remote:List<Song>):Int {
        val o=root();val p=playlists(o);require(p.has(name)&&o.optJSONObject("remote_playlists_v26")?.optString(name)==expectedId){"Playlist association changed"}
        val old=songs(arr(p,name));val merged=Batch27.merge(old,remote);val oldIds=old.map{it.id}.toHashSet();merged.filterNot{it.id in oldIds}.forEach{markAdded(o,"playlist:$name",it.id)};p.put(name,encodeAll(merged));save(o);return merged.size-old.size
    }
    @Synchronized fun remove(name: String,s: Song) { val o=root();val p=playlists(o);removeAdded(o,"playlist:$name",s.id);p.put(name,encodeAll(songs(arr(p,name)).filterNot { it.id==s.id }));save(o) }
    @Synchronized fun move(name:String,id:String,delta:Int):Boolean {
        val o=root();val p=playlists(o);if(!p.has(name))return false
        val list=songs(arr(p,name)).toMutableList();val from=list.indexOfFirst{it.id==id};val to=from+delta
        if(from<0||to !in list.indices)return false
        list.add(to,list.removeAt(from));p.put(name,encodeAll(list));save(o);return true
    }
    @Synchronized fun sortPlaylist(name:String,byArtist:Boolean){val o=root();val p=playlists(o);if(!p.has(name))return;val list=songs(arr(p,name));p.put(name,encodeAll(list.sortedBy{if(byArtist)it.artist.lowercase() else it.title.lowercase()}));save(o)}
    data class SavedAlbum(val title:String,val artist:String)
    @Synchronized fun savedAlbums():List<SavedAlbum>{val a=root().optJSONObject("favorite_albums_v18")?:return emptyList();return a.keys().asSequence().mapNotNull{k->a.optJSONObject(k)?.let{SavedAlbum(it.optString("title"),it.optString("artist"))} }.filter{it.title.isNotBlank()}.sortedBy{it.title.lowercase()}.toList()}
    @Synchronized fun toggleSavedAlbum(title:String,artist:String):Boolean{require(title.isNotBlank()&&title.length<=200&&artist.length<=300);val o=root();val a=o.optJSONObject("favorite_albums_v18")?:JSONObject().also{o.put("favorite_albums_v18",it)};val k=Batch18.albumKey(title,artist);val adding=!a.has(k);if(adding){require(a.length()<500);a.put(k,JSONObject().put("title",title).put("artist",artist))}else a.remove(k);save(o);return adding}
    @Synchronized fun savedPlaylists():List<String>{val o=root();val a=o.optJSONObject("favorite_playlists_v18")?:return emptyList();return a.keys().asSequence().filter{a.optBoolean(it)&&playlists(o).has(it)}.sorted().toList()}
    @Synchronized fun toggleSavedPlaylist(name:String):Boolean{val o=root();require(playlists(o).has(name));val a=o.optJSONObject("favorite_playlists_v18")?:JSONObject().also{o.put("favorite_playlists_v18",it)};val adding=!a.optBoolean(name);if(adding)a.put(name,true)else a.remove(name);save(o);return adding}
    @Synchronized fun delete(name: String) { val o=root();playlists(o).remove(name);o.optJSONObject("remote_playlists_v26")?.remove(name);o.optJSONObject("favorite_playlists_v18")?.remove(name);o.optJSONObject("covers_v10")?.remove(name);o.optJSONObject("added_v9")?.remove("playlist:$name");save(o) }
    @Synchronized fun rename(old: String,new: String): Boolean { val n=new.trim();val o=root();val p=playlists(o);if(n.isEmpty()||n.length>100||p.has(n)||!p.has(old))return false;p.put(n,p.getJSONArray(old));p.remove(old);o.optJSONObject("remote_playlists_v26")?.let{all->all.remove(old)?.let{all.put(n,it)}};o.optJSONObject("favorite_playlists_v18")?.let{a->if(a.optBoolean(old)){a.remove(old);a.put(n,true)}};o.optJSONObject("covers_v10")?.let{all->all.remove(old)?.let{all.put(n,it)}};o.optJSONObject("added_v9")?.let{all->all.remove("playlist:$old")?.let{all.put("playlist:$n",it)}};save(o);return true }
    @Synchronized fun query(q: String) {
        val o=root(); val recent=(listOf(q)+searches().filterNot { it.equals(q,true) }).take(50)
        val old=o.optJSONObject("search_counts_v31"); val counts=JSONObject()
        val key=q.lowercase(java.util.Locale.ROOT)
        recent.forEach { term -> val k=term.lowercase(java.util.Locale.ROOT); val n=(old?.optInt(k,1) ?: 1).coerceIn(1,999999); counts.put(k,if(k==key)n+1 else n) }
        if(old?.has(key)!=true && searches().none{it.equals(q,true)})counts.put(key,1)
        o.put("search_counts_v31",counts);o.put("searches",JSONArray(recent));save(o)
    }
    @Synchronized fun frequentSearches():List<String>{val counts=root().optJSONObject("search_counts_v31");return searches().sortedByDescending{counts?.optInt(it.lowercase(java.util.Locale.ROOT),1)?:1}}
    @Synchronized fun searches(): List<String> { val a=arr(root(),"searches");return (0 until a.length()).map { a.getString(it) } }
    @Synchronized fun clearSearch() {val o=root();o.remove("searches");o.remove("search_counts_v31");save(o)}
    @Synchronized fun lyric(id:String):String = root().optJSONObject("lyrics")?.optString(id).orEmpty()
    @Synchronized fun setLyric(id:String,text:String) {val o=root();val l=o.optJSONObject("lyrics")?:JSONObject().also{o.put("lyrics",it)};l.put(id,text);save(o)}
    @Synchronized fun albumTags():Map<String,String>{val all=root().optJSONObject("albums_v9")?:return emptyMap();return all.keys().asSequence().associateWith{all.optString(it)}}
    @Synchronized fun setAlbum(id:String,name:String){val o=root();val all=o.optJSONObject("albums_v9")?:JSONObject().also{o.put("albums_v9",it)};if(name.isBlank())all.remove(id)else all.put(id,name.trim().take(200));save(o)}
    @Synchronized fun record(s: Song, ms: Long) { if(ms<=0)return; val o=root();val a=arr(o,"history");a.put(encode(s).put("at",System.currentTimeMillis()).put("ms",ms)); val trimmed=JSONArray();for(i in (a.length()-20000).coerceAtLeast(0) until a.length())trimmed.put(a.get(i));o.put("history",trimmed);save(o) }
    data class Listen(val song: Song,val at: Long,val ms: Long)
    @Synchronized fun history(days:Int=0): List<Listen> {val a=arr(root(),"history");val cutoff=if(days==0)0L else System.currentTimeMillis()-days*86400000L;return (0 until a.length()).mapNotNull { i -> runCatching {val o=a.getJSONObject(i);Listen(decode(o),o.getLong("at"),o.getLong("ms"))}.getOrNull() }.filter{it.at>=cutoff} }
    @Synchronized fun clearHistory(){val o=root();o.remove("history");o.remove("plays_v10");save(o)}
    @Synchronized fun backup() = JSONObject().put("schema",1).put("library",root()).toString(2)
    fun validateBackup(raw:String):JSONObject {val b=JSONObject(raw);require(b.getInt("schema")==1);val o=b.getJSONObject("library");for(k in listOf("favorites","local","history")) {if(o.has(k)){val a=o.getJSONArray(k);for(i in 0 until a.length())decode(a.getJSONObject(i))}};val p=o.optJSONObject("playlists");p?.keys()?.forEach { k -> val a=p.getJSONArray(k);for(i in 0 until a.length())decode(a.getJSONObject(i)) };return o}
    @Synchronized fun restore(raw:String){save(validateBackup(raw))}
    @Synchronized fun followed():List<String> {val a=arr(root(),"followed");return (0 until a.length()).map{a.getString(it)}}
    @Synchronized fun setArtistFollowing(name:String,follow:Boolean){val n=name.trim();require(n.isNotBlank()&&n.length<=200);val o=root();val old=followed();val list=if(follow)(old+n).distinct() else old.filterNot{it==n};o.put("followed",JSONArray(list));save(o)}
    @Synchronized fun toggleArtist(name:String):Boolean {val n=name.trim();if(n.isBlank())return false;val o=root();val list=followed();val add=n !in list;o.put("followed",JSONArray(if(add)list+n else list.filterNot{it==n}));save(o);return add}
    @Synchronized fun note(id:String):String=root().optJSONObject("notes")?.optString(id).orEmpty()
    @Synchronized fun setNote(id:String,value:String){val o=root();val n=o.optJSONObject("notes")?:JSONObject().also{o.put("notes",it)};n.put(id,value.take(4000));save(o)}
    @Synchronized fun rating(id:String):Int=root().optJSONObject("ratings")?.optInt(id,0)?:0
    @Synchronized fun rate(id:String,value:Int){val o=root();val n=o.optJSONObject("ratings")?:JSONObject().also{o.put("ratings",it)};n.put(id,value.coerceIn(-1,1));save(o)}
    @Synchronized fun releaseYears():Map<String,Int>{val a=root().optJSONObject("years")?:return emptyMap();return a.keys().asSequence().mapNotNull{k->a.optInt(k,0).takeIf{it in 1900..2100}?.let{k to it}}.toMap()}
    @Synchronized fun releaseYear(id:String):Int?=root().optJSONObject("years")?.optInt(id,0)?.takeIf{it in 1900..2100}
    @Synchronized fun setReleaseYear(id:String,year:Int?){require(year==null||year in 1900..2100);val o=root();val a=o.optJSONObject("years")?:JSONObject().also{o.put("years",it)};if(year==null)a.remove(id)else a.put(id,year);save(o)}
    data class Marker(val ms:Long,val text:String,val reaction:String)
    @Synchronized fun markers(id:String):List<Marker> {val a=root().optJSONObject("markers")?.optJSONArray(id)?:return emptyList();return (0 until a.length()).mapNotNull{i->runCatching{val x=a.getJSONObject(i);Marker(x.getLong("ms"),x.getString("text"),x.optString("reaction"))}.getOrNull()}.sortedBy{it.ms}}
    @Synchronized fun addMarker(id:String,ms:Long,text:String,reaction:String){val o=root();val all=o.optJSONObject("markers")?:JSONObject().also{o.put("markers",it)};val a=all.optJSONArray(id)?:JSONArray();require(a.length()<200){"Maximum 200 markers per track"};a.put(JSONObject().put("ms",ms.coerceAtLeast(0)).put("text",text.take(250)).put("reaction",reaction.take(12)));all.put(id,a);save(o)}
    @Synchronized fun removeMarker(id:String,marker:Marker){val o=root();val a=o.optJSONObject("markers")?.optJSONArray(id)?:return;for(i in 0 until a.length()){val v=a.optJSONObject(i)?:continue;if(v.optLong("ms")==marker.ms&&v.optString("text")==marker.text&&v.optString("reaction")==marker.reaction){a.remove(i);save(o);return}}}
    @Synchronized fun clearMarkers(id:String){val o=root();o.optJSONObject("markers")?.remove(id);save(o)}
    data class MusicTags(val bpm:Int?,val key:String?)
    @Synchronized fun musicTags(id:String):MusicTags {val a=root().optJSONObject("music_tags_v19")?.optJSONObject(id);return MusicTags(a?.optInt("bpm",0)?.takeIf{it in 20..300},Batch19.key(a?.optString("key").orEmpty()))}
    @Synchronized fun setMusicTags(id:String,bpm:Int?,key:String?){require(bpm==null||bpm in 20..300);require(key==null||Batch19.key(key)==key);val o=root();val all=o.optJSONObject("music_tags_v19")?:JSONObject().also{o.put("music_tags_v19",it)};if(bpm==null&&key==null)all.remove(id)else all.put(id,JSONObject().apply{bpm?.let{put("bpm",it)};key?.let{put("key",it)}});save(o)}
    fun option(key:String,default:Int=0)=prefs.getInt(key,default)
    data class CollectionRef(val kind:String,val title:String,val url:String)
    @Synchronized fun collections():List<CollectionRef> {val a=arr(root(),"collections_v8");return (0 until a.length()).mapNotNull{i->runCatching{val v=a.getJSONObject(i);CollectionRef(v.getString("kind"),v.getString("title"),v.getString("url"))}.getOrNull()}.filter{it.kind in setOf("Playlist","Album","Podcast") && V8Core.httpsUrl(it.url)!=null}}
    @Synchronized fun saveCollection(kind:String,title:String,url:String) {require(kind in setOf("Playlist","Album","Podcast"));require(title.isNotBlank()&&title.length<=120);require(V8Core.httpsUrl(url)!=null);val o=root();val all=(listOf(CollectionRef(kind,title,url))+collections().filterNot{it.kind==kind&&it.url==url}).take(500);o.put("collections_v8",JSONArray().apply{all.forEach{put(JSONObject().put("kind",it.kind).put("title",it.title).put("url",it.url))}});save(o)}
    @Synchronized fun deleteCollection(ref:CollectionRef){val o=root();o.put("collections_v8",JSONArray().apply{collections().filterNot{it==ref}.forEach{put(JSONObject().put("kind",it.kind).put("title",it.title).put("url",it.url))}});save(o)}
    @Synchronized fun explicit(id:String):Boolean=root().optJSONObject("explicit_v8")?.optBoolean(id,false)?:false
    @Synchronized fun toggleExplicit(id:String):Boolean{val o=root();val x=o.optJSONObject("explicit_v8")?:JSONObject().also{o.put("explicit_v8",it)};val marked=!x.optBoolean(id,false);if(marked)x.put(id,true)else x.remove(id);save(o);return marked}
    @Synchronized fun providerExplicit(ids:Set<String>){if(ids.isEmpty())return;val o=root();val x=o.optJSONObject("explicit_provider")?:JSONObject().also{o.put("explicit_provider",it)};var changed=false;ids.forEach{if(!x.optBoolean(it)){x.put(it,true);changed=true}};if(changed)save(o)}
    @Synchronized fun providerExplicit(id:String)=root().optJSONObject("explicit_provider")?.optBoolean(id,false)?:false
    fun allowed(id:String):Boolean=option("skip_explicit")==0||(!explicit(id)&&!providerExplicit(id))
    @Synchronized fun countPlay(song:Song){val o=root();val all=o.optJSONObject("plays_v10")?:JSONObject().also{o.put("plays_v10",it)};val count=(all.optJSONObject(song.id)?.optLong("count",0L)?:0L).coerceIn(0,Long.MAX_VALUE-1);all.put(song.id,JSONObject().put("song",encode(song)).put("count",count+1));save(o)}
    @Synchronized fun mostPlayed():List<Pair<Song,Long>> {val all=root().optJSONObject("plays_v10")?:return emptyList();return Batch10.ranked(all.keys().asSequence().mapNotNull{id->runCatching{val entry=all.getJSONObject(id);decode(entry.getJSONObject("song")) to entry.getLong("count")}.getOrNull()}.toMap())}
    @Synchronized fun cover(name:String):String=root().optJSONObject("covers_v10")?.optString(name).orEmpty()
    @Synchronized fun setCover(name:String,value:String){val o=root();require(playlists(o).has(name));require(value.length<=300000);val all=o.optJSONObject("covers_v10")?:JSONObject().also{o.put("covers_v10",it)};if(value.isEmpty())all.remove(name)else all.put(name,value);save(o)}
    fun option(key:String,value:Int,write:Boolean) { if(write)prefs.edit().putInt(key,value).apply() }
}
