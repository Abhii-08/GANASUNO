import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import com.abhi.ganasuno.player.EffectsProcessor
import com.abhi.ganasuno.player.EffectsSettings
import com.abhi.ganasuno.player.DeckSweep
import com.abhi.ganasuno.data.LibraryStore
import com.abhi.ganasuno.data.Prefs
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun main(){
    var checks=0
    fun verify(value:Boolean,label:String){check(value){label};checks++}
    val shared=AudioProcessor.EMPTY_BUFFER
    val sweep=DeckSweep()
    val p=EffectsProcessor(sweep)
    p.configure(AudioFormat(44100,2,2));p.flush()
    p.queueInput(shared);verify(!p.getOutput().hasRemaining(),"empty shared buffer on first poll")
    repeat(10){p.queueInput(shared);verify(!p.getOutput().hasRemaining(),"repeated empty poll")}
    fun pcm(vararg samples:Int)=ByteBuffer.allocateDirect(samples.size*2).order(ByteOrder.nativeOrder()).apply{samples.forEach{putShort(it.toShort())};flip()}
    val input=pcm(1234,-4321,500,-600)
    p.queueInput(input);verify(!input.hasRemaining(),"consume input")
    val out=p.getOutput();verify(out.remaining()==8,"preserve PCM byte count")
    verify(List(4){out.short.toInt()}==listOf(1234,-4321,500,-600),"dry PCM unchanged")
    p.queueInput(shared);verify(!p.getOutput().hasRemaining(),"empty poll after real PCM")
    // Empty slice with nonzero capacity/position must also be ignored.
    val empty=ByteBuffer.allocateDirect(16).apply{position(8);limit(8)}
    p.queueInput(empty);verify(empty.position()==8&&!p.getOutput().hasRemaining(),"empty slice untouched")
    LibraryStore.options["rotate"]=1;LibraryStore.options["delay_ms"]=100;LibraryStore.options["dsp_eq"]=1;LibraryStore.options["dsp_band_2"]=3;LibraryStore.options["reverb"]=1;Prefs.normalize=true;EffectsSettings.refresh();sweep.progress=.5
    p.queueInput(shared);verify(!p.getOutput().hasRemaining(),"empty poll with effects enabled")
    val wet=pcm(1000,-1000,2000,-2000);p.queueInput(wet);verify(!wet.hasRemaining(),"effects consume PCM");verify(p.getOutput().remaining()==8,"effects preserve byte count")
    p.flush();p.queueInput(shared);verify(!p.getOutput().hasRemaining(),"empty after flush")
    p.reset();p.configure(AudioFormat(48000,1,2));p.flush();p.queueInput(shared);verify(!p.getOutput().hasRemaining(),"empty after reset and mono configure")
    val mono=pcm(800,-800);p.queueInput(mono);verify(p.getOutput().remaining()==4,"mono effects preserve bytes")
    println("$checks EffectsProcessor JVM assertions passed")
}
