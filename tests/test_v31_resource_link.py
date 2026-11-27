"""Local resource-reference checks; not a substitute for AAPT2 linking."""
import re
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res'
ANDROID = '{http://schemas.android.com/apk/res/android}'
class ResourceChecks(unittest.TestCase):
    def test_search_backgrounds(self):
        for folder in ['drawable','drawable-night']:
            root = ET.parse(RES/folder/'search_field.xml').getroot()
            self.assertEqual(root.tag,'ripple')
            shape = root.find('item/shape')
            self.assertEqual(shape.find('corners').get(ANDROID+'radius'),'28dp')
            self.assertIsNotNone(shape.find('gradient'))
            self.assertIsNotNone(shape.find('stroke'))
    def test_local_xml_refs(self):
        defined=set()
        xmls=list(RES.rglob('*.xml'))+[RES.parent/'AndroidManifest.xml']
        for p in RES.rglob('*'):
            if not p.is_file():continue
            kind=p.parent.name.split('-')[0]
            if kind=='values':
                for e in ET.parse(p).getroot():
                    if e.get('name'):defined.add((e.get('type',e.tag),e.get('name')))
            else:defined.add((kind,p.name.split('.')[0]))
        for p in xmls:
            ET.parse(p)
            defined.update(('id',n) for n in re.findall(r'@\+id/([\w.]+)',p.read_text()))
        external={('style','Widget.Material3.Button.TextButton'),('style','Widget.Material3.Button.TonalButton')}
        for p in xmls:
            for k,n in re.findall(r'(?<![\w:])@(color|drawable|mipmap|string|layout|xml|id|dimen|style)/([\w.]+)',p.read_text()):
                self.assertIn((k,n),defined|external,str(p))
if __name__=='__main__':unittest.main()
