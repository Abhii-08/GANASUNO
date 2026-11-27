package androidx.media3.common.audio
import java.nio.ByteBuffer
import java.nio.ByteOrder
interface AudioProcessor {
    data class AudioFormat(val sampleRate:Int,val channelCount:Int,val encoding:Int) {
        companion object { val NOT_SET=AudioFormat(-1,-1,-1) }
    }
    companion object { val EMPTY_BUFFER:ByteBuffer=ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder()) }
}
// JVM test double for the relevant Media3 BaseAudioProcessor buffer lifecycle.
abstract class BaseAudioProcessor {
    protected var inputAudioFormat=AudioProcessor.AudioFormat.NOT_SET
    private var buffer=AudioProcessor.EMPTY_BUFFER
    private var output=AudioProcessor.EMPTY_BUFFER
    private var pending=AudioProcessor.AudioFormat.NOT_SET
    fun configure(format:AudioProcessor.AudioFormat):AudioProcessor.AudioFormat { pending=format;return onConfigure(format) }
    fun flush(){inputAudioFormat=pending;output=AudioProcessor.EMPTY_BUFFER;onFlush()}
    fun reset(){flush();buffer=AudioProcessor.EMPTY_BUFFER;inputAudioFormat=AudioProcessor.AudioFormat.NOT_SET;pending=inputAudioFormat;onReset()}
    protected fun replaceOutputBuffer(size:Int):ByteBuffer {if(buffer.capacity()<size)buffer=ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()) else buffer.clear();output=buffer;return buffer}
    fun getOutput():ByteBuffer {val result=output;output=AudioProcessor.EMPTY_BUFFER;return result}
    abstract fun queueInput(inputBuffer:ByteBuffer)
    open fun onConfigure(inputAudioFormat:AudioProcessor.AudioFormat)=inputAudioFormat
    open fun onFlush(){}
    open fun onReset(){}
}
