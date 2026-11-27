package androidx.media3.exoplayer.audio
import android.content.Context
import androidx.media3.common.audio.BaseAudioProcessor
interface AudioSink
class DefaultAudioSink:AudioSink {class Builder(context:Context){fun setAudioProcessors(value:Array<BaseAudioProcessor>)=this;fun setEnableFloatOutput(value:Boolean)=this;fun setEnableAudioTrackPlaybackParams(value:Boolean)=this;fun build()=DefaultAudioSink()}}
