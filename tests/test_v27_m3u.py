import sys, tempfile, unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'desktop'))
from native_queue import Queue
class M3uTest(unittest.TestCase):
    def test_roundtrip(self):
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp); a=p/'one.wav'; b=p/'two.mp3'; a.touch(); b.touch()
            file=p/'list.m3u8'; file.write_text('#EXTM3U\none.wav\nhttps://example.org/a.mp3\nmissing.mp3\ntwo.mp3\none.wav\n',encoding='utf-8')
            q=Queue(); self.assertEqual(q.import_m3u(file),2); self.assertEqual(q.files,[a,b]); self.assertEqual(q.import_m3u(file),0)
            out=p/'out.m3u8'; q.export_m3u(out); other=Queue(); self.assertEqual(other.import_m3u(out),2); self.assertEqual(other.files,q.files)
    def test_large(self):
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp)/'big.m3u'; p.write_bytes(b'x'*1000001)
            with self.assertRaises(ValueError): Queue().import_m3u(p)
    def test_reject_output(self):
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp)/'sound.wav'; p.touch(); q=Queue(); q.add([p]);
            with self.assertRaises(ValueError): q.export_m3u(p)
if __name__=='__main__': unittest.main()
