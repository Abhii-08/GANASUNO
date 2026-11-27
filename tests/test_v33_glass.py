from pathlib import Path
import unittest, xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1];MAIN=ROOT/'app/src/main';SRC=MAIN/'java/com/abhi/ganasuno';S=(SRC/'MainActivity.kt').read_text();G=(SRC/'ui/LiquidGlassSurface.kt').read_text();H=(SRC/'ui/GlassHardwareBlur.kt').read_text();A='{http://schemas.android.com/apk/res/android}'
class Glass(unittest.TestCase):
 def test_overlay(self):
  root=ET.parse(MAIN/'res/layout/activity_main.xml').getroot();self.assertEqual(root.tag,'FrameLayout');self.assertEqual([v.get(A+'id') for v in root],['@+id/glassContent','@+id/playerDock']);self.assertEqual(root[1].get(A+'layout_gravity'),'bottom|center_horizontal');self.assertTrue(all(v.tag.endswith('LiquidGlassSurface') for v in root[1]))
 def test_backdrop_is_not_foreground(self):
  self.assertIn('content.draw(c)',G);self.assertIn('nav.capture(content);mini.capture(content)',G);self.assertIn('node.setRenderEffect',H);self.assertIn('GlassMath.blur',G);self.assertIn('SDK_INT>=31',G);self.assertIn('dock.postDelayed(capture,125)',G)
 def test_neutral_nav(self):
  self.assertIn('0xffc4c4c4.toInt()',G);self.assertIn('0xffa0a0a0.toInt()',G);self.assertIn('compoundDrawableTintList',G);self.assertIn('GlassMath.spring(t)',G);self.assertIn('ValueAnimator.areAnimatorsEnabled()',G)
 def test_playback_routes(self):
  for route in ['b.miniPlay.setOnClickListener','b.miniNext.setOnClickListener','b.miniPrev.setOnClickListener','b.miniTitle.setOnClickListener { nowPlaying(true) }','b.seek.setOnSeekBarChangeListener','glassDock.artwork(bitmap)','b.miniBuffering.visibility']:
   self.assertIn(route,S)
 def test_clearance_and_lifecycle(self):
  for route in ['glassDock.start()','glassDock.stop()','glassDock.clearance()','WindowInsetsCompat.Type.ime()','tabScroll[page]']:
   self.assertIn(route,S)
  self.assertIn('base[i]+height',G);self.assertIn('removeOnScrollChangedListener',G)
 def test_reduced(self):
  self.assertIn('LibraryStore.option("glass_reduced",n,true)',S);self.assertIn('if(reduced || !isShown',G)
if __name__=='__main__':unittest.main()
