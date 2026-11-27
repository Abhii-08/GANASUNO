import com.abhi.ganasuno.data.*
fun main() {
    val song=Song("test","Test track","Test artist","","",false,"")
    val entry=LibraryStore.Listen(song,1700000000000L,120000L)
    val report=Insights.report(listOf(entry),listOf(entry.copy(ms=60000)),7)
    check("Listening time: 2 min" in report)
    check("Difference: 1 min" in report)
    check("Test artist" in report)
    check("100%" in report)
    check(Insights.report(emptyList(),emptyList(),7).startsWith("No listening data"))
    println("Insights: 5 checks passed")
}
