from pathlib import Path
import unittest
R=Path(__file__).resolve().parents[1]/'app/src/main/java/com/abhi/ganasuno'
S=(R/'ui/SmartSettingIndicators.kt').read_text(); M=(R/'MainActivity.kt').read_text()
class SmartIndicators(unittest.TestCase):
 def test_rotation_distinct_from_artwork(self):
  self.assertIn('"Rotation" -> opt("rotate")',S)
  self.assertIn('"Animated cover artwork" -> opt("animated_art")',S)
  self.assertIn('save("rotate",it)',M)
 def test_native_switch_preserves_action(self):
  self.assertIn('labels==listOf("Off","On")',S)
  self.assertIn('isChecked=checked==1',S)
  self.assertIn('action(if(enabled)1 else 0)',S)
 def test_real_player_state(self):
  for expr in ['it.shuffleModeEnabled','it.repeatMode','it.playbackParameters.speed','player?.playbackParameters?.pitch']:
   self.assertIn(expr,S)
 def test_unknown_not_marked(self):
  self.assertIn('if(selected==null)return false',S)
  self.assertIn('selected.takeIf{it in labels.indices} ?: -1',S)
  self.assertIn('Custom / no matching preset',S)
 def test_live_rows_and_cleanup(self):
  self.assertIn('refreshSettingIndicators()',M)
  self.assertIn('settingCards.clear()',M)
  self.assertIn('card.setCurrentStatus',M)
  self.assertIn('if(statusLabel.text.toString()==label)return',(R/'ui/GlassFeatureCard.kt').read_text())
 def test_original_menu_wiring(self):
  self.assertIn('SmartSettingIndicators.selection(this,title,controller)',M)
  self.assertIn('SmartSettingIndicators.selection(a,title,player())',(R/'ui/AdvancedTools.kt').read_text())
 def test_actual_prefs_and_timer(self):
  for expr in ['Prefs.audioQuality','Prefs.skipSilence','Prefs.normalize','SleepTimer.isActive','opt("reverb_mix",35)']:
   self.assertIn(expr,S)
 def test_custom_eq_curve(self):
  self.assertIn('curves.indexOf(List(10){LibraryStore.option("dsp_band_$it") })',(R/'ui/TenBandDialog.kt').read_text())
