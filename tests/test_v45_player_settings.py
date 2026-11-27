from pathlib import Path
import re, unittest
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'app/src/main/java/com/abhi/ganasuno'
M=(SRC/'MainActivity.kt').read_text()
class V45(unittest.TestCase):
 def test_categories_are_complete_and_disjoint(self):
  sections=M.split('private val settingsSections = listOf(',1)[1].split('private fun openSettings(){',1)[0]
  groups=re.findall(r'setOf\(([^)]+)\)',sections)
  self.assertEqual(len(groups),7)
  mapped=[g for group in groups for g in re.findall(r'"([^"]+)"',group)]
  rows=M.split('private fun settingsRows()',1)[1].split('// ------------------------------------------------------------------ search',1)[0]
  self.assertEqual(len(mapped),len(set(mapped)))
  self.assertEqual(set(mapped),set(re.findall(r'ToolRow\("([^"]+)"',rows)))
  self.assertEqual(rows.count('ToolRow('),53)
 def test_root_search_and_back(self):
  for x in ['renderRows(rows,true,showHeadings=false,searchRows=all)','val candidates=if(terms.isEmpty())rows else searchRows','page=="Settings"&&settingsGroup!=null->openSettings()','settingsRows().filter{it.group in section.groups}','setOnClickListener{openSettings()}']:
   self.assertIn(x,M)
 def test_transport_shared(self):
  for x in ['b.miniNext.setOnClickListener { skipTrack(true) }','b.miniPrev.setOnClickListener { skipTrack(false) }','transport(R.drawable.ic_prev,"Previous track"){skipTrack(false)}','transport(R.drawable.ic_next,"Next track"){skipTrack(true)}','if(now-lastSkipAt<350L)return','if(decision.prepare || c.playbackState==Player.STATE_IDLE)c.prepare()','Action.RESTART->{c.seekToDefaultPosition();c.play()}']:
   self.assertIn(x,M)
  self.assertEqual(M.count('c.seekToNextMediaItem()'),1)
  self.assertEqual(M.count('c.seekToPreviousMediaItem()'),1)
 def test_quick_actions_real(self):
  for x in ['LibraryStore.option("rotate",if(LibraryStore.option("rotate")==1)0 else 1,true)','EffectsSettings.refresh()','TenBandDialog.show(this)','SettingsDialog.showSleepTimer(this)','SettingsDialog.sleepLabel()','LibraryStore.option("dsp_eq")==1','syncQuickAudio()']:
   self.assertIn(x,M)
 def test_avoid_duplicate_quick_actions(self):
  more=M.split('private fun playerMore(',1)[1].split('private fun nowPlaying(',1)[0]
  self.assertIn('includeSleepTimer=false',more);self.assertIn('includeRotation=false',more)
  self.assertNotIn('TenBandDialog.show',more)
 def test_pause_icon_and_loading(self):
  p=M.split('private fun nowPlaying(',1)[1].split('private fun lyricsDialog(',1)[0]
  self.assertIn('MiniPlaybackUi.resolve',p);self.assertIn('loading.visibility=if(state.showLoading)',p)
 def test_existing_mix_resume(self):
  s=(SRC/'player/PlaybackService.kt').read_text()
  self.assertIn('val resumeEnded=p.playWhenReady&&p.playbackState==Player.STATE_ENDED',s)
  self.assertIn('if(resumeEnded&&p.playWhenReady)',s)
