import sys, tempfile, unittest, json
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'desktop'))
from native_queue import Queue
class ResumeTests(unittest.TestCase):
    def test_roundtrip(self):
        with tempfile.TemporaryDirectory() as t:
            root=Path(t); a=root/'a.wav'; b=root/'b.wav'; a.touch(); b.touch()
            q=Queue(); q.add([a,b]); q.select(1); q.position=12345; q.volume=33; q.fade_seconds=7; q.keep_albums=False; p=root/'session.json'; q.save(p)
            r=Queue(); r.restore(p); self.assertEqual((r.index,r.position,r.volume,r.fade_seconds,r.keep_albums),(1,12345,33,7,False)); r.select(0); self.assertEqual(r.position,0)
            b.unlink(); r.restore(p); self.assertEqual(r.position,0)
    def test_old_and_invalid_values(self):
        with tempfile.TemporaryDirectory() as t:
            root=Path(t); a=root/'a.wav';a.touch();p=root/'session.json';p.write_text(json.dumps({'version':1,'files':[str(a)],'selected':str(a),'position':True,'volume':999,'fade_seconds':-1,'keep_albums':'false'}));q=Queue();q.restore(p);self.assertEqual((q.position,q.volume,q.fade_seconds,q.keep_albums),(0,100,0,True))
