from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
MAIN=ROOT/'app/src/main'; SRC=MAIN/'java/com/abhi/ganasuno'
class Playback(unittest.TestCase):
 def test_english(self):
  strings={n.attrib['name']:n.text for n in ET.parse(MAIN/'res/values/strings.xml').getroot()}
  self.assertEqual(strings['search_hint'],'Search for music')
  self.assertEqual(strings['no_results'],'No results found')
  self.assertEqual(strings['play_failed'],'Unable to play this track. Tap Play to retry.')
  self.assertIn('%1$s',strings['video_failed']);self.assertIn('%1$d',strings['sleep_remaining'])
 def test_resolver(self):
  s=(SRC/'data/Streams.kt').read_text();self.assertIn('PlaybackRecovery.validMediaUrl',s);self.assertIn('PlaybackRecovery.expired',s);self.assertIn('muxed != null -> Resolved(muxed.content, false)',s)
 def test_ui_retry(self):
  s=(SRC/'MainActivity.kt').read_text();self.assertIn('togglePlayback(it)',s);self.assertIn('togglePlayback(c)',s);self.assertIn('Tap Play for options.',s);self.assertIn('c.prepare();c.play()',s);self.assertIn('View diagnostics',s)
 def test_bounded_retry(self):
  s=(SRC/'player/PlaybackService.kt').read_text();self.assertIn('retriedItem!=id',s);self.assertIn('failure.retryable',s);self.assertIn('player.playerError!=null',s);self.assertIn('PlaybackFailures.inspect',s)
 def test_redaction(self):
  s=(SRC/'player/PlaybackFailures.kt').read_text();self.assertNotIn('.message',s);self.assertIn('responseCode',s);self.assertIn('error.errorCode',s)
if __name__=='__main__':unittest.main()
