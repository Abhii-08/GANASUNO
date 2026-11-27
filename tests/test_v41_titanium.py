"""Static checks of the titanium action theme and settings control wiring."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'app/src/main/res'
SRC=ROOT/'app/src/main/java/com/abhi/ganasuno'
A='{http://schemas.android.com/apk/res/android}'
class TitaniumGrey(unittest.TestCase):
 def test_shared_accent_in_both_modes(self):
  for mode in ('values','values-night'):
   colors={e.attrib['name']:e.text for e in ET.parse(RES/mode/'colors.xml').getroot()}
   self.assertEqual(colors['brand_primary'],'#8E8E93')
   self.assertEqual(colors['brand_on_primary'],'#161616')
   palettes={e.attrib['name']:e.text for e in ET.parse(RES/mode/'palettes.xml').getroot()}
   self.assertEqual(palettes['palette_red'],'#8E8E93')
 def test_material_action_theme(self):
  theme=ET.parse(RES/'values/themes.xml').getroot().find("style[@name='Theme.Ganasuno']")
  values={i.attrib['name']:i.text for i in theme}
  self.assertEqual(values['colorPrimary'],'@color/brand_primary')
  self.assertEqual(values['colorOnPrimary'],'@color/brand_on_primary')
  self.assertEqual(values['colorAccent'],'?attr/colorPrimary')
  self.assertNotIn('colorError',values)
  source=(SRC/'ui/PremiumUi.kt').read_text()
  self.assertIn('setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.brand_primary))',source)
  self.assertNotIn('0xfffa4055',source)
 def test_settings_icon_and_route(self):
  root=ET.parse(RES/'layout/activity_main.xml').getroot()
  btn=next(x for x in root.iter() if x.get(A+'id')=='@+id/btnSettings')
  self.assertEqual(btn.get(A+'src'),'@drawable/ic_settings_menu')
  self.assertEqual(btn.get(A+'background'),'@drawable/glass_settings_field')
  self.assertEqual(btn.get(A+'layout_width'),'48dp')
  self.assertEqual(btn.get(A+'layout_height'),'48dp')
  main=(SRC/'MainActivity.kt').read_text()
  self.assertIn('b.btnSettings.setOnClickListener { openSettings() }',main)
  self.assertIn('b.btnSettings.contentDescription=getString(R.string.settings)',main)
  self.assertIn('"Titanium Grey"',main)
 def test_glass_drawables(self):
  for mode in ('drawable','drawable-night'):
   surface=ET.parse(RES/mode/'glass_settings_field.xml').getroot()
   self.assertEqual(surface.tag,'ripple')
   layers=surface.findall('item/layer-list/item/shape')
   self.assertEqual(len(layers),2)
   self.assertEqual(layers[0].find('corners').get(A+'radius'),'17dp')
   self.assertIsNotNone(layers[0].find('stroke'))
   self.assertTrue(all(s.find('gradient') is not None for s in layers))
  icon=ET.parse(RES/'drawable/ic_settings_menu.xml').getroot()
  self.assertEqual(icon.tag,'vector')
  self.assertEqual(icon.find('path').get(A+'strokeLineCap'),'round')
  self.assertEqual(icon.find('path').get(A+'pathData').count('M'),3)
 def test_version(self):
  s=(ROOT/'app/build.gradle.kts').read_text()
  self.assertIn('versionCode = 111',s)
  self.assertIn('versionName = "45.0"',s)
if __name__=='__main__':unittest.main()
