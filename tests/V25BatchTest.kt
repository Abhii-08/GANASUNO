import com.abhi.ganasuno.data.Batch25 as B
fun main(){
 var n=0
 fun verify(ok:Boolean){check(ok);n++}
 verify(B.cacheMiB(16)==16);verify(B.cacheMiB(512)==512);verify(B.cacheMiB(-1)==64);verify(B.cacheBytes(128)==134217728L)
 verify(B.selected(0).isEmpty());verify(B.selected(511).size==9);verify(B.selected(1)==listOf("sponsor"));verify(B.selected(256)==listOf("poi_highlight"))
 verify(B.videoId("kxOuG8jMIgI"));verify(!B.videoId("local:abc"));verify(!B.videoId("../bad"))
 val a=B.segment(1.0,4.0,"sponsor","skip")!!;val b=B.segment(3.0,5.0,"intro","skip")!!;val c=B.segment(5.0,6.0,"sponsor","skip")!!;val h=B.segment(2.0,2.0,"poi_highlight","poi")!!
 verify(a.start==1000L&&a.end==4000L);verify(B.segment(-1.0,4.0,"sponsor","skip")==null);verify(B.segment(Double.NaN,4.0,"sponsor","skip")==null);verify(B.segment(5.0,4.0,"sponsor","skip")==null);verify(B.segment(1.0,2.0,"poi_highlight","skip")==null);verify(B.segment(1.0,2.0,"intro","mute")==null)
 verify(B.target(1000,10000,listOf(a,b,c,h),511)==6000L);verify(B.target(1000,10000,listOf(a,b,c,h),1)==4000L);verify(B.target(0,10000,listOf(a),1)==null);verify(B.target(4000,10000,listOf(a),1)==null);verify(B.target(2000,10000,listOf(h),511)==null);verify(B.target(2000,3000,listOf(a),1)==null);verify(B.target(2000,-1,listOf(a),1)==null);verify(B.target(2000,10000,listOf(a),0)==null)
 verify(B.count("0")==0L);verify(B.count("-1")==null);verify(B.count("not a number")==null);verify(B.safeText("  x\u0000y  ",2)=="xy");verify(B.rankLabels(listOf("A","B"))==listOf("1. A","2. B"))
 println("$n v25 assertions passed")
}
