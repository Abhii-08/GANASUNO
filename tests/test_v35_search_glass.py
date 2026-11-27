"""Static layout, resource and routing contracts. Not Android runtime tests."""
from pathlib import Path
import re, unittest, xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
MAIN=ROOT/'app/src/main'; SRC=MAIN/'java/com/abhi/ganasuno'; RES=MAIN/'res'; A='{http://schemas.android.com/apk/res/android}'
class SearchGlass(unittest.TestCase):
 def test_categories(self):
  source=(SRC/'ui/SearchBrowseView.kt').read_text()
  rows=re.findall(r'Genre\("([^"]+)", "([^"]+)", (0x[0-9a-f]+)\.toInt\(\), R.drawable.(genre_\w+)\)',source)
  expected=['6414ff','a923b0','2c2c2e','006b59','007d57','b00000','9c4b15','3549a8','795315','51408c','9a305d','42525e','a14a25','22478b','76502e','95245b','295965','7343a4','2456a0','00784f']
  self.assertEqual([c[4:] for _,_,c,_ in rows],expected)
  self.assertEqual(len({a for *_,a in rows}),20)
  for name,query,color,art in rows:
   self.assertTrue(query)
   vector=ET.parse(RES/'drawable'/f'{art}.xml').getroot()
   self.assertEqual(vector.tag,'vector');self.assertGreaterEqual(len(vector.findall('path')),5)
  self.assertIn('search(genre.query)',source);self.assertIn('genres.chunked(2)',source)
  self.assertIn('GenreGlassCard(context, genre.name, genre.color, genre.art)',source)
 def test_card_layers_and_accessibility(self):
  s=(SRC/'ui/GenreGlassCard.kt').read_text()
  for token in ['LayerDrawable','setStroke','RippleDrawable','Color.WHITE','Gravity.BOTTOM','maxLines = 2','IMPORTANT_FOR_ACCESSIBILITY_NO','onSizeChanged','setImageResource(artwork)']:
   self.assertIn(token,s)
 def test_search_row(self):
  root=ET.parse(RES/'layout/activity_main.xml').getroot()
  row=next(v for v in root.iter() if v.get(A+'id')=='@+id/searchBox')
  self.assertEqual(row.get(A+'orientation'),'horizontal')
  self.assertEqual([v.get(A+'id') for v in row],['@+id/btnSearchGo','@+id/search','@+id/btnSearchClear'])
  self.assertEqual(row[1].get(A+'layout_weight'),'1')
  for folder in ['drawable','drawable-night']:
   for name,radius in [('search_field','28dp'),('glass_profile_field','32dp')]:
    shape=ET.parse(RES/folder/f'{name}.xml').getroot().find('item/shape')
    self.assertEqual(shape.find('corners').get(A+'radius'),radius)
    self.assertIsNotNone(shape.find('gradient'));self.assertIsNotNone(shape.find('stroke'))
 def test_dock(self):
  source=(SRC/'ui/LiquidGlassSurface.kt').read_text()
  self.assertIn('0xffc4c4c4.toInt()',source);self.assertIn('0xffa0a0a0.toInt()',source)
  self.assertIn('content.draw(c)',source)
 def test_playback_guard_preserved(self):
  self.assertIn('if (!inputBuffer.hasRemaining()) return',(SRC/'player/EffectsProcessor.kt').read_text())
if __name__=='__main__':unittest.main()
