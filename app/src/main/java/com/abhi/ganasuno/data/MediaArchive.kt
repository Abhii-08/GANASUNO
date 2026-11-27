package com.abhi.ganasuno.data

import android.content.Context
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** User-selected archive with private app-owned audio and library metadata; external SAF files excluded. */
object MediaArchive {
    private const val FILE_LIMIT=100L*1024*1024
    private const val TOTAL_LIMIT=1024L*1024*1024
    private const val META_LIMIT=32L*1024*1024
    private val filename=Regex("[a-f0-9-]{36}\\.audio")
    private fun directory(c:Context)=File(c.filesDir,"offline-media").apply{mkdirs()}
    private fun bounded(input:InputStream,out:OutputStream,limit:Long):Long {
        val buffer=ByteArray(64*1024);var total=0L
        while(true){val n=input.read(buffer);if(n<0)break;total+=n;require(total<=limit){"Archive size limit exceeded"};out.write(buffer,0,n)}
        return total
    }
    @Synchronized fun write(c:Context,out:OutputStream,settings:String?=null) {
        val metadata=LibraryStore.backup().toByteArray(Charsets.UTF_8);require(metadata.size<=META_LIMIT)
        val files=directory(c).listFiles().orEmpty().filter{it.isFile&&filename.matches(it.name)}
        require(files.size<=1000&&files.all{it.length() in 1..FILE_LIMIT}&&files.sumOf{it.length()}<=TOTAL_LIMIT){"Backup exceeds 1 GB / 1000 audio files"}
        val manifest=files.associate{f->val actual=f.inputStream().use{AudioReceipts.digest(it)};val saved=AudioReceipts.read(c.filesDir,f.name);require(saved==null||actual==saved){"Audio checksum mismatch: ${f.name}"};f.name to actual}
        ZipOutputStream(out).use{zip->
            zip.putNextEntry(ZipEntry("library.json"));zip.write(metadata);zip.closeEntry()
            if(settings!=null){val data=settings.toByteArray(Charsets.UTF_8);require(data.size<=65536);zip.putNextEntry(ZipEntry("settings.json"));zip.write(data);zip.closeEntry()}
            if(manifest.isNotEmpty()){zip.putNextEntry(ZipEntry("audio-checksums.txt"));zip.write(AudioReceipts.manifest(manifest).toByteArray(Charsets.UTF_8));zip.closeEntry()}
            files.forEach{f->zip.putNextEntry(ZipEntry("audio/${f.name}"));f.inputStream().use{bounded(it,zip,FILE_LIMIT)};zip.closeEntry()}
        }
    }
    @Synchronized fun restore(c:Context,input:InputStream,validateSettings:(String)->Unit={},applySettings:(String)->Unit={}):Int {
        val stage=File(c.filesDir,"restore-${UUID.randomUUID()}").apply{check(mkdirs())}
        val created=mutableListOf<File>();var committed=false
        try {
            val names=mutableSetOf<String>();var total=0L;var metadata:File?=null;var settingsFile:File?=null;var checksumFile:File?=null
            ZipInputStream(input).use{zip->
                while(true){val e=zip.nextEntry?:break;val name=e.name
                    require(!e.isDirectory&&names.add(name)&&names.size<=1003){"Invalid or duplicate archive entry"}
                    val isMeta=name=="library.json";val isSettings=name=="settings.json";val isChecksums=name=="audio-checksums.txt";val base=name.removePrefix("audio/")
                    require(isMeta||isSettings||isChecksums||(name.startsWith("audio/")&&filename.matches(base))){"Unsupported archive entry"}
                    val f=File(stage,if(isMeta)"library.json" else if(isSettings)"settings.json" else if(isChecksums)"audio-checksums.txt" else base)
                    val bytes=f.outputStream().use{bounded(zip,it,if(isMeta)META_LIMIT else if(isSettings)65536L else if(isChecksums)120000L else minOf(FILE_LIMIT,TOTAL_LIMIT-total))}
                    require(bytes>0);if(isMeta)metadata=f else if(isSettings)settingsFile=f else if(isChecksums)checksumFile=f else total+=bytes;zip.closeEntry()
                }
            }
            val raw=metadata?.readText(Charsets.UTF_8)?:error("Missing library metadata")
            LibraryStore.validateBackup(raw)
            val settings=settingsFile?.readText(Charsets.UTF_8);settings?.let(validateSettings)
            val manifest=checksumFile?.let{AudioReceipts.readManifest(it.readText(Charsets.UTF_8))}
            val stagedAudio=stage.listFiles().orEmpty().filter{filename.matches(it.name)}
            if(manifest!=null)require(manifest.keys==stagedAudio.map{it.name}.toSet()){"Audio checksum list mismatch"}
            stagedAudio.forEach{f->
                if(manifest!=null)require(f.inputStream().use{AudioReceipts.digest(it)}==manifest[f.name]){"Archive audio checksum mismatch"}
            }
            val target=directory(c)
            stage.listFiles().orEmpty().filter{filename.matches(it.name)}.forEach{f->
                val dest=File(target,f.name)
                if(dest.exists()) { require(dest.isFile&&dest.length()==f.length()&&digest(dest).contentEquals(digest(f))){"Audio filename collision"} }
                else {created.add(dest);f.copyTo(dest,overwrite=false)}
            }
            manifest?.forEach{(n,r)->val old=AudioReceipts.read(c.filesDir,n);require(old==null||old==r){"Existing receipt mismatch"};if(old==null){created.add(AudioReceipts.file(c.filesDir,n));AudioReceipts.write(c.filesDir,n,r)}}
            LibraryStore.restore(raw);committed=true;settings?.let(applySettings)
            return names.count{it.startsWith("audio/")}
        } finally {if(!committed)created.forEach{it.delete()};stage.deleteRecursively()}
    }
    private fun digest(f:File):ByteArray {val d=java.security.MessageDigest.getInstance("SHA-256");f.inputStream().use{input->val b=ByteArray(65536);while(true){val n=input.read(b);if(n<0)break;d.update(b,0,n)}};return d.digest()}
}
