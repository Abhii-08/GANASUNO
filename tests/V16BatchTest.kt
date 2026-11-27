import com.abhi.ganasuno.data.Batch16
import java.time.Instant

fun main(){
    var n=0
    fun test(value:Boolean){check(value){"Assertion ${n+1} failed"};n++}
    val v="aBcDeFgH123";val w="aBcDeFgH124";val x="aBcDeFgH125";val c="UC"+"a".repeat(22);val p="PL"+"a".repeat(22)
    test(Batch16.valid("video",v));test(!Batch16.valid("video",v+"a"));test(!Batch16.valid("video","abc?x=12345"));test(Batch16.valid("channel",c));test(!Batch16.valid("channel",p));test(Batch16.valid("playlist",p));test(!Batch16.valid("unknown",v));test(Batch16.publicUrl("video",v)=="https://www.youtube.com/watch?v=$v");test(Batch16.publicUrl("channel",c).endsWith(c));test(Batch16.publicUrl("playlist",p).endsWith("list=$p"));test(runCatching{Batch16.publicUrl("video","bad")}.isFailure)
    test(Batch16.homeLimit(-3)==10);test(Batch16.homeLimit(30)==30);test(Batch16.homeLimit(999)==100);test(Batch16.pageSize(52)==50);test(Batch16.pageSize(0)==1)
    test(Batch16.since(Instant.parse("2026-11-25T00:00:00Z"),30)=="2026-10-26T00:00:00Z")
    val day=86400000L;test(Batch16.fresh(day,day+6*day));test(!Batch16.fresh(day,day+7*day));test(!Batch16.fresh(day,day-1));test(!Batch16.fresh(0,day));test(Batch16.due(0,day));test(!Batch16.due(day,day+1));test(Batch16.due(day,2*day));test(Batch16.due(day,day-1));test(Batch16.keyLooksValid("a".repeat(39)));test(!Batch16.keyLooksValid("secret\nkey"));test(!Batch16.keyLooksValid("short"));test(Batch16.stats("Likes",null)=="Likes: Unavailable");test(Batch16.stats("Views","0")=="Views: 0");test(Batch16.stats("Views","12x").endsWith("Unavailable"));test(Batch16.clean("abc\u0000def",4)=="abcd")
    test(Batch16.alternate(v,"A Song",listOf(v to "A Song",w to "A Song (Live)",x to "Another",w to "A Song remix"))==listOf(w));test(Batch16.alternate(v,"",listOf(w to "Song")).isEmpty())
    val requests=mutableListOf<Pair<Int,String>>();val rows=Batch16.collect(3,{it:Int->it}){size,token->requests.add(size to token);if(token.isEmpty())listOf(1,2) to "second" else listOf(2,3,4) to "third"};test(rows==listOf(1,2,3));test(requests==listOf(3 to "",1 to "second"))
    var calls=0;test(Batch16.collect(10,{it:Int->it}){_,_->calls++;listOf(1) to "loop"}==listOf(1));test(calls==2)
    calls=0;test(Batch16.collect(100,{it:Int->it}){_,_->calls++;listOf(calls) to "p$calls"}.size==5);test(calls==5)
    test(Batch16.collect(20,{it:Int->it}){_,_->emptyList<Int>() to ""}.isEmpty());test(runCatching{Batch16.collect(0,{it:Int->it}){_,_->emptyList<Int>() to ""}}.isFailure)
    println("$n V16 core assertions passed")
}
