import com.abhi.ganasuno.data.*
import com.abhi.ganasuno.player.TenBandDsp
import kotlin.math.*

fun main(){
    val flat=ShortArray(1000){(it*7-3500).toShort()};val orig=flat.copyOf();TenBandDsp(48000,2,List(10){0}).process(flat);check(flat.contentEquals(orig))
    val atten=shortArrayOf(10000,-10000);TenBandDsp(48000,2,List(10){0},-6).process(atten);check(atten[0] in 4990..5030&&atten[1] in -5030..-4990)
    val sine=ShortArray(48000){(sin(2*PI*1000*it/48000)*1000).toInt().toShort()};val boosted=sine.copyOf();TenBandDsp(48000,1,List(10){if(it==5)6 else 0}).process(boosted);check(boosted.takeLast(24000).max()>1800)
    val stereo=ShortArray(2000){if(it%2==0)1000 else 0};TenBandDsp(48000,2,List(10){6}).process(stereo);check(stereo.indices.filter{it%2==1}.all{stereo[it]==0.toShort()})
    val profile=TenBandDsp.graphicProfile("GraphicEQ: 20 -4; 1000 2; 20000 4");check(profile.size==10&&profile[5]==2&&profile[0]<0)
    check(runCatching{TenBandDsp.graphicProfile("100 2; 100 3")}.isFailure)
    val silence=AudioAnalysis(8000,6000);repeat(48000){silence.sample(0.0)};val quiet=silence.result();check(quiet.bpm==null&&quiet.key==null&&quiet.peaks.all{it==0f})
    val rhythm=AudioAnalysis(8000,10000);repeat(80000){i->val beat=i%4000;rhythm.sample(if(beat<400)sin(i*2*PI*440/8000)*0.8 else 0.0)};val r=rhythm.result();check(r.bpm in 118..122){"Expected 120 BPM, got ${r.bpm}"};check(r.peaks.max()==1f&&r.analyzedMs==10000L)
    val harmony=AudioAnalysis(8000,6000);repeat(48000){i->val v=listOf(261.6256,329.6276,391.9954).sumOf{sin(2*PI*it*i/8000)}*.25;harmony.sample(v)};check(harmony.result().key=="C major"){harmony.result().key?:"Unknown key"}
    val s=Song("1","One","Artist A","","");val other=Song("2","Two","Artist B","","");val at=1700000000000L
    val score=Fingerprint.calculate(listOf(LibraryStore.Listen(s,at,3000),LibraryStore.Listen(other,at,1000)),listOf(LibraryStore.Listen(s,at-100000,1000)),4)!!
    check(score.consistency==25&&score.discovery==50&&score.diversity==100&&score.concentration==75&&score.replay==75)
    check(Fingerprint.calculate(emptyList(),emptyList(),4)==null)
    println("V6 core: 12 checks passed")
}
