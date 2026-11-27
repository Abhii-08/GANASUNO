"""Static UI wiring assertions, not device tests."""
from pathlib import Path
import unittest, xml.etree.ElementTree as ET, re
ROOT=Path(__file__).resolve().parents[1]
M=ROOT/'app/src/main'
A='{http://schemas.android.com/apk/res/android}'
S=(M/'java/com/abhi/ganasuno/MainActivity.kt').read_text()
B=(M/'java/com/abhi/ganasuno/ui/SearchBrowseView.kt').read_text()
L=(M/'java/com/abhi/ganasuno/data/LibraryStore.kt').read_text()
class Browse(unittest.TestCase):
    def test_layout_order(self):
        root=ET.parse(M/'res/layout/activity_main.xml').getroot()
        self.assertTrue(any(x.get(A+'id')=='@+id/sectionTitle' for x in root[0][0].iter()))
        self.assertEqual(root[0][1].get(A+'id'),'@+id/searchBox')
    def test_navigation(self):
        root=ET.parse(M/'res/layout/activity_main.xml').getroot()
        nav=next(v for v in root.iter() if v.get(A+'id')=='@+id/bottomNav')
        self.assertEqual([v.get(A+'text') for v in nav if v.get(A+'visibility')!='gone'],['Listen Now','Search','Library'])
        self.assertIn('b.btnSettings.setOnClickListener { openSettings() }',S)
        self.assertIn('b.miniTitle.setOnClickListener { nowPlaying(true) }',S)
        self.assertIn('5->openVideo()',S)
    def test_cards(self):
        self.assertEqual(len(re.findall(r'Genre\("',B)),20)
        self.assertIn('genres.chunked(2)',B);self.assertIn('search(genre.query)',B)
        self.assertIn('search(query)',B);self.assertIn('ImageLoader.load(song?.thumb.orEmpty()',B)
        self.assertIn('Your frequent searches will appear here',B)
    def test_history(self):
        self.assertIn('LibraryStore.frequentSearches()',S)
        self.assertIn('sortedByDescending',L);self.assertIn('o.remove("search_counts_v31")',L)
        self.assertIn('take(50)',L)
    def test_dock(self):
        root=ET.parse(M/'res/layout/activity_main.xml').getroot()
        dock=next(v for v in root.iter() if v.get(A+'id')=='@+id/playerDock')
        self.assertEqual(dock.get(A+'layout_gravity'),'bottom|center_horizontal')
        self.assertEqual([v.get(A+'id') for v in dock],['@+id/mini','@+id/bottomNav'])
        for id in ['miniPrev','miniPlay','miniNext','seek']:
            self.assertTrue(any(v.get(A+'id')=='@+id/'+id for v in dock.iter()))
if __name__=='__main__':unittest.main()
