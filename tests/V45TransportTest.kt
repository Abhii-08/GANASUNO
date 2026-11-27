import com.abhi.ganasuno.player.TransportPolicy
import com.abhi.ganasuno.player.TransportPolicy.Action
fun main() {
    var count=0
    fun checkCase(value:Boolean){check(value);count++}
    val p=TransportPolicy
    checkCase(p.toggle(false,false,true,false,false)==Action.EMPTY)
    checkCase(p.toggle(true,true,true,false,true)==Action.ERROR)
    checkCase(p.toggle(true,true,false,false,false)==Action.PAUSE)
    checkCase(p.toggle(true,false,false,false,false)==Action.PLAY)
    checkCase(p.toggle(true,false,true,false,false)==Action.PREPARE)
    checkCase(p.toggle(true,true,false,true,false)==Action.RESTART)
    checkCase(p.toggle(true,false,false,true,false)==Action.RESTART)
    for(forward in listOf(true,false)) for(next in listOf(true,false)) for(prev in listOf(true,false)) for(idle in listOf(true,false)) for(ended in listOf(true,false)) for(error in listOf(true,false)) {
        val v=p.skip(forward,next,prev,idle,ended,error)
        val can=if(forward)next else prev
        checkCase(v.seek==can && v.prepare==(can&&(idle||ended||error)))
    }
    println("$count transport policy assertions passed")
}
