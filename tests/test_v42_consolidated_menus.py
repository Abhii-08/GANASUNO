"""Regression contracts for consolidated routes; not device instrumentation."""
from pathlib import Path
import re, unittest
S=(Path(__file__).resolve().parents[1]/'app/src/main/java/com/abhi/ganasuno/MainActivity.kt').read_text()
def body(name, next_name):
    return S.split('private fun '+name+'(',1)[1].split('private fun '+next_name+'(',1)[0]
def titles(text):
    return re.findall(r'ToolRow\((?:"[^"]+"|group),"([^"]+)"',text)
class ConsolidatedMenus(unittest.TestCase):
    def test_library_single_entry_per_hub(self):
        lib=body('openLibrary','renderRows')
        top=lib.split('private fun openLibraryGroup',1)[0]
        self.assertEqual(titles(top),['Collection','Playlists','Listening','Storage'])
        for name in ['Offline Audio','Artists and Albums','Backup and restore','Manage library','Create or manage playlists']:
            self.assertEqual(titles(lib).count(name),1)
    def test_offline_actions(self):
        off=body('offlineMenu','animatedStorage')
        for token in ['0->showLocalList(LibraryStore.list("local")','1->importAudio.launch','2->batch22MenuAction(6)','3->moreFeaturesAction(20)','4->MaterialAlertDialogBuilder','5->offlineAudit()']:
            self.assertIn(token,off)
    def test_management_actions(self):
        for token in ['LibraryStore.option("ui_grid",it,true)','LibraryStore.ordered(songs,n,scope)','2->bulkMenu()']:
            self.assertIn(token,body('libraryMenu','playlistHub'))
        for token in ['0->playlistsMenu()','1->advanced.menuAction(3)','2->advanced.menuAction(4)','3->sharedPlaylists().show()','4->v8MenuAction(6)','5->playlistLinkMenu()']:
            self.assertIn(token,body('playlistHub','backupHub'))
        for token in ['0->exportLibrary.launch','1->restoreLibrary.launch','2->backupMenu()','3->moreFeaturesAction(3)','4->moreFeaturesAction(4)','5->moreFeaturesAction(1)','6->moreFeaturesAction(2)']:
            self.assertIn(token,body('backupHub','playlistsMenu'))
    def test_history(self):
        h=body('historyMenu','wrapped')
        for token in ['0->showLocalList','10->mostPlayedMenu()','11->wrapped()','12->showLocalList(LibraryStore.mostPlayed()','13->decadeMenu()','intArrayOf(7,30,90,0)[i-2]','Clear listening history']:
            self.assertIn(token,h)
    def test_search_and_settings(self):
        search=body('openSearch','openLibrary')
        self.assertEqual(len(titles(search)),6)
        self.assertNotIn('Songs',titles(search))
        self.assertIn('doSearch(b.search.text.toString())',S)
        settings=S.split('private fun openSettings(){')[1].split('// ------------------------------------------------------------------ search')[0]
        self.assertEqual(len(titles(settings)),53)
        for action in ['offlineMenu()','backupMenu()','historyMenu()','libraryMenu()','discoverMenu()','publicCatalogue.show()','advanced.artists()']:
            self.assertNotIn(action,settings)
        self.assertIn('advanced.menuAction(7)',settings)
        self.assertNotIn('toolsMenu()',settings)
    def test_library_contextual_more(self):
        self.assertIn('b.btnSectionMore.visibility=if(next=="Home")View.VISIBLE else View.GONE',S)
        self.assertIn('if(page=="Library")b.btnSectionMore.visibility=View.VISIBLE',body('showSongContent','showToolsContent'))
if __name__=='__main__':unittest.main()
