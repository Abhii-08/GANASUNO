"""Source wiring checks; Android interaction must still be tested on a device."""
from pathlib import Path
import re, unittest, xml.etree.ElementTree as ET
R=Path(__file__).resolve().parents[1]
S=(R/'app/src/main/java/com/abhi/ganasuno/MainActivity.kt').read_text()
def section(a,b): return S.split('private fun '+a+'(',1)[1].split('private fun '+b+'(',1)[0]
class GroupedLibraryPlayer(unittest.TestCase):
 def test_four_group_cards(self):
  top=section('openLibrary','openLibraryGroup')
  self.assertEqual(re.findall(r'ToolRow\("Library","([^"]+)"',top),['Collection','Playlists','Listening','Storage'])
  for group in ['Collection','Playlists','Listening','Storage']:
   self.assertIn('openLibraryGroup("'+group+'")',top)
 def test_group_routes_and_back(self):
  g=section('openLibraryGroup','renderRows')
  for route in ['localCatalogue()','favouriteCollections()','podcastMenu()','playlistsMenu()','advanced.menuAction(3)','advanced.menuAction(4)','sharedPlaylists().show()','mostPlayedMenu()','dateInsights()','monthlyRecap()','wrapped()','decadeMenu()','offlineMenu()','backupHub()']:
   self.assertIn(route,g)
  self.assertIn('setOnClickListener{openLibrary()}',g)
  self.assertIn('if(group!=null)openLibraryGroup(group)else openLibrary()',S)
  self.assertIn('page=="Library"&&libraryGroup!=null->openLibrary()',S)
 def test_player_direct_controls(self):
  p=section('nowPlaying','lyricsDialog')
  self.assertIn('button(extra,"Queue"){artworkQueue()}',p)
  self.assertIn('button(extra,"Lyrics")',p)
  self.assertIn('button(tools,"More options"){playerMore(fullscreen)}',p)
  for old in ['setNeutralButton("Controls")','button(videoRow','button(sound','button(analysisRow','button(extra,"♥")']:
   self.assertNotIn(old,p)
 def test_player_advanced_routes(self):
  p=section('playerMore','nowPlaying')
  for route in ['fromPlayer=true','SettingsDialog.show(this,includeSleepTimer=false)','EqualizerDialog.show(this)','dspMenu(includeRotation=false)','outputPicker()','playbackControls(advancedOnly=true)','advanced.menuAction(1)','analyzeTrack(fromMediaItem(it))','openVideo()','nowPlaying(!fullscreen)']:
   self.assertIn(route,p)
  self.assertIn('!advancedOnly||it !in listOf(0,1,4)',section('playbackControls','fadeMenu'))
  self.assertIn('if(fromPlayer)labels.removeAll(listOf("Toggle favourite","Lyrics","Analyze audio and waveform","Play video"))',S)
 def test_heart_state(self):
  p=section('nowPlaying','lyricsDialog')
  for v in ['if(saved)0xfff4c2c2.toInt()','R.drawable.player_heart_filled','R.drawable.player_heart_outline','LibraryStore.list("favorites").any','LibraryStore.toggleFavorite(fromMediaItem(it))','setFavourite(saved)','last=current.id;syncFavourite()','Remove from favourites','Add to favourites']:
   self.assertIn(v,p)
  self.assertNotIn('button(extra,"♥")',p)
  for name in ['outline','filled']:
   x=ET.parse(R/f'app/src/main/res/drawable/player_heart_{name}.xml').getroot()
   self.assertEqual(x.find('path').get('{http://schemas.android.com/apk/res/android}fillColor'),'#00000000' if name=='outline' else '#FFFFFFFF')
if __name__=='__main__': unittest.main()
