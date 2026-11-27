import com.abhi.ganasuno.data.Batch27
import com.abhi.ganasuno.data.Song
fun main(){
    var n=0; fun test(v:Boolean){check(v){"Assertion ${n+1}"};n++}
    test(Batch27.comment(" Hi ")=="Hi");test(Batch27.comment("  ")==null);test(Batch27.comment("x".repeat(10000))!=null);test(Batch27.comment("x".repeat(10001))==null);test(Batch27.comment("a\nb\tc")!=null);test(Batch27.comment("a\u0000b")==null)
    test(Batch27.commentId("Ugz.a-b_123"));test(!Batch27.commentId(""));test(!Batch27.commentId("x/y"));test(!Batch27.commentId("x".repeat(201)))
    val a=Song("abcdefghijk","Local title","A","","");val b=Song("12345678901","Remote title","B","","");val c=b.copy(id="01234567890");val local=Song("local:content://x","Own file","","","",localUri="content://x")
    test(Batch27.merge(listOf(a,local),listOf(a.copy(title="Overwrite"),b,b))==listOf(a,local,b));test(Batch27.merge(listOf(a),listOf(c.copy(title=" "),c.copy(localUri="content://x"),c.copy(id="invalid")))==listOf(a));test(Batch27.merge(emptyList(),listOf(b,c))==listOf(b,c));test(Batch27.merge(listOf(a),emptyList())==listOf(a))
    val rank=Batch27.ranks(listOf(listOf(a.id,b.id,b.id),listOf(b.id,c.id),listOf(c.id,b.id)))
    test(rank.first().id==b.id);test(rank.first().countries==3);test(rank.first().score==58);test(rank[1].id==c.id);test(Batch27.ranks(emptyList()).isEmpty());test(Batch27.ranks(listOf(listOf("bad"))).isEmpty());test(Batch27.ranks(listOf(listOf(a.id),listOf(b.id))).map{it.id}==listOf(b.id,a.id))
    println("$n v27 standalone assertions passed")
}
