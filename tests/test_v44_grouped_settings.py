"""Source contracts for grouped Settings. Does not replace Android UI testing."""
import re, unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
S=(ROOT/'app/src/main/java/com/abhi/ganasuno/MainActivity.kt').read_text(encoding="utf-8")
G=(ROOT/'app/src/main/java/com/abhi/ganasuno/ui/GlassFeatureCard.kt').read_text(encoding="utf-8")
CAT=S.split('private fun settingsRows():List<ToolRow>{',1)[1].split('// ------------------------------------------------------------------ search',1)[0]
SECTIONS=S.split('private val settingsSections = listOf(',1)[1].split('private fun openSettings(){',1)[0]

# Expected Settings routes for the current release. Descriptions can change independently.
EXPECTED_ROUTES = """\
Appearance|Liquid Glass effects|choose("Liquid Glass",listOf("Full effects","Reduced effects")){n->LibraryStore.option("glass_reduced",n,true);glassDock.configure(n==1);toast("Glass settings saved")}
Playback|Audio & video quality|SettingsDialog.show(this)
Playback|Playback controls|playbackControls()
Playback|Crossfade & DJ transitions|transitionMenu()
Playback|Manual fade|fadeMenu()
Sound|Audio output|outputPicker()
Playback|Explicit content|v8MenuAction(1)
Playback|320 kbps preference|v8MenuAction(0)
Playback|Video playback mode|batch14MenuAction(0)
Sound|Equalizer|com.abhi.ganasuno.ui.EqualizerDialog.show(this)
Sound|10-band software EQ|com.abhi.ganasuno.ui.TenBandDialog.show(this)
Sound|Stereo rotation & delay|dspMenu()
Sound|Reverb and preamp|advanced.menuAction(0)
Sound|System equalizer|toolsMenuAction(9)
Sound|Track waveform and music tags|moreFeaturesAction(10)
Player & appearance|Player style|advanced.menuAction(1)
Player & appearance|Now playing|if(controller?.currentMediaItem==null)toast("Play a song first")else nowPlaying(true)
Player & appearance|Artwork queue|artworkQueue()
Player & appearance|Animated artwork|v8MenuAction(3)
Player & appearance|Download animated artwork|batch22MenuAction(4)
Player & appearance|Theme|toolsMenuAction(3)
Player & appearance|Theme colours|moreFeaturesAction(0)
Player & appearance|Language and RTL|v8MenuAction(8)
Lyrics|Current song lyrics|controller?.currentMediaItem?.let{lyricsDialog(fromMediaItem(it))}?:toast("Play a song first")
Discovery|Curated playlists|v8MenuAction(4)
Discovery|Related artists|v8MenuAction(5)
Discovery|Country & home feed|toolsMenuAction(8)
Discovery|Current track metadata|moreFeaturesAction(16)
Discovery|Curator channels|batch22MenuAction(2)
Discovery|Alternative song versions|batch22MenuAction(0)
Storage|Playback cache|mediaCacheMenu()
Storage|Thumbnail & artwork cache|thumbnailStorage()
Storage|Downloaded animated artwork cache|batch22MenuAction(5)
Storage|Waveform cache|batch15MenuAction(4)
Listening insights|Search history|searchHistory()
Integrations|Listen Together|rooms.open()
Integrations|Community & charts|moreFeaturesAction(18)
Integrations|YouTube account & blog|moreFeaturesAction(19)
Integrations|Artist subscriptions|batch22MenuAction(3)
Integrations|AI playlist suggestions|advanced.menuAction(5)
Integrations|AI listening recap|advanced.menuAction(6)
System & privacy|Stop service on exit|advanced.menuAction(8)
System & privacy|System sound settings|advanced.menuAction(9)
System & privacy|Diagnostics|diagnostics()
Playback|Playback troubleshooting|showPlaybackHelp()
System & privacy|Developer diagnostics|moreFeaturesAction(5)
System & privacy|Update checks|advanced.updates()
System & privacy|Current playback format|currentFormat()
System & privacy|Combined song information|batch22MenuAction(1)
System & privacy|Look up release year|batch14MenuAction(2)
System & privacy|About & privacy|toolsMenuAction(7)
System & privacy|Release notes and support|releaseNotes()
System & privacy|Developer information|advanced.menuAction(7)
"""

class GroupedSettings(unittest.TestCase):
    def test_seven_groups_cover_all_settings_once(self):
        entries=re.findall(r'SettingsSection\("([^"]+)","[^"]+",setOf\(([^\n]+)\)\)',SECTIONS)
        self.assertEqual([name for name,_ in entries],['Appearance','Playback','Sound & Effects','Discovery','Storage','Account & Integrations','System & Privacy'])
        groups=[g for _,source in entries for g in re.findall(r'"([^"]+)"',source)]
        rows=re.findall(r'ToolRow\("([^"]+)","([^"]+)"',CAT)
        self.assertEqual(len(rows),len(set(rows)))
        self.assertEqual(len(groups),len(set(groups)))
        self.assertEqual(set(groups),{group for group,_ in rows})
        for _, source in entries:
            section_groups = set(re.findall(r'"([^"]+)"', source))
            self.assertTrue(any(group in section_groups for group, _ in rows))
    def test_root_cards_search_and_group(self):
        root=S.split('private fun openSettings(){',1)[1].split('private fun openSettingsGroup(',1)[0]
        self.assertIn('ToolRow("Settings",section.title,section.detail){openSettingsGroup(section.title)}',root)
        self.assertIn('renderRows(rows,true,showHeadings=false,searchRows=all)',root)
        self.assertIn('val candidates=if(terms.isEmpty())rows else searchRows',S)
        sub=S.split('private fun openSettingsGroup(',1)[1].split('private fun settingsRows()',1)[0]
        self.assertIn('settingsRows().filter{it.group in section.groups}',sub)
        self.assertIn('renderRows(rows,true,showHeadings=false)',sub)
        self.assertIn('setOnClickListener{openSettings()}',sub)
    def test_back_and_state(self):
        for needle in ['page=="Settings"&&settingsGroup!=null->openSettings()', 'if(next!="Settings")settingsGroup=null', 'outState.putString("settings_group",settingsGroup)', 'savedInstanceState?.getString("settings_group")']:
            self.assertIn(needle,S)
    def test_icons(self):
        for name in ['appearance','playback','sound & effects','discovery','storage','account & integrations','system & privacy']:
            self.assertIn('group=="Settings" && t=="'+name+'"',G)
    def test_original_actions_unchanged(self):
        actual = re.findall(r'^\s*ToolRow\("([^"]+)","([^"]+)","[^"]*"\)\{(.*)\}[,)]?$', CAT, re.M)
        expected = [tuple(row.split("|", 2)) for row in EXPECTED_ROUTES.splitlines()]
        self.assertCountEqual(actual, expected)
if __name__=='__main__': unittest.main()
