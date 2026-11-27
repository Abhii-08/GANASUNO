import com.abhi.ganasuno.data.PortablePlaylists
import com.abhi.ganasuno.ui.WordLrc
fun main(){
    check(PortablePlaylists.parse("https://music.youtube.com/watch?v=abcdefghijk\nhttps://youtu.be/abcdefghijk\nABCDEFGHIJK\ninvalid") == listOf("abcdefghijk","ABCDEFGHIJK"))
    check(PortablePlaylists.parse("https://youtube.com/watch?v=abcdefghijkl").isEmpty())
    check(PortablePlaylists.export(listOf("abcdefghijk","bad")) == "https://music.youtube.com/watch?v=abcdefghijk")
    val line="[00:01.000]<00:01.000>Hello <00:02.30>world"
    check(WordLrc.parse(line).map{it.time} == listOf(1000L,2300L))
    check(WordLrc.parse("<00:03>third <00:02>second").isEmpty())
    check(WordLrc.valid(line));check(!WordLrc.valid("[00:01]<00:62>bad"));check(WordLrc.plain("<00:01>Hello") == "Hello")
    println("8 portable playlist and enhanced LRC checks passed")
}
