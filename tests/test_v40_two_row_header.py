from pathlib import Path
import unittest
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'app/src/main/java/com/abhi/ganasuno'
FLIP=(SRC/'ui/AnimatedPlaylistGreeting.kt').read_text()
MAIN=(SRC/'MainActivity.kt').read_text()
A='{http://schemas.android.com/apk/res/android}'
class TwoRowHeader(unittest.TestCase):
 def test_shared_container(self):
  xml=ET.parse(ROOT/'app/src/main/res/layout/activity_main.xml')
  card=next(x for x in xml.iter() if x.get(A+'id')=='@+id/headerFlipCard')
  self.assertEqual(card.tag,'LinearLayout');self.assertEqual(card.get(A+'orientation'),'vertical')
  self.assertEqual([x.get(A+'id') for x in card],['@+id/sectionTitle','@+id/sectionSubtitle'])
  self.assertEqual(card.get(A+'layout_weight'),'1')
  self.assertIn('AnimatedPlaylistGreeting(b.headerFlipCard, b.sectionTitle, b.sectionSubtitle)',MAIN)
 def test_card_rotation_not_text_rotation(self):
  self.assertIn('card.rotationY = PlaylistGreeting.angle(progress)',FLIP)
  self.assertNotIn('title.rotationY',FLIP)
  self.assertIn('card.rotationY = 0f',FLIP)
  self.assertIn('card.setOnClickListener { flip(true) }',FLIP)
 def test_text_and_adaptive_size(self):
  for token in ['card.minimumHeight = GlassFeatureStyle.dp(context, 94)','title.setAutoSizeTextTypeUniformWithConfiguration(16, 28, 1, TypedValue.COMPLEX_UNIT_SP)','subtitle.text = subtitleLabel','Listen Now · GANASUNO','subtitle.maxLines = Int.MAX_VALUE','subtitle.setTextColor(GlassFeatureStyle.secondary(context))']:
   self.assertIn(token,FLIP)
 def test_restore_other_screens(self):
  unbind=FLIP.split('fun unbind()',1)[1]
  for token in ['card.minimumHeight = originalMinimumHeight','card.gravity = originalGravity','card.background = originalBackground','TextView.AUTO_SIZE_TEXT_TYPE_NONE','height = originalTitleHeight','subtitle.setTextColor(originalSubtitleColors)','subtitle.visibility = originalSubtitleVisibility','subtitle.maxLines = originalSubtitleMaxLines','subtitle.importantForAccessibility = originalSubtitleAccessibility']:
   self.assertIn(token,unbind)
 def test_reduced_motion_still_two_rows(self):
  bind=FLIP.split('fun bind(name:',1)[1].split('private var scheduled',1)[0]
  self.assertNotIn('eligible()',bind)
  self.assertIn('!reduced()',FLIP)
if __name__=='__main__':unittest.main()
