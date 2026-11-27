import com.abhi.ganasuno.player.EndlessQueuePolicy as P
fun main(){
 var n=0
 fun expect(b:Boolean){check(b);n++}
 expect(P.shouldRefill(true,true,false,true,false,5))
 expect(!P.shouldRefill(true,true,false,true,false,6))
 expect(!P.shouldRefill(false,true,true,true,false,0))
 expect(!P.shouldRefill(true,false,false,true,false,0))
 expect(P.shouldRefill(true,false,true,true,false,0))
 expect(!P.shouldRefill(true,true,false,false,false,0))
 expect(!P.shouldRefill(true,true,true,true,true,0))
 expect(P.fresh(listOf("a","b"),listOf("a","c","d"),setOf("b")){it}==listOf("a","c","d"))
 expect(P.fresh(emptyList(),(1..40).map{it.toString()},emptySet()){it}.size==15)
 expect(P.trimPrefix(100,false)==50)
 expect(P.trimPrefix(100,true)==0)
 // Simulated sequential refills/Next requests, with an ever-growing provider catalogue.
 val queue=(0..14).map{it.toString()}.toMutableList();val seen=mutableSetOf<String>();var current=0;var serial=15
 repeat(1000){
   expect(seen.add(queue[current]))
   if(P.shouldRefill(true,true,false,true,false,queue.lastIndex-current)){
      val fresh=P.fresh(emptyList(),(serial until serial+15).map{it.toString()},queue.toSet()){it};serial+=15
      val trim=P.trimPrefix(current,false);repeat(trim){queue.removeAt(0)};current-=trim;queue.addAll(fresh)
   }
   expect(current<queue.lastIndex);expect(queue.size<=71);current++
 }
 println("$n endless queue policy assertions passed (1000 simulated skips)")
}
