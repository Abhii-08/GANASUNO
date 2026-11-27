import com.abhi.ganasuno.data.Batch26
import com.abhi.ganasuno.data.Song

fun main(){
    var n=0
    fun test(ok:Boolean){check(ok){"Assertion ${n+1} failed"};n++}
    val a=Song("abcdefghijk","A","Artist","3:00","")
    val b=a.copy(id="12345678901")
    test(Batch26.eligible(listOf(a,b,a))==listOf(a.id,b.id))
    test(Batch26.eligible(listOf(a.copy(localUri="content://sample"))).isEmpty())
    test(Batch26.eligible(listOf(a.copy(live=true))).isEmpty())
    test(Batch26.eligible(listOf(a.copy(id="bad"))).isEmpty())
    test(Batch26.missing(listOf(a.id,b.id,a.id),listOf(a.id))==listOf(b.id))
    test(Batch26.missing(listOf(a.id),listOf(a.id)).isEmpty())
    test(Batch26.missing(emptyList(),emptyList()).isEmpty())
    for(v in listOf("like","dislike","none"))test(Batch26.rating(v))
    test(!Batch26.rating("LIKE"));test(!Batch26.rating(""))
    test(Batch26.https("https://example.org/feed")!=null)
    for(v in listOf("http://example.org","https://user:pass@example.org/","https://example.org/#frag","https://example.org:8080/","file:///tmp/a","not-a-url"))test(Batch26.https(v)==null)
    val p=Batch26.post("one","First","https://example.org/1")!!
    val q=Batch26.post("two","Second","https://example.org/2")!!
    test(p.id.length==64);test(p.id!=q.id)
    test(Batch26.post("one","Updated","https://example.org/other")!!.id==p.id)
    test(Batch26.post("","","https://example.org/")==null)
    test(Batch26.post("","Bad","javascript:alert(1)")==null)
    test(Batch26.post("","Good","https://example.org/1")!!.id==Batch26.post("","Changed","https://example.org/1")!!.id)
    test(Batch26.newPosts(listOf(p,q),emptySet()).isEmpty())
    test(Batch26.newPosts(listOf(p,q,q),setOf(p.id))==listOf(q))
    test(Batch26.newPosts(listOf(p,q),setOf(p.id,q.id)).isEmpty())
    test(Batch26.seen(listOf(q,p),listOf(p.id))==listOf(q.id,p.id))
    test(Batch26.seen(emptyList(),(1..600).map{it.toString()}).size==500)
    println("$n v26 standalone assertions passed")
}
