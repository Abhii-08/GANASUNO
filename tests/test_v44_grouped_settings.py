"""Source contracts for grouped Settings. Does not replace Android UI testing."""
import re, unittest, hashlib
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
S=(ROOT/'app/src/main/java/com/abhi/ganasuno/MainActivity.kt').read_text()
G=(ROOT/'app/src/main/java/com/abhi/ganasuno/ui/GlassFeatureCard.kt').read_text()
CAT=S.split('private fun settingsRows():List<ToolRow>{',1)[1].split('// ------------------------------------------------------------------ search',1)[0]
SECTIONS=S.split('private val settingsSections = listOf(',1)[1].split('private fun openSettings(){',1)[0]
class GroupedSettings(unittest.TestCase):
    def test_seven_groups_cover_all_settings_once(self):
        entries=re.findall(r'SettingsSection\("([^"]+)","[^"]+",setOf\(([^\n]+)\)\)',SECTIONS)
        self.assertEqual([name for name,_ in entries],['Appearance & player','Playback','Sound & equalizer','Discovery & history','Storage','Integrations','System & privacy'])
        groups=[g for _,source in entries for g in re.findall(r'"([^"]+)"',source)]
        rows=re.findall(r'ToolRow\("([^"]+)","([^"]+)"',CAT)
        self.assertEqual(len(rows),53)
        self.assertEqual(len(set(rows)),53)
        self.assertEqual(len(groups),len(set(groups)))
        self.assertEqual(set(groups),{group for group,_ in rows})
        self.assertEqual([sum(group in re.findall(r'"([^"]+)"',src) for group,_ in rows) for _,src in entries],[10,9,6,7,4,6,11])
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
        for name in ['appearance & player','playback','sound & equalizer','discovery & history','storage','integrations','system & privacy']:
            self.assertIn('group=="Settings" && t=="'+name+'"',G)
    def test_original_actions_unchanged(self):
        rows='\n'.join(line.strip() for line in CAT.splitlines() if line.strip().startswith('ToolRow('))
        self.assertEqual(hashlib.sha256(rows.encode()).hexdigest(),'309271cff98dffabe0bbf40eab90dadb82d2d727f60df5a0303d9be51da1a833')
if __name__=='__main__': unittest.main()
