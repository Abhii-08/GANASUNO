import com.abhi.ganasuno.data.*
import java.io.ByteArrayInputStream
fun main(){
 var n=0
 fun verify(ok:Boolean){check(ok){"Assertion ${n+1} failed"};n++}
 fun digest(s:String,limit:Long=100)=Batch23.digest(ByteArrayInputStream(s.toByteArray()),limit)
 val a=digest("abc")
 verify(a.size==3L);verify(a.sha256=="ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
 verify(Batch23.audit(a,a)=="Verified");verify(Batch23.audit(a,null)=="No saved checksum");verify(Batch23.audit(digest("abd"),a)=="Changed or damaged")
 verify(runCatching{digest("")}.isFailure);verify(runCatching{digest("abc",2)}.isFailure)
 val id="UC"+"a".repeat(22);val other="UC"+"b".repeat(22)
 verify(Batch23.addChannel(listOf(other,id,"invalid"),"https://www.youtube.com/channel/$id")==listOf(id,other))
 verify(runCatching{Batch23.addChannel(emptyList(),"bad")}.isFailure)
 verify(Batch23.artistChannel(id)==id);verify(Batch23.artistChannel("https://example.com/channel/$id")==null)
 verify(Batch23.tempos(List(500){0.0},50.0).isEmpty());verify(Batch23.tempos(List(100){1.0},50.0).isEmpty())
 verify(Batch23.tempos(List(500){if(it==3)Double.NaN else 0.0},50.0).isEmpty())
 val tempo=Batch23.tempos(List(1000){if(it%25==0)1.0 else 0.0},50.0)
 verify(tempo.any{it.bpm==120});verify(tempo.size<=5);verify(tempo.all{it.bpm in 45..300&&it.correlation in .2..1.000001})
 val audio=AudioAnalysis(8000,6000);repeat(48000){audio.sample(if(it%4000<200)0.8 else 0.0)}
 val result=audio.result();verify(result.analyzedMs==6000L);verify(result.peaks.size==120);verify(result.tempoCandidates.any{it.bpm==120})
 println("$n v23 assertions passed")
}
