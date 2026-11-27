import com.abhi.ganasuno.data.*
import java.time.ZoneId
fun main(){
    var count=0;fun test(b:Boolean){check(b){"Assertion ${count+1}"};count++}
    test(Batch13.year("1999")==1999);test(Batch13.year("2020-03-01")==2020);test(Batch13.year("unknown")==null);test(Batch13.year("1880")==null);test(Batch13.year("20201231")==null);test(Batch13.year(null)==null)
    val names=listOf("Sony WH-1000XM4","Sony WH-1000XM5","Sennheiser HD 600")
    test(Batch13.profiles(names,"SONY xm4")==listOf(names[0]));test(Batch13.profiles(names,"HD 600")==listOf(names[2]));test(Batch13.profiles(names,"").size==3);test(Batch13.profiles(names,"bose").isEmpty());test(Batch13.profileName(" ")==null);test(Batch13.profileName("a\nb")==null);test(Batch13.profileName(" Test ")=="Test")
    val a=Song("a","A","Artist A","","");val b=Song("b","B","Artist B","","");val c=Song("c","C","artist a","","")
    val h=listOf(LibraryStore.Listen(a,0,60000),LibraryStore.Listen(b,86400000,60000),LibraryStore.Listen(c,86400000,60000));val zone=ZoneId.of("UTC")
    val dec=Batch13.decades(h,mapOf("a" to 1999,"b" to 2001));test(dec[1990]==60000L);test(dec[2000]==60000L);test(dec[null]==60000L);test(Batch13.decades(emptyList(),emptyMap()).isEmpty());test(Batch13.decades(h,mapOf("a" to 200)).keys==setOf(null))
    val m=Batch13.metrics(h,listOf(h[0]),4,zone)!!;test(m.consistency==50);test(m.discovery==67);test(m.concentration==67);test(m.replay==33);test(m.diversity in 40..50);test(Batch13.metrics(emptyList(),h,4,zone)==null);test(Batch13.metrics(h,emptyList(),4,zone)?.discovery==null)
    test(Batch13.archetype(m,3,2)=="Devotee");test(Batch13.archetype(Batch13.Metrics(80,10,10,40,50),20,5)=="Deep Diver");test(Batch13.archetype(Batch13.Metrics(80,10,80,10,5),20,18)=="Omnivore");test(Batch13.archetype(Batch13.Metrics(80,80,40,30,10),20,8)=="Explorer");test(Batch13.archetype(Batch13.Metrics(80,20,40,30,10),20,8)=="Regular")
    test(Batch13.fraction(50,100)==.5f);test(Batch13.fraction(-1,100)==0f);test(Batch13.fraction(200,100)==1f);test(Batch13.fraction(50,0)==0f)
    println("$count Batch 13 assertions passed")
}
