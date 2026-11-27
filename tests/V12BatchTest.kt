import com.abhi.ganasuno.player.SoftwareReverb
import com.abhi.ganasuno.ui.KaraokeTiming
import kotlin.math.abs

fun main(){
    var assertions=0
    fun verify(value:Boolean){check(value){"Failed assertion ${assertions+1}"};assertions++}
    val rate=16000
    fun impulse(preset:Int):ShortArray=ShortArray(rate*8).apply{this[0]=20000;SoftwareReverb(rate,1,preset).process(this,90)}
    val room=impulse(1);val cathedral=impulse(7)
    verify(room.drop(1).any{it!=0.toShort()});verify(cathedral.drop(1).any{it!=0.toShort()})
    verify(cathedral.drop(rate).sumOf{abs(it.toInt()).toLong()}>room.drop(rate).sumOf{abs(it.toInt()).toLong()})
    verify((1..7).map{SoftwareReverb.profile(it)}.distinct().size==7)
    for(p in 1..7){val a=impulse(p);verify(a.drop(1).any{it!=0.toShort()})}
    val original=ShortArray(1200){(it*17%6000-3000).toShort()};val bypass=original.copyOf();SoftwareReverb(rate,1,7).process(bypass,0);verify(original.contentEquals(bypass))
    val source=ShortArray(rate*2){if(it==0)20000 else 0};val full=source.copyOf();SoftwareReverb(rate,1,7).process(full,50)
    val engine=SoftwareReverb(rate,1,7);val first=source.copyOfRange(0,1001);val second=source.copyOfRange(1001,source.size);engine.process(first,50);engine.process(second,50);verify(full.contentEquals(first+second))
    val stereo=ShortArray(rate*2).apply{this[0]=20000};SoftwareReverb(rate,2,7).process(stereo,80);verify(stereo.indices.filter{it%2==1}.all{stereo[it]==0.toShort()})
    val silence=ShortArray(rate);SoftwareReverb(rate,1,7).process(silence,100);verify(silence.all{it==0.toShort()})
    val lyric=KaraokeTiming.prepare("<00:01.000>Hello <00:02.000>world<00:03.000>",4000)
    verify(lyric.text=="Hello world");verify(lyric.segments.size==2);verify(lyric.segments.last().endMs==3000L)
    verify(KaraokeTiming.fills(lyric,900).isEmpty());verify(KaraokeTiming.progress(lyric.segments.first(),1500)==.5f)
    verify(KaraokeTiming.fills(lyric,1550).any{it.fraction>0&&it.fraction<1});verify(KaraokeTiming.fills(lyric,3500).all{it.fraction==1f})
    verify(KaraokeTiming.fills(lyric,1200)!=KaraokeTiming.fills(lyric,2500));verify(KaraokeTiming.fills(lyric,1200)==KaraokeTiming.fills(lyric,1200))
    val last=KaraokeTiming.prepare("<00:01>last",null);verify(last.segments.first().endMs==1000L)
    verify(KaraokeTiming.prepare("<00:01>last",2000).segments.first().endMs==2000L)
    verify(KaraokeTiming.prepare("Plain lyrics",null).segments.isEmpty())
    val unicode=KaraokeTiming.prepare("<00:01>A😀<00:02>",null);verify(unicode.segments.first().clusters==listOf(0,1,3))
    println("$assertions standalone assertions passed")
}
