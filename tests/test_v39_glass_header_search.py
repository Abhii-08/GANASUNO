from pathlib import Path
import unittest, re, xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'app/src/main/java/com/abhi/ganasuno'
MAIN=(SRC/'MainActivity.kt').read_text()
FLIP=(SRC/'ui/AnimatedPlaylistGreeting.kt').read_text()
CARD=(SRC/'ui/GlassFeatureCard.kt').read_text()
class GlassHeaderSearch(unittest.TestCase):
 def test_header_uses_same_glass_and_rotates(self):
  self.assertIn('card.background = GlassFeatureStyle.surface(context, 24)',FLIP)
  self.assertIn('card.foreground = GlassFeatureStyle.ripple(context, 24)',FLIP)
  self.assertIn('card.rotationY = PlaylistGreeting.angle(progress)',FLIP)
 def test_header_restores_original_style(self):
  self.assertIn('card.background = originalBackground',FLIP)
  self.assertIn('card.foreground = originalForeground',FLIP)
  self.assertIn('card.setPaddingRelative(originalPaddingStart, originalPaddingTop, originalPaddingEnd, originalPaddingBottom)',FLIP)
 def test_search_uses_library_cards(self):
  self.assertIn('val glassRows=page=="Library"||page=="Settings"||page=="Search"',MAIN)
  self.assertIn('GlassFeatureCard(this,row.group,row.title,row.detail,reduced,row.action)',MAIN)
  search=MAIN.split('private fun openSearch(){',1)[1].split('private fun openLibrary(){',1)[0]
  self.assertIn('browse.render(LibraryStore.frequentSearches()',search)
  labels=re.findall(r'ToolRow\("([^"]+)","([^"]+)"',search)
  self.assertEqual(len(labels),6)
  for group,title in labels:
   self.assertIn(f'group=="{group}" && t=="{title.lower()}"',CARD)
 def test_new_vectors(self):
  for n in ['video','radio']:
   self.assertEqual(ET.parse(ROOT/f'app/src/main/res/drawable/glass_feature_{n}.xml').getroot().tag,'vector')
 def test_version(self):
  s=(ROOT/'app/build.gradle.kts').read_text()
  self.assertIn('versionCode = 111',s);self.assertIn('versionName = "45.0"',s)
if __name__=='__main__': unittest.main()
