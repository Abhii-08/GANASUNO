import com.abhi.ganasuno.data.Batch19
fun main(){
 var n=0;fun ck(b:Boolean){check(b);n++}
 val key="a".repeat(32)
 val d=Batch19.destination("https://$key@ingest.example.org/123")!!
 ck(d.endpoint=="https://ingest.example.org/api/123/envelope/");ck(d.key==key)
 ck(Batch19.destination("https://$key@ingest.example.org/sentry/123")?.endpoint=="https://ingest.example.org/sentry/api/123/envelope/")
 for(raw in listOf("http://$key@x.com/123","https://$key@x.com/abc","https://$key@x.com/123?q=secret","https://$key:secret@x.com/123","https://$key@x.com:8080/123","https://$key@x.com/../123","https://$key@x.com/123#secret","https://x.com/123"))ck(Batch19.destination(raw)==null)
 ck(Batch19.safeReport("Uncaught java.lang.Exception\nAndroid API 36\ncom.app.Foo.bar:42\nsecret=https://example.com\nmessage text")=="Uncaught java.lang.Exception\nAndroid API 36\ncom.app.Foo.bar:42")
 ck(Batch19.safeReport("token=secret").isBlank());ck(Batch19.bitrate(2400000,60000)==320);ck(Batch19.bitrate(0,1)==null);ck(Batch19.bitrate(100,0)==null)
 ck(Batch19.bpm(" 120 ")==120);ck(Batch19.bpm("19")==null);ck(Batch19.bpm("301")==null);ck(Batch19.key("F♯ minor")=="F# minor");ck(Batch19.key("B♭ major")=="Bb major");ck(Batch19.key("D dorian")=="D dorian");ck(Batch19.key("X major")==null);ck(Batch19.scale("D dorian")=="dorian");ck(Batch19.scale(null)==null)
 println("$n standalone assertions passed")
}
