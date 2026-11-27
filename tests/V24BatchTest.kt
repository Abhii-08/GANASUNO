import com.abhi.ganasuno.data.AudioReceipts
import com.abhi.ganasuno.data.RoomTiming
import com.abhi.ganasuno.data.MediaArchive
import android.content.Context
import java.io.*
import java.nio.file.Files
import java.util.zip.*
fun main(){
 val root=Files.createTempDirectory("v24").toFile()
 try {
  val n="12345678-1234-1234-1234-123456789abc.audio"
  val bytes="test audio".toByteArray();val r=AudioReceipts.digest(bytes.inputStream());check(r.size==10L);check(AudioReceipts.parse(AudioReceipts.encode(r))==r)
  check(AudioReceipts.readManifest(AudioReceipts.manifest(mapOf(n to r)))==mapOf(n to r))
  check(runCatching{AudioReceipts.readManifest("../file:${AudioReceipts.encode(r)}")}.isFailure)
  check(runCatching{AudioReceipts.readManifest("$n:${AudioReceipts.encode(r)}\n$n:${AudioReceipts.encode(r)}")}.isFailure)
  check(runCatching{AudioReceipts.digest(bytes.inputStream(),3)}.isFailure)
  val c=Context(File(root,"src").apply{mkdirs()});val d=Context(File(root,"dest").apply{mkdirs()});val f=File(c.filesDir,"offline-media/$n");f.parentFile.mkdirs();f.writeBytes(bytes)
  AudioReceipts.write(c.filesDir,n,r);check(AudioReceipts.read(c.filesDir,n)==r)
  val archive=ByteArrayOutputStream().also{MediaArchive.write(c,it)}.toByteArray();check(MediaArchive.restore(d,archive.inputStream())==1);check(AudioReceipts.read(d.filesDir,n)==r)
  f.writeText("damaged");check(runCatching{MediaArchive.write(c,ByteArrayOutputStream())}.isFailure)
  val bad=ByteArrayOutputStream().also{out->ZipOutputStream(out).use{zip->mapOf("library.json" to "{\"schema\":1}","audio-checksums.txt" to AudioReceipts.manifest(mapOf(n to r)),"audio/$n" to "damaged").forEach{(k,v)->zip.putNextEntry(ZipEntry(k));zip.write(v.toByteArray());zip.closeEntry()}}}.toByteArray()
  val target=Context(File(root,"bad").apply{mkdirs()});check(runCatching{MediaArchive.restore(target,bad.inputStream())}.isFailure);check(!File(target.filesDir,"offline-media/$n").exists())
  check(RoomTiming.estimate(listOf(1000,100,200))==50L);check(RoomTiming.estimate(listOf(-1,5000))==0L);check(RoomTiming.publicationAge(1000,1100,listOf(200))==200L);check(RoomTiming.publicationAge(0,5000,listOf(300))==2000L);check(RoomTiming.arrivalAge(200,100,listOf(100))==150L);check(RoomTiming.arrivalAge(200,5000,listOf(100))==2000L)
  println("18 archive, receipt and timing assertions passed")
 }finally{root.deleteRecursively()}
}
