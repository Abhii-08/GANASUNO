import com.abhi.ganasuno.data.Batch18
import com.abhi.ganasuno.data.Song
fun main(){var n=0;fun t(x:Boolean){check(x){"Assertion ${n+1}"};n++}
 fun local(name:String,artist:String="A",album:String="Album")=Song("local:content://music/$name",name,artist,"","",localUri="content://music/$name",album=album)
 val seed=local("seed");val a=local("a");val b=local("b","B");val live=local("c").copy(live=true);val online=Song("abcdefghijk","Online","A","","");val other=local("x","C","Other")
 t(Batch18.local(seed));t(!Batch18.local(online));t(!Batch18.local(seed.copy(id="bad")))
 t(Batch18.eligible(seed,listOf(a,b,live,online,other,a,seed),listOf("B"),emptySet(),{true}).map{it.id}==listOf(a.id,b.id))
 t(Batch18.eligible(seed,listOf(a,b),listOf("B"),setOf(a.id),{true}).map{it.id}==listOf(b.id))
 t(Batch18.eligible(seed,listOf(a,b),listOf("B"),emptySet(),{it.id!=a.id}).map{it.id}==listOf(b.id))
 t(Batch18.eligible(online,listOf(a,online.copy(id="abcdefghijx")),emptyList(),emptySet(),{true}).single().id=="abcdefghijx")
 t(Batch18.markerFractions(listOf(-1,0,500,500,1000,1001),1000)==listOf(0f,.5f,1f));t(Batch18.markerFractions(listOf(1),0).isEmpty())
 t(Batch18.projectedPosition(1000,true,2.0,200,10000)==1200L);t(Batch18.projectedPosition(1000,false,2.0,200,null)==1000L);t(Batch18.projectedPosition(900,true,2.0,1000,1000)==1000L);t(Batch18.projectedPosition(0,true,1.0,100000,null)==1000L)
 t(!Batch18.validSpeed(Double.NaN));t(!Batch18.validSpeed(3.0));t(Batch18.validSpeed(.5));t(Batch18.shouldSeek(0,1600,true,false));t(!Batch18.shouldSeek(0,1499,true,false));t(Batch18.shouldSeek(0,251,false,false));t(!Batch18.shouldSeek(0,5000,true,true));t(Batch18.albumKey(" Album ","Artist")==Batch18.albumKey("album","artist"));t(Batch18.albumKey("ab","c")!=Batch18.albumKey("a","bc"));t(Batch18.albumKey("Album","A")!=Batch18.albumKey("Album","B"));println("$n V18 core assertions passed")}
