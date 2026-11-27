import android.content.Context
import com.abhi.ganasuno.data.MediaArchive
import com.abhi.ganasuno.data.LibraryStore
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
fun main(){
    val tmp=Files.createTempDirectory("archive-test").toFile()
    try{
        val src=Context(File(tmp,"src").apply{mkdirs()});val dst=Context(File(tmp,"dst").apply{mkdirs()});val name="12345678-1234-1234-1234-123456789abc.audio"
        val audio=File(src.filesDir,"offline-media").apply{mkdirs()};File(audio,name).writeBytes(byteArrayOf(1,2,3,4))
        val bytes=ByteArrayOutputStream().also{MediaArchive.write(src,it)}.toByteArray()
        check(MediaArchive.restore(dst,ByteArrayInputStream(bytes))==1)
        val restored=File(dst.filesDir,"offline-media/$name");check(restored.readBytes().contentEquals(byteArrayOf(1,2,3,4)));check(LibraryStore.restored==LibraryStore.backup())
        check(MediaArchive.restore(dst,ByteArrayInputStream(bytes))==1)
        restored.writeText("different");check(runCatching{MediaArchive.restore(dst,ByteArrayInputStream(bytes))}.isFailure);check(restored.readText()=="different")
        val attack=ByteArrayOutputStream().also{out->ZipOutputStream(out).use{z->z.putNextEntry(ZipEntry("../escape"));z.write(byteArrayOf(1));z.closeEntry()}}.toByteArray()
        check(runCatching{MediaArchive.restore(dst,ByteArrayInputStream(attack))}.isFailure);check(!File(tmp,"escape").exists())
        check(dst.filesDir.listFiles().orEmpty().none{it.name.startsWith("restore-")})
        println("9 archive assertions passed (filesystem with Android/library stubs)")
    }finally{tmp.deleteRecursively()}
}
