from pathlib import Path
import unittest
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'app/src/main/java/com/abhi/ganasuno'
MAIN=(SRC/'MainActivity.kt').read_text()
FLIP=(SRC/'ui/AnimatedPlaylistGreeting.kt').read_text()
class PlaylistFlip(unittest.TestCase):
 def test_wiring(self):
  for token in ['AnimatedPlaylistGreeting(b.headerFlipCard, b.sectionTitle, b.sectionSubtitle)','startQueue(songs, index, displayedSource)','browse.categoryName(query)','startQueue(playable,0,title)','displayedSource=title','onMediaMetadataChanged','onTimelineChanged','playlistGreeting.bind','clearPlaylistGreeting()']:
   self.assertIn(token,MAIN)
 def test_metadata_persistence(self):
  for token in ['extras.putString(com.abhi.ganasuno.ui.PlaylistGreeting.SOURCE_KEY, it)','saved.sources.getOrNull(it.index)','playbackSource(c.getMediaItemAt(it))']:
   self.assertIn(token,MAIN)
  queue=(SRC/'data/QueueStore.kt').read_text()
  self.assertIn('sources.getOrNull(index)',queue);self.assertIn('o.optString("source", "")',queue)
 def test_lifecycle_and_animation(self):
  for token in ['card.rotationY','progress >= .5f','card.removeCallbacks(next)','animator != null','getGlobalVisibleRect','hasWindowFocus','areAnimatorsEnabled','isTouchExplorationEnabled','ACTION_UP','CLOCK_TICK','title.maxLines = 1','removeOnAttachStateChangeListener']:
   self.assertIn(token,FLIP)
  for token in ['playlistGreeting.setForeground(true)','playlistGreeting.setForeground(false)','playlistGreeting.dispose()']:
   self.assertIn(token,MAIN)
 def test_no_audio_controls_in_flip(self):
  for token in ['MediaController','prepare()','seekTo(','setMediaItems','play()']:
   self.assertNotIn(token,FLIP)
if __name__=='__main__':unittest.main()
