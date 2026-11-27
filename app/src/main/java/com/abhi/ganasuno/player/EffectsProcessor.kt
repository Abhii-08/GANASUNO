package com.abhi.ganasuno.player

import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import java.nio.ByteBuffer
import java.nio.ByteOrder
import com.abhi.ganasuno.data.LibraryStore

@UnstableApi
object EffectsSettings {
    data class Eq(val enabled:Boolean=false,val gains:List<Int> = List(10){0},val preamp:Int=0, val inputGain:Int=0)
    @Volatile var eq=Eq()
        private set
    data class Reverb(val preset:Int=0,val mix:Int=35)
    @Volatile var reverb=Reverb()
        private set
    @Volatile var options=StereoDsp.Options()
        private set
    fun refresh(){refreshReverb();eq=Eq(LibraryStore.option("dsp_eq")==1,List(10){LibraryStore.option("dsp_band_$it").coerceIn(-12,12)},LibraryStore.option("dsp_headroom",-6),LibraryStore.option("preamp",0).coerceIn(-12,6));options=StereoDsp.Options(LibraryStore.option("rotate")==1,LibraryStore.option("rotate_period",8),LibraryStore.option("rotate_direction",1),LibraryStore.option("delay_ms"),LibraryStore.option("delay_feedback",20),LibraryStore.option("delay_mix",20))}
    fun refreshReverb(){reverb=Reverb(LibraryStore.option("reverb",0).coerceIn(0,7),LibraryStore.option("reverb_mix",35).coerceIn(0,100))}
}
@UnstableApi
class EffectsProcessor(private val sweep:DeckSweep=DeckSweep()):BaseAudioProcessor(){
    private var filter:SweepFilter?=null
    private var dsp:StereoDsp?=null
    private var normalizer:com.abhi.ganasuno.data.RmsNormalizer?=null
    private var normalizing=false
    private var wasActive=false
    private var lastEq:EffectsSettings.Eq?=null
    private var tenBand:TenBandDsp?=null
    private var reverb:SoftwareReverb?=null
    private var lastPreset=0
    override fun onConfigure(inputAudioFormat:AudioFormat):AudioFormat = if(inputAudioFormat.encoding==C.ENCODING_PCM_16BIT&&inputAudioFormat.channelCount in 1..2)inputAudioFormat else AudioFormat.NOT_SET
    override fun onFlush(){filter=null;normalizer=null;normalizing=false;reverb=null;lastPreset=0;lastEq=null;tenBand=null;wasActive=false;dsp=if(inputAudioFormat.channelCount in 1..2&&inputAudioFormat.sampleRate>0)StereoDsp(inputAudioFormat.sampleRate,inputAudioFormat.channelCount)else null}
    override fun onReset(){filter=null;normalizer=null;normalizing=false;reverb=null;lastPreset=0;dsp=null;lastEq=null;tenBand=null}
    override fun queueInput(inputBuffer:ByteBuffer){
        // Media3 polls the pipeline with its shared EMPTY_BUFFER. Do not allocate
        // or copy on an empty poll: BaseAudioProcessor can return that same buffer.
        if (!inputBuffer.hasRemaining()) return
        val out=replaceOutputBuffer(inputBuffer.remaining());val settings=EffectsSettings.options;val core=dsp;val eq=EffectsSettings.eq;val rv=EffectsSettings.reverb;val sweepProgress=sweep.progress;if(sweepProgress<0)filter=null
        if(core!=null&&rv.preset!=lastPreset){lastPreset=rv.preset;reverb=if(rv.preset in 1..7 && inputAudioFormat.sampleRate in 8000..192000)SoftwareReverb(inputAudioFormat.sampleRate,inputAudioFormat.channelCount,rv.preset)else null}
        if(core!=null&&eq!=lastEq){lastEq=eq;tenBand=if(eq.enabled||eq.inputGain!=0)TenBandDsp(inputAudioFormat.sampleRate,inputAudioFormat.channelCount,if(eq.enabled)eq.gains else List(10){0},eq.inputGain+if(eq.enabled)eq.preamp else 0)else null}
        val normalize=com.abhi.ganasuno.data.Prefs.normalize
        if(normalize!=normalizing){normalizing=normalize;normalizer=if(normalize&&core!=null&&inputAudioFormat.sampleRate in 8000..192000)com.abhi.ganasuno.data.RmsNormalizer(inputAudioFormat.sampleRate,inputAudioFormat.channelCount)else null}
        if(core==null||(!settings.rotate&&settings.delayMs==0&&!eq.enabled&&eq.inputGain==0&&reverb==null&&normalizer==null&&sweepProgress<0)){out.put(inputBuffer);if(core!=null&&wasActive){dsp=StereoDsp(inputAudioFormat.sampleRate,inputAudioFormat.channelCount)};wasActive=false}
        else{wasActive=true;val count=inputBuffer.remaining()/2;val channels=inputAudioFormat.channelCount;val frames=count-count%channels;val samples=ShortArray(frames);inputBuffer.order(ByteOrder.nativeOrder());for(i in samples.indices)samples[i]=inputBuffer.short;normalizer?.process(samples);tenBand?.process(samples);core.process(samples,settings);reverb?.process(samples,rv.mix);if(sweepProgress>=0){val f=filter?:SweepFilter(inputAudioFormat.sampleRate,channels).also{filter=it};f.process(samples,sweepProgress,sweep.incoming)};for(v in samples)out.putShort(v);out.put(inputBuffer)}
        out.flip()
    }
}
@UnstableApi
class EffectsRenderers(context:Context,private val sweep:DeckSweep=DeckSweep()):DefaultRenderersFactory(context){
    override fun buildAudioSink(context:Context,enableFloatOutput:Boolean,enableAudioTrackPlaybackParams:Boolean):AudioSink = DefaultAudioSink.Builder(context).setAudioProcessors(arrayOf(EffectsProcessor(sweep))).setEnableFloatOutput(false).setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams).build()
}
