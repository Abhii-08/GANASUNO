import com.abhi.ganasuno.ui.PlaylistGreeting as G
fun main() {
    var n = 0
    fun checkThat(ok: Boolean) { check(ok); n++ }
    for (h in 0..23) checkThat(G.greeting(h) == when(h){in 5..11->"Good Morning";in 12..16->"Good Afternoon";in 17..20->"Good Evening";else->"Good Night"})
    checkThat(G.source(null)=="Now Playing"); checkThat(G.source("  ")=="Now Playing"); checkThat(G.source(" Custom playlist ")=="Custom playlist")
    checkThat(G.reverse(5,0)=="Good Morning"); checkThat(G.reverse(21,2)=="Good Night"); checkThat(G.reverse(13,1)=="Enjoy Your Music"); checkThat(G.reverse(13,3)=="Music Is Life")
    checkThat((0..29).filter{it%2==1}.map{G.reverse(10,it)}.distinct().size==15)
    checkThat(G.FRONT_MS==6000L && G.BACK_MS==3000L && G.FLIP_MS in 650L..850L)
    for (step in 0..100) checkThat(G.angle(step/100f) in -90f..90f)
    checkThat(G.angle(0f)==0f && G.angle(.5f)==-90f && G.angle(1f)==0f)
    println("$n greeting and rotation JVM assertions passed")
}
