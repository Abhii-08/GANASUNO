from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
MAIN=(ROOT/'app/src/main/java/com/abhi/ganasuno/MainActivity.kt').read_text()
FLIP=(ROOT/'app/src/main/java/com/abhi/ganasuno/ui/AnimatedPlaylistGreeting.kt').read_text()
class HomeHeaderFlip(unittest.TestCase):
    def test_home_only_binding(self):
        home=MAIN.split('private fun home() {',1)[1].split('private fun offlineHome()',1)[0]
        self.assertIn('homeGreetingActive = true\n        refreshPlaylistGreeting()',home)
        self.assertEqual(MAIN.count('homeGreetingActive = true'),1)
        self.assertIn('if (!homeGreetingActive || page != "Home") return',MAIN)
        self.assertIn('playbackSource(controller?.currentMediaItem)',MAIN)
        self.assertNotIn('Good morning',home)
    def test_disable_on_other_screens(self):
        self.assertIn('private fun stopHomeGreeting() { homeGreetingActive = false; playlistGreeting.unbind() }',MAIN)
        for prefix in ['private fun selectPage(next:String){\n        ','private fun showToolsContent(){','private fun showLocalList(list:List<Song>,title:String,playlist:String?=null){\n        ']:
            self.assertIn(prefix+'clearPlaylistGreeting()',MAIN)
        self.assertIn('private fun showSongContent(){stopHomeGreeting()',MAIN)
        self.assertIn('b.sectionTitle.text = if (video) "Video results" else "Search results"',MAIN)
    def test_metadata_refresh(self):
        self.assertIn('onMediaMetadataChanged(mediaMetadata: MediaMetadata) { refreshPlaylistGreeting() }',MAIN)
        self.assertIn('onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) { refreshPlaylistGreeting() }',MAIN)
        self.assertIn('val item = c.currentMediaItem\n        refreshPlaylistGreeting()',MAIN)
        self.assertIn('startQueue(songs, index, displayedSource)',MAIN)
        self.assertIn('saved.sources.getOrNull(it.index)',MAIN)
    def test_only_header_view_and_no_layout_mutation(self):
        self.assertIn('AnimatedPlaylistGreeting(b.headerFlipCard, b.sectionTitle, b.sectionSubtitle)',MAIN)
        self.assertIn('title.maxLines = 1',FLIP)
        self.assertIn('title.maxLines = originalMaxLines',FLIP)
        self.assertIn('height = originalTitleHeight',FLIP)
        self.assertNotIn('setBackground',FLIP)
        self.assertNotIn('textSize =',FLIP)
        self.assertIn('progress >= .5f',FLIP)
    def test_reduced_effects_and_lifecycle(self):
        for t in ('areAnimatorsEnabled()','!reduced()','isTouchExplorationEnabled','hasWindowFocus()','card.removeCallbacks(next)','removeAllUpdateListeners()','card.removeOnAttachStateChangeListener'):
            self.assertIn(t,FLIP)
        self.assertIn('playlistGreeting.setForeground(false)',MAIN)
if __name__=='__main__':unittest.main()
