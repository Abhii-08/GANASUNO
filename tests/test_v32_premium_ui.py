"""Source contracts only, not a substitute for compilation or device interaction."""
import re, unittest, xml.etree.ElementTree as ET
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MAIN=ROOT/'app/src/main'
SRC=MAIN/'java/com/abhi/ganasuno'
S=(SRC/'MainActivity.kt').read_text()
U=(SRC/'ui/PremiumUi.kt').read_text()
class Premium(unittest.TestCase):
    def test_search(self):
        self.assertIn('ui.postDelayed(job,850)',S)
        self.assertIn('++requestSerial',S)
        self.assertIn('b.btnSearchClear.setOnClickListener',S)
        self.assertIn('b.suggestRow.addView',S)
    def test_layout_bindings(self):
        ids=set(re.findall(r'@\+id/(\w+)',(MAIN/'res/layout/activity_main.xml').read_text()))
        for name in ('btnSearchClear','suggestRow','suggestScroll','playerDock','miniThumb'):
            self.assertIn(name,ids);self.assertIn('b.'+name,S)
    def test_artwork_and_dock(self):
        self.assertIn('bitmap.getPixel',U)
        self.assertIn('dockArtwork==url',S)
        self.assertIn('ValueAnimator.areAnimatorsEnabled()',U)
        self.assertIn('onLoaded?.invoke(bitmap)',(SRC/'ui/ImageLoader.kt').read_text())
    def test_home_data(self):
        for route in ('LibraryStore.history()','Batch11.quick','Batch11.mix','LibraryStore.mostPlayed()','YtApi.search'):
            self.assertIn(route,S.split('private fun home()')[1].split('private fun offlineHome')[0])
    def test_library_and_details(self):
        self.assertIn('GridLayoutManager(this,2)',S)
        self.assertIn('getItemViewType', (SRC/'ui/SongAdapter.kt').read_text())
        self.assertIn('collectionDetails(name,tracks',S)
        self.assertIn('collectionDetails(g.title,g.tracks',S)
        self.assertIn('collectionDetails(artist,tracks',S)
    def test_playback_routes(self):
        player=S.split('private fun playerMore(')[1].split('private fun lyricsDialog(')[0]
        for route in ('skipTrack(false)','skipTrack(true)','shuffleModeEnabled','repeatMode','artworkQueue()','lyricsDialog(','outputPicker()','TenBandDialog.show(this)'):
            self.assertIn(route,player)
    def test_theme(self):
        colors={x.get('name'):x.text for x in ET.parse(MAIN/'res/values-night/colors.xml').getroot()}
        self.assertEqual(colors['browse_surface'],'#000000')
        self.assertEqual(colors['brand_primary'],'#8E8E93')
        self.assertEqual(colors['browse_field'],'#252525')
if __name__=='__main__':unittest.main()
