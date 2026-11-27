import com.abhi.ganasuno.player.StereoDsp
fun main(){
    val engine=StereoDsp(1000,2);val input=shortArrayOf(1000,-1000,2000,-2000);val original=input.copyOf();engine.process(input,StereoDsp.Options());check(input.contentEquals(original))
    val delay=StereoDsp(1000,1);val signal=ShortArray(100);signal[0]=10000;delay.process(signal,StereoDsp.Options(delayMs=10,feedback=50,mix=25));check(signal[0].toInt()==7500);check(signal[10].toInt()==2500);check(signal[20].toInt()==1250)
    val rotate=StereoDsp(1000,2);val stereo=ShortArray(1600){10000};rotate.process(stereo,StereoDsp.Options(rotate=true,period=3));check(stereo[1500].toInt()<2000);check(stereo[1501].toInt()==10000)
    val reverse=StereoDsp(1000,2);val right=ShortArray(1600){10000};reverse.process(right,StereoDsp.Options(rotate=true,period=3,direction=-1));check(right[1501].toInt()<2000);check(right[1500].toInt()==10000)
    println("8 PCM DSP assertions passed")
}
