import com.abhi.ganasuno.ui.LrcParser
fun main() {
    check(LrcParser.parse("plain lyrics").isEmpty())
    val parsed = LrcParser.parse("[00:03.5]Third\n[00:01.25][00:02.125]First\n[ar:Example]\n[01:00]Minute")
    check(parsed.map { it.ms } == listOf(1250L,2125L,3500L,60000L))
    check(parsed.map { it.text } == listOf("First","First","Third","Minute"))
    println("LRC parser: 3 checks passed")
}
