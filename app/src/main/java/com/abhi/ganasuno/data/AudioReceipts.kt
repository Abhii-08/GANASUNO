package com.abhi.ganasuno.data

import java.io.File
import java.io.InputStream
import java.security.MessageDigest

/** Portable integrity receipts. These detect accidental damage, not malicious archive authors. */
object AudioReceipts {
    private val name = Regex("[a-f0-9-]{36}\\.audio")
    private val hash = Regex("[a-f0-9]{64}")
    data class Receipt(val size:Long,val sha256:String)
    fun digest(input:InputStream, limit:Long=100L*1024*1024):Receipt {
        val md=MessageDigest.getInstance("SHA-256"); val buf=ByteArray(65536); var nbytes=0L
        while(true){val n=input.read(buf);if(n<0)break;if(n==0)continue;nbytes+=n;require(nbytes<=limit);md.update(buf,0,n)}
        require(nbytes>0);return Receipt(nbytes,md.digest().joinToString(""){"%02x".format(it.toInt() and 255)})
    }
    fun parse(raw:String):Receipt {val p=raw.split(':');require(p.size==2);val size=p[0].toLong();require(size in 1..100L*1024*1024&&hash.matches(p[1]));return Receipt(size,p[1])}
    fun encode(r:Receipt)="${r.size}:${r.sha256}"
    fun manifest(entries:Map<String,Receipt>):String {require(entries.size<=1000);return entries.toSortedMap().entries.joinToString("\n"){(n,r)->require(name.matches(n));"$n:${encode(r)}"}}
    fun readManifest(raw:String):Map<String,Receipt> {require(raw.length<=120000);val out=linkedMapOf<String,Receipt>();raw.lineSequence().filter{it.isNotBlank()}.forEach{line->val n=line.substringBefore(':');require(name.matches(n)&&!out.containsKey(n)&&out.size<1000);out[n]=parse(line.substringAfter(':'))};return out}
    fun file(root:File,n:String):File {require(name.matches(n));return File(File(root,"offline-receipts"),"$n.sha256")}
    fun read(root:File,n:String):Receipt? {val f=file(root,n);if(!f.isFile)return null;require(f.length()<=128);return parse(f.readText())}
    fun write(root:File,n:String,r:Receipt){val f=file(root,n);f.parentFile?.mkdirs();val temp=File.createTempFile("receipt-",".tmp",f.parentFile);try{temp.writeText(encode(r));java.nio.file.Files.move(temp.toPath(),f.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING)}finally{temp.delete()}}
}
