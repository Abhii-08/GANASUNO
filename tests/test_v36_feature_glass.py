"""Source contracts for Library/Settings glass cards (not runtime UI tests)."""
from pathlib import Path
import re, unittest, xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'app/src/main/java/com/abhi/ganasuno'
RES=ROOT/'app/src/main/res'
MAIN=(SRC/'MainActivity.kt').read_text()
CARD=(SRC/'ui/GlassFeatureCard.kt').read_text()
class FeatureGlass(unittest.TestCase):
 def test_reference_icons(self):
  for label,icon,color in [('favourites','heart','a43f65'),('offline audio','download','3e639b'),('import audio','upload','7452a3'),('artists and albums','people','2c797c'),('favourite collections','collection','98743f'),('podcasts','podcast','725ca5'),('followed artists','follow','326a98'),('search library','search','636870')]:
   line=next(line for line in CARD.splitlines() if f't=="{label}"' in line)
   self.assertIn(f'R.drawable.glass_feature_{icon},0xff{color}',line)
  names=re.findall(r'R.drawable.(glass_feature_\w+)',CARD)
  for name in names:
   xml=ET.parse(RES/'drawable'/f'{name}.xml').getroot()
   self.assertEqual(xml.tag,'vector'); self.assertTrue(xml.findall('path'))
 def test_routes(self):
  lib=MAIN.split('private fun openLibrary(){',1)[1].split('private fun renderRows',1)[0]
  for route in ['showLocalList(LibraryStore.list("favorites")','localCatalogue()','favouriteCollections()','podcastMenu()','advanced.artists()','catalogueTracks().filter','libraryMenu()','playlistsMenu()','mostPlayedMenu()','offlineMenu()','backupHub()']:
   self.assertIn(route,lib)
  for route in ['showLocalList(LibraryStore.list("local")','importAudio.launch','catalogueGroups(true)','catalogueGroups(false)','LibraryStore.option("ui_grid",it,true)','playlistsMenu()','playlistDetails(name)','LibraryStore.mostPlayed()','backupMenu()']:
   self.assertIn(route,MAIN)
  self.assertIn('LibraryStore.list("favorites").size',lib);self.assertIn('LibraryStore.list("local").size',lib)
  self.assertIn('row.detail,reduced,row.action',MAIN)
  self.assertIn('Your music, organized in one place.',MAIN)
 def test_material_and_accessibility(self):
  for token in ['LayerDrawable','GradientDrawable','RippleDrawable','minimumHeight=dp(76)','contentDescription="$title. $detail"','ValueAnimator.areAnimatorsEnabled()','IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS','setOnClickListener{action()}','UI_MODE_NIGHT_YES']:
   self.assertIn(token,CARD)
  self.assertIn('return@rowLoop',MAIN);self.assertIn('Find a library option',MAIN)
 def test_audio_preferences_preserved(self):
  s=(SRC/'ui/SettingsDialog.kt').read_text()
  for token in ['Prefs.audioQuality','Prefs.audioLang','Prefs.videoHeight','Prefs.skipSilence = checked','Prefs.normalize = checked','PlayerHub.applyAll()','PlayerHub.applyNormalize()','SleepTimer.startMinutes','SleepTimer.startEndOfSong','SleepTimer.cancel()','toggle.trackTintList=tint']:
   self.assertIn(token,s)
 def test_playback_guard_and_dock(self):
  self.assertIn('if (!inputBuffer.hasRemaining()) return',(SRC/'player/EffectsProcessor.kt').read_text())
  dock=(SRC/'ui/LiquidGlassSurface.kt').read_text()
  for token in ['content.draw(c)','0xffc4c4c4.toInt()','0xffa0a0a0.toInt()']: self.assertIn(token,dock)
if __name__=='__main__':unittest.main()
