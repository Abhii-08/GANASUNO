"""Source contracts for the player measurement fix; not an Android rendering test."""
from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[1]
S=(R/'app/src/main/java/com/abhi/ganasuno/MainActivity.kt').read_text()
P=S.split('private fun nowPlaying(',1)[1].split('private fun lyricsDialog(',1)[0]
class PlayerLayout(unittest.TestCase):
    def test_dialog_has_bounded_panel_not_artwork_scroll(self):
        self.assertIn('.setView(panel).setPositiveButton("Close",null)',P)
        self.assertIn('panel.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))',P)
        self.assertIn('config.screenHeightDp-48',P)
    def test_normal_action_rows_not_inside_scroll(self):
        normal=P.split('if(compactControls){',1)[1].split('}else{',1)[1].split('val dialog=',1)[0]
        self.assertIn('attachRow(panel,controls);attachRow(panel,extra);attachRow(panel,modes)',normal)
        self.assertIn('LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(4)}',P)
        self.assertIn('orientation=LinearLayout.HORIZONTAL',P)
    def test_compact_fallback(self):
        compact=P.split('if(compactControls){',1)[1].split('}else{',1)[0]
        for expected in ('attachRow(panel,controls)','layout.addView(extra,0,','layout.addView(modes,1,'):
            self.assertIn(expected,compact)
        self.assertIn('config.fontScale>1.45f',P)
        self.assertIn('config.screenHeightDp-390',P)
    def test_playback_buttons_and_unique_menu(self):
        for x in ('c.seekToPreviousMediaItem()', 'togglePlayback(c)', 'nextTrack()', 'button(extra,"Queue"){artworkQueue()}', 'button(extra,"Lyrics")', 'c.shuffleModeEnabled', 'c.repeatMode', 'playerMore(fullscreen)', '0xfff4c2c2.toInt()'):
            self.assertIn(x,P)
        self.assertEqual(P.count('playerMore(fullscreen)'),1)
        self.assertNotIn('setNeutralButton("Controls")',P)
        self.assertIn('minHeight=dp(48);minimumHeight=dp(48)',P)
    def test_artwork_and_player_lifecycle(self):
        self.assertIn('PremiumUi.applyArtwork(panel,bitmap,0f)',P)
        self.assertIn('ui.removeCallbacks(refresh)',P)
        self.assertIn('syncFavourite()',P)
        self.assertIn('waveHolder',P)
if __name__=='__main__':unittest.main()
