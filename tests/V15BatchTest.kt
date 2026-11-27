import com.abhi.ganasuno.data.*
fun main(){
    var count=0;fun test(x:Boolean){check(x){"Failed assertion ${count+1}"};count++}
    val a=Song("abcdefghijk","First","Artist A","","",album="Album 1")
    val b=Song("12345678901","Second","artist a","","",album="album 1")
    val c=Song("zyxwvutsrqp","Third","Artist B","","",album="Album 1")
    val local=Song("local:content://private","Local","Artist B","","",localUri="content://private",album="Album 2")
    val live=Song("11111111111","Live","Artist B","","",live=true)
    val tracks=listOf(a,b,c,local,live,a)
    test(Batch15.publicIds(tracks)==listOf(a.id,b.id,c.id));test(Batch15.publicIds(listOf(local,live)).isEmpty());test(Batch15.shareIds(listOf(a.id,a.id))==listOf(a.id));test(runCatching{Batch15.shareIds(emptyList())}.isFailure);test(runCatching{Batch15.shareIds(listOf(local.id))}.isFailure);test(runCatching{Batch15.shareIds(List(101){a.id})}.isFailure)
    test(Batch15.validCode("a".repeat(32)));test(!Batch15.validCode("/".repeat(32)));test(!Batch15.validCode("a".repeat(31)));test(Batch15.cleanTitle("  hello\nworld  ")=="helloworld");test(Batch15.cleanTitle("x".repeat(110)).length==100)
    val artists=Batch15.artists(tracks);test(artists.size==2);test(artists.first().tracks.size==2);test(Batch15.matching(artists,"artist b").single().tracks.size==3);test(Batch15.matching(artists,"not there").isEmpty())
    val albums=Batch15.albums(tracks);test(albums.size==3);test(albums.first().tracks.size==2);test(Batch15.matching(albums,"album 1 artist b").single().tracks==listOf(c));test(Batch15.albums(tracks,mapOf(a.id to "Changed")).size==4);test(Batch15.albums(listOf(live)).isEmpty())
    test(Batch15.streamFormat("webm",251)==("Opus" to "audio/webm"));test(Batch15.streamFormat("m4a",140)==("AAC" to "audio/mp4"));test(Batch15.streamFormat("webm",999)==("Unknown (WebM audio)" to "audio/webm"));test(Batch15.streamFormat(null,null)==("Unknown" to "Unknown"))
    val start=java.time.Instant.parse("2025-01-01T00:00:00Z").toEpochMilli();val end=java.time.Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();val zone=java.time.ZoneId.of("UTC")
    val recap=Batch15.recap(listOf(LibraryStore.Listen(a,start,60000),LibraryStore.Listen(b,start+86400000,120000),LibraryStore.Listen(c,end,900000)),start,end,zone)
    test(recap.contains("3 minutes, 2 active days, 2 tracks"));test(recap.contains("2025-01: 3 min"));test(recap.contains("2025-12: 0 min"));test(!recap.contains("Third"));test(Batch15.recap(emptyList(),start,end,zone).isEmpty());test(runCatching{Batch15.recap(emptyList(),end,start,zone)}.isFailure)
    println("$count Batch 15 standalone assertions passed")
}
