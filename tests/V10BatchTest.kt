import com.abhi.ganasuno.data.*
fun main(){
 var n=0;fun verify(v:Boolean){check(v){"Check ${n+1} failed"};n++}
 verify(Batch10.fade(0f,1f,0,1000)==0f);verify(Batch10.fade(0f,1f,500,1000)==.5f);verify(Batch10.fade(0f,1f,1500,1000)==1f);verify(Batch10.fade(1f,0f,500,1000)==.5f);verify(Batch10.fade(1f,0f,-20,1000)==1f);verify(Batch10.fade(1f,0f,1,0)==0f)
 verify(Batch10.crop(1200,800,.5f,.5f).contentEquals(intArrayOf(200,0,800)));verify(Batch10.crop(800,1200,0f,1f).contentEquals(intArrayOf(0,400,800)));verify(Batch10.crop(800,800,1f,0f).contentEquals(intArrayOf(0,0,800)));verify(Batch10.crop(1200,800,2f,0f).contentEquals(intArrayOf(400,0,800)));verify(runCatching{Batch10.crop(0,800,.5f,.5f)}.isFailure)
 val a=Song("a","Alpha","A","","");val b=Song("b","Beta","B","","");val c=Song("c","Gamma","C","","");verify(Batch10.ranked(mapOf(a to 3L,b to 7L,c to 3L)).map{it.first}==listOf(b,a,c));verify(Batch10.ranked(emptyMap()).isEmpty())
 verify(Batch10.explicitBadge("MUSIC_EXPLICIT_BADGE"));verify(Batch10.explicitBadge("explicit"));verify(!Batch10.explicitBadge("LIVE"));verify(!Batch10.explicitBadge(""))
 verify(Batch10.PLAY_THRESHOLD_MS==5000L)
 println("V10 batch: $n checks passed")
}
