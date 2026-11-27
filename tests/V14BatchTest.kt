import com.abhi.ganasuno.data.*
import kotlin.math.abs
fun main(){
    var count=0;fun test(b:Boolean){check(b){"Assertion ${count+1}"};count++}
    val paths=listOf("results/oratory1990/over-ear/Sony WH-1000XM4/Sony WH-1000XM4 GraphicEQ.txt","results/rtings/over-ear/Sony WH-1000XM5/Sony WH-1000XM5 GraphicEQ.txt","results/rtings/in-ear/Other/Other ParametricEQ.txt","results/../unsafe GraphicEQ.txt","README.md")
    test(Batch14.findProfiles(paths,"sony xm4").single().name=="Sony WH-1000XM4");test(Batch14.findProfiles(paths,"sony").size==2);test(Batch14.findProfiles(paths,"unknown").isEmpty());test(Batch14.profilePath(paths[3])==null);test(Batch14.profilePath("https://example.org/GraphicEQ.txt")==null);test(Batch14.findProfiles(paths+paths,"sony").size==2);test(Batch14.profilePath(paths[0])?.source=="oratory1990")
    test(Batch14.lucene("a\"b\\c")=="\"a\\\"b\\\\c\"");test(Batch14.matchTitle("A SONG","A song live"));test(!Batch14.matchTitle("One","Someone"));test(!Batch14.matchTitle("","One"));val song=Song("1","My song","A","","");val tracks=listOf(song,Song("2","My song live","A","",""),Song("3","My song remix","B","",""),Song("4","Something else","A","",""),Song("5","My song live","A","","",true));test(Batch14.alternateVersions(song,tracks).map{it.id}==listOf("2","3"));test(Batch14.alternateVersions(song,tracks+tracks).size==2)
    test(Batch14.mediaBudget(10)==655360L);test(Batch14.mediaBudget(1000)==6553600L);test(Batch14.mediaBudget(0)==655360L);test(Batch14.backupKeep(0)==1);test(Batch14.backupKeep(100)==20)
    val rate=8000;val quiet=RmsNormalizer(rate,1);val q=ShortArray(rate*10){if(it%2==0)500 else -500};quiet.process(q);test(abs(q.last().toInt())>850);test(abs(q.last().toInt())<=1000)
    val loud=RmsNormalizer(rate,1);val l=ShortArray(rate*10){if(it%2==0)20000 else -20000};loud.process(l);test(abs(l.last().toInt()) in 4500..6000);test(l.all{abs(it.toInt())<=32113})
    val stereo=RmsNormalizer(rate,2);val ss=ShortArray(rate*8){if(it%2==0)1000 else -500};stereo.process(ss);test(abs(ss[ss.size-2].toInt()) in 1500..2000);test(abs(ss.last().toInt()*2+ss[ss.size-2].toInt())<=2)
    val silent=ShortArray(10000);RmsNormalizer(rate,1).process(silent);test(silent.all{it==0.toShort()});test(runCatching{RmsNormalizer(10,1)}.isFailure)
    println("$count batch 14 pure Kotlin assertions passed")
}
