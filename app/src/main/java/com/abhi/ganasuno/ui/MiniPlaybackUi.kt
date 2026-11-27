package com.abhi.ganasuno.ui

/** Pure mini-player presentation policy; no playback or queue side effects. */
internal data class MiniPlaybackUi(val showLoading: Boolean, val showPause: Boolean) {
    companion object {
        fun resolve(hasItem: Boolean, playWhenReady: Boolean, buffering: Boolean,
                    isPlaying: Boolean, hasError: Boolean): MiniPlaybackUi {
            val activeRequest = hasItem && playWhenReady && !hasError
            val loading = activeRequest && buffering
            return MiniPlaybackUi(loading, hasItem && !hasError && (isPlaying || loading))
        }
    }
}
