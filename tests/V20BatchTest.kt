import com.abhi.ganasuno.data.Batch20
fun main(){
 var n=0;fun ck(b:Boolean){check(b);n++}
 val id="abcdefghijk"
 ck(Batch20.lyricKey(id,"[00:00] hi")?.length==64)
 ck(Batch20.lyricKey(id,"a\r\nb")==Batch20.lyricKey(id,"a\nb"));ck(Batch20.lyricKey(id,"a")!=Batch20.lyricKey(id,"b"));ck(Batch20.lyricKey("short","a")==null);ck(Batch20.lyricKey(id," ")==null)
 ck(Batch20.origin("https://example.org/")=="https://example.org");ck(Batch20.origin("https://example.org:443")=="https://example.org")
 for(s in listOf("http://x.com","https://user@x.com","https://x.com/path","https://x.com?key=1","https://x.com#fragment","https://x.com:90"))ck(Batch20.origin(s)==null)
 ck(Batch20.artworkName("../../evil").matches(Regex("[a-f0-9]{64}\\.gif")))
 ck(Batch20.rateLabel(320).contains("Approximately 320"));ck(Batch20.rateLabel(128).contains("not approximately"));ck(Batch20.rateLabel(null)=="Bitrate unavailable")
 ck(Batch20.canPreload(true,true,true,false,true));ck(!Batch20.canPreload(false,true,true,false,true));ck(!Batch20.canPreload(true,false,true,false,true));ck(!Batch20.canPreload(true,true,false,false,true));ck(!Batch20.canPreload(true,true,true,true,true));ck(!Batch20.canPreload(true,true,true,false,false));ck(Batch20.PRELOAD_BYTES==524288L)
 val requests=mutableListOf<Int>();val rows=Batch20.boundedPages(100,{i:Int->i}){size,token->requests.add(size);listOf(1,2) to (token+"a")};ck(requests==listOf(50,50));ck(rows==listOf(1,2))
 val small=mutableListOf<Int>();Batch20.boundedPages(30,{i:Int->i}){size,_->small.add(size);emptyList<Int>() to "next"};ck(small==listOf(30))
 ck(Batch20.nearestMarker(listOf(.2f,.5f),.51f,.03f)==1);ck(Batch20.nearestMarker(listOf(.2f,.5f),.8f,.03f)==null);ck(Batch20.nearestMarker(listOf(Float.NaN),.5f,.1f)==null)
 val artists=listOf(Batch20.SimilarArtist("A",1.0),Batch20.SimilarArtist("B",.2),Batch20.SimilarArtist("b",.9),Batch20.SimilarArtist("C",.8),Batch20.SimilarArtist("X",Double.NaN),Batch20.SimilarArtist("Y",2.0));ck(Batch20.similarArtists("a",artists).map{it.name}==listOf("b","C"));ck(Batch20.similarArtists("A",artists,1).size==1)
 println("$n V20 assertions passed")
}
