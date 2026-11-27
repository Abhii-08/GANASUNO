"""Source wiring regression checks. Does not replace an Android APK/device test."""
import csv, re, unittest, xml.etree.ElementTree as ET
from pathlib import Path
from collections import Counter
ROOT=Path(__file__).resolve().parents[1]
MAIN=ROOT/'app/src/main'
S=(MAIN/'java/com/abhi/ganasuno/MainActivity.kt').read_text()
ANDROID='{http://schemas.android.com/apk/res/android}'
class UiWiring(unittest.TestCase):
    def test_xml(self):
        for file in MAIN.rglob('*.xml'):
            with self.subTest(file=file):ET.parse(file)
    def test_navigation(self):
        nodes=ET.parse(MAIN/'res/layout/activity_main.xml').getroot().iter()
        ids={n.get(ANDROID+'id') for n in nodes}
        for id in ['navHome','navSearch','navLibrary','navPlayer','navSettings','searchBox','chipScroll','sectionTitle','sectionSubtitle','toolsScroll','toolsContent','btnSearchGo','btnSectionMore']:
            self.assertIn('@+id/'+id,ids);self.assertIn('b.'+id,S)
        for id,fn in [('navHome','home'),('navSearch','openSearch'),('navLibrary','openLibrary'),('navSettings','openSettings')]:
            self.assertRegex(S,rf'b\.{id}\.setOnClickListener\{{[^\n]*{fn}\(\)\}}')
        self.assertIn('b.navPlayer.setOnClickListener',S)
    def test_settings_actions(self):
        section=S.split('private fun openSettings(){',1)[1].split('// ------------------------------------------------------------------ search',1)[0]
        rows=re.findall(r'ToolRow\("([^"]+)","([^"]+)"',section)
        self.assertEqual(len(rows),len(set(rows)));self.assertEqual(len(rows),53)
        for fn in ['transitionMenu','dspMenu','mediaCacheMenu','lyricsDialog','rooms.open','advanced.updates','diagnostics','advanced.menuAction']:
            self.assertIn(fn+'(',section)
        self.assertIn('renderRows(rows,true,showHeadings=false,searchRows=all)',section)
        self.assertIn('input.addTextChangedListener',S)
    def test_song_actions(self):
        item=(MAIN/'res/layout/item_song.xml').read_text()
        adapter=(MAIN/'java/com/abhi/ganasuno/ui/SongAdapter.kt').read_text()
        self.assertIn('@+id/songMore',item);self.assertIn('b.songMore.setOnClickListener',adapter);self.assertIn('onLongClick(pos)',adapter)
    def test_catalogue(self):
        with (ROOT/'docs/V30_UI_ROUTE_AUDIT.csv').open(encoding='utf-8-sig') as f: rows=list(csv.DictReader(f))
        self.assertEqual(len(rows),200);self.assertEqual({int(x['ID']) for x in rows},set(range(1,201)))
        status=Counter(x['V29 source status (unchanged)'] for x in rows)
        self.assertEqual(status['Partial'],23);self.assertEqual(status['Not implemented'],32)
        self.assertEqual(sum(status.values())-55,145)
        self.assertTrue(all(x['V30 source route'] for x in rows))
if __name__=='__main__':unittest.main()
