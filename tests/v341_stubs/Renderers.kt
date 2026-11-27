package androidx.media3.exoplayer
import android.content.Context
import androidx.media3.exoplayer.audio.AudioSink
open class DefaultRenderersFactory(context:Context){open fun buildAudioSink(context:Context,enableFloatOutput:Boolean,enableAudioTrackPlaybackParams:Boolean):AudioSink=object:AudioSink{}}
