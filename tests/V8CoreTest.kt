import com.abhi.ganasuno.data.V8Core
fun main(){
    check(V8Core.playlistId("https://music.youtube.com/playlist?list=PLabcdefghijk") == "PLabcdefghijk")
    check(V8Core.playlistId("PLabcdefghijk") == "PLabcdefghijk")
    check(V8Core.playlistId("https://music.youtube.com.evil.test/playlist?list=PLabcdefghijk") == null)
    check(V8Core.playlistId("http://youtube.com/playlist?list=PLabcdefghijk") == null)
    check(V8Core.playlistId("https://www.youtube.com/playlist?list=bad") == null)
    check(V8Core.httpsUrl("https://example.com/album") != null)
    check(V8Core.httpsUrl("https://user:pass@example.com/") == null)
    check(V8Core.httpsUrl("javascript:alert(1)") == null)
    check(V8Core.related("A", listOf(listOf("a","B","b","C"),listOf("A","B"),listOf("D","C"))) == listOf("B","C"))
    check(V8Core.related("unknown",listOf(listOf("A","B"))).isEmpty())
    check(V8Core.preferredBitrate(listOf(128,256,320,384)) == 320)
    check(V8Core.preferredBitrate(listOf(0,-1,128,256)) == 256)
    check(V8Core.preferredBitrate(listOf(0,-1)) == null)
    println("13 v8 core assertions passed")
}
