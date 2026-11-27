"""Source contracts for the bounded player dialog. Device QA covers rendering."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
SOURCE = (ROOT / "app/src/main/java/com/abhi/ganasuno/MainActivity.kt").read_text(encoding="utf-8")
PLAYER = SOURCE.split("private fun nowPlaying(", 1)[1].split("private fun lyricsDialog(", 1)[0]


class PlayerLayout(unittest.TestCase):
    def test_dialog_has_bounded_panel_not_artwork_scroll(self):
        self.assertIn(".setView(panel)", PLAYER)
        self.assertIn('.setPositiveButton("Close", null)', PLAYER)
        self.assertIn("panel.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))", PLAYER)
        self.assertIn("config.screenHeightDp - 32", PLAYER)

    def test_normal_action_rows_not_inside_scroll(self):
        self.assertIn("panel.addView(controls, LinearLayout.LayoutParams(-1, -2))", PLAYER)
        self.assertNotIn("layout.addView(controls", PLAYER)
        self.assertIn("for (row in listOf(extra, modes, tools)) panel.addView(row", PLAYER)
        self.assertIn("orientation = LinearLayout.VERTICAL", PLAYER)

    def test_compact_fallback(self):
        compact = PLAYER.split("if (compactControls) {", 1)[1].split("} else {", 1)[0]
        for row, index in (("extra", 0), ("modes", 1), ("tools", 2)):
            self.assertIn(f"layout.addView({row}, {index})", compact)
        self.assertNotIn("layout.addView(controls", compact)
        self.assertRegex(PLAYER, r"config.screenHeightDp < \d+ \|\| config.fontScale > [0-9.]+f")

    def test_playback_buttons_and_unique_menu(self):
        for expected in (
            'transport(R.drawable.ic_prev,"Previous track"){skipTrack(false)}',
            '{togglePlayback(c)}',
            'transport(R.drawable.ic_next,"Next track"){skipTrack(true)}',
            'button(extra,"Queue"){artworkQueue()}',
            'button(extra,"Lyrics")',
            "c.shuffleModeEnabled",
            "c.repeatMode",
            "0xfff4c2c2.toInt()",
        ):
            self.assertIn(expected, PLAYER)
        self.assertEqual(PLAYER.count("playerMore(fullscreen)"), 1)
        self.assertNotIn('setNeutralButton("Controls")', PLAYER)
        self.assertIn("minimumHeight=dp(48)", PLAYER)
        self.assertIn("dp(if(large)64 else 54)", PLAYER)

    def test_artwork_and_player_lifecycle(self):
        for expected in (
            "PremiumUi.applyArtwork(panel,bitmap,0f)",
            "ui.removeCallbacks(refresh)",
            "syncFavourite()",
            "waveHolder",
        ):
            self.assertIn(expected, PLAYER)


if __name__ == "__main__":
    unittest.main()
