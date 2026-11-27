import com.abhi.ganasuno.data.*
import com.abhi.ganasuno.ui.WordLrc
import com.abhi.ganasuno.player.TenBandDsp
fun main(){
 var n=0
 fun verify(value:Boolean){check(value){"Assertion ${n+1} failed"};n++}
 val first=Song("a","Zed","Beta","", "",album="Record");val second=Song("b","Alpha","Alpha","", "",album="Other");val third=Song("c","Gamma","Gamma","", "")
 val items=listOf(first,second,third);val added=mapOf("a" to 20L,"b" to 10L);val recent=mapOf("b" to 50L,"a" to 10L)
 verify(BatchCore.order(items,0,added,recent)==listOf(second,third,first));verify(BatchCore.order(items,2,added,recent)==items);verify(BatchCore.order(items,3,added,recent)==listOf(second,first,third));verify(BatchCore.order(items,4,added,recent)==listOf(second,first,third));verify(BatchCore.order(items,5,added,recent)==items);verify(BatchCore.order(items,6,added,recent)==items.reversed())
 verify(BatchCore.swipe(60f,1f,30f)==1);verify(BatchCore.swipe(-60f,1f,30f)==2);verify(BatchCore.swipe(1f,60f,30f)==3);verify(BatchCore.swipe(3f,5f,30f)==0)
 verify(BatchCore.color("#123ABC")==0xff123abc.toInt());verify(BatchCore.color("123") == null);verify(BatchCore.color("#ffffff")==-1)
 verify(BatchCore.headers("X-Api-Key: value\nX-Tenant: alpha:beta")==mapOf("X-Api-Key" to "value","X-Tenant" to "alpha:beta"));verify(runCatching{BatchCore.headers("Host: bad")}.isFailure);verify(runCatching{BatchCore.headers("X-Key: one\nx-key: two")}.isFailure);verify(runCatching{BatchCore.headers("X-Key: a\u0000")}.isFailure);verify(BatchCore.headers("").isEmpty())
 verify(WordLrc.valid("[ar:Artist]\n[00:01.00]Hi\n[00:02.00]There"));verify(WordLrc.valid("[00:01]<00:01>Hi <00:02>there"));verify(!WordLrc.valid("[00:61]invalid"));verify(!WordLrc.valid("[00:01]<00:02>hi <00:01>bad"));verify(!WordLrc.valid("[00:01]<00:00>early"));verify(!WordLrc.valid("[00:01.1234]bad"));verify(!WordLrc.valid("[00:01]hello\nnot timed"));verify(WordLrc.valid("Plain lyrics\nsecond line"));verify(!WordLrc.valid("[00:01]<00:01><00:02>bad"))
 val amp=shortArrayOf(10000,-10000,30000);TenBandDsp(48000,1,List(10){0},6).process(amp);verify(amp[0] in 19940..19970 && amp[1] in -19970..-19940 && amp[2]==32767.toShort())
 val h=listOf(LibraryStore.Listen(first,1000,120000),LibraryStore.Listen(second,2000,60000),LibraryStore.Listen(third,3000,30000));val report=Insights.report(h,emptyList(),7,mapOf("c" to "Manual album"));verify("Record — Beta · 2 min" in report);verify("Other — Alpha · 1 min" in report);verify("Manual album" in report);verify("Unknown album" !in report);verify("Unknown album" in Insights.report(h,emptyList(),7))
 println("V9 batch: $n checks passed")
}
