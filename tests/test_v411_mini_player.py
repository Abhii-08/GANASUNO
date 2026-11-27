"""Source wiring and layout checks; not Android instrumentation."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT/'app/src/main/java/com/abhi/ganasuno'
S = (SRC/'MainActivity.kt').read_text()
A = '{http://schemas.android.com/apk/res/android}'
class MiniPlayer(unittest.TestCase):
    def test_restoration_remains_paused(self):
        restore = S.split('private fun restoreQueue(')[1].split('private fun playVideo(')[0]
        self.assertIn('c.prepare()', restore)
        self.assertNotIn('c.play()', restore)
    def test_loading_inputs(self):
        self.assertIn('playWhenReady = c.playWhenReady', S)
        self.assertIn('buffering = c.playbackState == Player.STATE_BUFFERING', S)
        self.assertIn('hasError = c.playerError != null', S)
        self.assertIn('if (state.showLoading) View.VISIBLE else View.GONE', S)
        self.assertIn('if (state.showPause) R.drawable.ic_pause else R.drawable.ic_play', S)
    def test_updates_during_buffering_and_error(self):
        self.assertIn('onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) = syncMini()', S)
        self.assertIn('onPlaybackStateChanged(playbackState: Int) = syncMini()', S)
        self.assertIn('onPlayerError(error: PlaybackException) {\n                syncMini()', S)
        self.assertIn('controller ?: run { b.miniBuffering.visibility = View.GONE; return }', S)
    def test_spaced_buttons_keep_targets(self):
        root=ET.parse(ROOT/'app/src/main/res/layout/activity_main.xml').getroot()
        by_id={v.get(A+'id'):v for v in root.iter()}
        for key in ('miniPrev','miniPlay','miniNext'):
            view=by_id['@+id/'+key]
            self.assertEqual(view.get(A+'layout_width'),'48dp')
            self.assertEqual(view.get(A+'layout_height'),'48dp')
        for key in ('miniPlay','miniNext'):
            self.assertEqual(by_id['@+id/'+key].get(A+'layout_marginStart'),'6dp')
        self.assertIn('if(b.miniPrev.visibility==View.VISIBLE)dp(6) else 0', S)
        self.assertIn('dp(if(resources.configuration.screenWidthDp<360)4 else 6)', S)
    def test_spinner_hidden_and_neutral(self):
        root=ET.parse(ROOT/'app/src/main/res/layout/activity_main.xml').getroot()
        spinner=next(v for v in root.iter() if v.get(A+'id')=='@+id/miniBuffering')
        self.assertEqual(spinner.get(A+'visibility'),'gone')
        self.assertEqual(spinner.get(A+'indeterminateTint'),'#8E8E93')
        self.assertIn('b.miniBuffering.indeterminateTintList=android.content.res.ColorStateList.valueOf(0xff8e8e93.toInt())',S)
if __name__=='__main__': unittest.main()
