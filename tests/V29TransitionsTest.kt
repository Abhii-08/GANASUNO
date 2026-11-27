import com.abhi.ganasuno.player.TransitionMath
import com.abhi.ganasuno.player.SweepFilter
import kotlin.math.*

fun main(){
    var checks=0
    fun test(ok:Boolean){check(ok){"Assertion ${checks+1}"};checks++}
    test(TransitionMath.durationMs(-1)==0L);test(TransitionMath.durationMs(18)==15000L)
    test(TransitionMath.durationMs(5)==5000L)
    test(TransitionMath.sameAlbum(" Album  A ","album a","Artist","artist"))
    test(!TransitionMath.sameAlbum("","","A","A"));test(!TransitionMath.sameAlbum("Same","Same","A","B"))
    test(TransitionMath.canOverlap(5,1000,false,false,false,true))
    test(!TransitionMath.canOverlap(0,1000,false,false,false,true))
    test(!TransitionMath.canOverlap(5,0,false,false,false,true))
    test(!TransitionMath.canOverlap(5,1000,true,false,false,true))
    test(!TransitionMath.canOverlap(5,1000,false,true,false,true))
    test(!TransitionMath.canOverlap(5,1000,false,false,true,true))
    test(TransitionMath.canOverlap(5,1000,false,false,true,false))
    test(TransitionMath.progress(5000,5000)==0.0);test(TransitionMath.progress(2500,5000)==0.5);test(TransitionMath.progress(0,5000)==1.0)
    test(TransitionMath.progress(7000,5000)==0.0);test(TransitionMath.progress(-1,5000)==1.0)
    for(t in listOf(0.0,0.25,0.5,0.75,1.0)){val (a,b)=TransitionMath.gains(t);test(abs(a*a+b*b-1)<0.00001)}
    test(TransitionMath.cutoff(0.0,false)>TransitionMath.cutoff(1.0,false));test(TransitionMath.cutoff(0.0,true)>TransitionMath.cutoff(1.0,true))
    fun rms(x:ShortArray)=sqrt(x.drop(1000).map{it.toDouble()*it}.average())
    fun tone(hz:Double)=ShortArray(4800){(sin(2*PI*hz*it/48000)*12000).toInt().toShort()}
    val low=tone(80.0);val high=tone(8000.0);val lowRms=rms(low);val highRms=rms(high)
    SweepFilter(48000,1).process(low,1.0,false);SweepFilter(48000,1).process(high,1.0,false)
    test(rms(low)>lowRms*0.8);test(rms(high)<highRms*0.15)
    val bass=tone(80.0);val treble=tone(8000.0);SweepFilter(48000,1).process(bass,0.0,true);SweepFilter(48000,1).process(treble,0.0,true)
    test(rms(bass)<lowRms*0.15);test(rms(treble)>highRms*0.7)
    println("$checks transition/DSP assertions passed")
}
