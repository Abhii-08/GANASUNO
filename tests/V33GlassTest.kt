import com.abhi.ganasuno.ui.GlassMath
fun main(){
    check(GlassMath.spring(0f)==0f);check(GlassMath.spring(1f)==1f)
    check((0..100).all{GlassMath.spring(it/100f).isFinite()})
    check((0..100).all{GlassMath.spring(it/100f) in 0f..1.3f})
    val constant=IntArray(35){0xff123456.toInt()};GlassMath.blur(constant,IntArray(35),7,5,2);check(constant.all{it==0xff123456.toInt()})
    val line=IntArray(9){if(it==4)0xffffffff.toInt() else 0xff000000.toInt()};GlassMath.blur(line,IntArray(9),9,1,1);check((line[3] and 255)>0);check((line[4] and 255)<255);check(line[3]==line[5]);check(line.all{it ushr 24==255})
    val single=intArrayOf(0xffabcdef.toInt());GlassMath.blur(single,IntArray(1),1,1,8);check(single[0]==0xffabcdef.toInt())
    println("10 glass math assertions passed")
}
