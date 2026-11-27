import unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'app/src/main/java/com/abhi/ganasuno'
S=(BASE/'player/PlaybackService.kt').read_text(); A=(BASE/'MainActivity.kt').read_text()
class EndlessQueue(unittest.TestCase):
 def test_next_paths(self):
  self.assertIn('b.miniNext.setOnClickListener { nextTrack() }',A)
  self.assertIn('button(controls,"››"){nextTrack()}',A)
  self.assertIn('else nextTrack()',A)
  self.assertIn('PlayerHub.nextTrack = { requestNext(player) }',S)
 def test_refill_and_retry(self):
  self.assertIn('LibraryStore.option("endless",1)==1',S)
  self.assertIn('EndlessQueuePolicy.shouldRefill',S)
  self.assertIn('mixRetryAt=SystemClock.elapsedRealtime()+EndlessQueuePolicy.RETRY_MS',S)
  self.assertNotIn('attemptedMix',S);self.assertNotIn('p.mediaItemCount>=500',S)
 def test_safe_append(self):
  for x in ['generation!=mixGeneration','current !in original','LibraryStore.allowed(it.id)','p.addMediaItems','if(advance && p.currentMediaItem==current)','PlayerHub.roomActive','recentMixIds','trimPrefix']:
   self.assertIn(x,S)
