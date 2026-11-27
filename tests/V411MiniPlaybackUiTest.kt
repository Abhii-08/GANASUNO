import com.abhi.ganasuno.ui.MiniPlaybackUi

fun main() {
    var count = 0
    fun state(label: String, hasItem: Boolean = true, requested: Boolean = false,
              buffering: Boolean = false, playing: Boolean = false, error: Boolean = false,
              loading: Boolean = false, pause: Boolean = false) {
        val actual = MiniPlaybackUi.resolve(hasItem, requested, buffering, playing, error)
        check(actual == MiniPlaybackUi(loading, pause)) { "$label: $actual" }
        count++
    }
    state("no item", hasItem = false, requested = true, buffering = true)
    state("restored queue pre-buffer", buffering = true)
    state("restored ready")
    state("play requested, buffering", requested = true, buffering = true, loading = true, pause = true)
    state("ready and playing", requested = true, playing = true, pause = true)
    state("rebuffer during playback", requested = true, buffering = true, loading = true, pause = true)
    state("paused while buffering", buffering = true)
    state("paused ready")
    state("failed while buffering", requested = true, buffering = true, error = true)
    state("error, idle", requested = true, error = true)
    state("retry requested", requested = true, buffering = true, loading = true, pause = true)
    state("ended", requested = true)
    state("ready but temporarily suppressed", requested = true)
    println("$count mini-player state scenarios passed")
}
