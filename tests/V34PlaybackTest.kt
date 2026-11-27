import com.abhi.ganasuno.data.PlaybackRecovery as R
fun main() {
    val checks = listOf(
        R.validMediaUrl("https://example.org/test.mp3"),
        !R.validMediaUrl("http://example.org/test.mp3"),
        !R.validMediaUrl("https://user:pass@example.org/test.mp3"),
        !R.validMediaUrl("not-a-url"),
        !R.validMediaUrl(null),
        R.expired("https://example.org/a?expire=100",100000),
        R.expired("https://example.org/a?expire=129",100000),
        !R.expired("https://example.org/a?expire=131",100000),
        !R.expired("https://example.org/a",100000),
        !R.expired("https://example.org/a?expire=bad",100000),
        R.transient(2001,null,false),R.transient(2002,null,false),
        R.transient(2004,503,false),R.transient(2004,408,false),
        !R.transient(2004,401,false),!R.transient(2004,403,false),
        !R.transient(2004,429,false),!R.transient(2001,null,true),
        !R.transient(4001,null,false),
        R.description(2004,403,false,false).contains("denied access"),
        R.description(2004,429,false,false).contains("rate-limiting"),
        R.description(2000,null,true,false).contains("playable stream"),
        R.description(4001,null,false,false).contains("decode"),
        R.description(5001,null,false,false).contains("audio output"),
        R.description(2005,null,false,false).contains("missing")
    )
    checks.forEachIndexed { i, passed -> check(passed) { "Failed check ${i+1}" } }
    println("${checks.size}/${checks.size} playback policy checks passed")
}
