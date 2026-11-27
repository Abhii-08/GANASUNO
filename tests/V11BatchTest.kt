import com.abhi.ganasuno.data.*
import com.abhi.ganasuno.ui.WordLrc
import java.time.*
fun main(){
 val a=Song("a","Alpha","Artist A","","");val b=Song("b","Beta","Artist B","","");val c=Song("c","Gamma","Artist A","","");val d=Song("d","Delta","Artist C","","")
 val zone=ZoneId.of("UTC");val at=Instant.parse("2026-01-01T00:00:00Z").toEpochMilli();val h=listOf(LibraryStore.Listen(a,at,120000),LibraryStore.Listen(b,at,60000),LibraryStore.Listen(a,at+86400000,60000));var n=0
 fun test(ok:Boolean){check(ok){"Assertion ${n+1}"};n++}
 test(Batch11.quick(h,listOf(c,a)).map{it.id}==listOf("a","b","c"));test(Batch11.quick(emptyList(),listOf(c)).single()==c);test(Batch11.quick(h,emptyList(),0).isEmpty())
 val mix=Batch11.mix(h,listOf(a),listOf(a,b,c,d,a));test(mix.size==4);test(mix.map{it.id}.distinct().size==4);test(mix.take(3).map{it.artist}.distinct().size==3);test(Batch11.mix(h,emptyList(),listOf(a,b),1).size==1)
 val date=LocalDate.of(2026,1,1);val daily=Batch11.daily(h,listOf(a),listOf(a,b,c,d),date);test(daily.first().tracks.map{it.id}.toSet()==setOf("c","d"));test(daily==Batch11.daily(h,listOf(a),listOf(a,b,c,d),date));test(Batch11.daily(emptyList(),emptyList(),emptyList(),date).isEmpty())
 val suggestions=Batch11.suggestions("Alpha\tArtist A\nAlpha\tArtist A\nBeta\tArtist B\ninvalid\nhttps://bad\tArtist C");test(suggestions.size==2);test(Batch11.matching(suggestions.first(),listOf(b,a))==a);test(Batch11.matching(suggestions.first(),listOf(c))==null);test(Batch11.key(" ARTIST-A ")=="artist a")
 val r=Batch11.recap(h,2026,zone)!!;test(r.minutes==4L);test(r.days==2);test(r.tracks==2);test(r.artists==2);test(r.months.first().second==240000L);test(r.months.drop(1).all{it.second==0L});test(Batch11.recap(h,2025,zone)==null)
 val words=WordLrc.parse("<00:01.000>Hello <00:02.000>world");test(words.indexOfLast{it.time<=999}==-1);test(words.indexOfLast{it.time<=1000}==0);test(words.indexOfLast{it.time<=2000}==1);test(WordLrc.parse("Plain line").isEmpty());println("$n assertions passed")
}
