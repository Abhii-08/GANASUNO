import sys, tempfile, unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'desktop'))
from native_queue import Queue, fade_allowed, fade_gains
class SessionTests(unittest.TestCase):
    def test_fade(self):
        self.assertFalse(fade_allowed('Album',' album ',True,5));self.assertTrue(fade_allowed('Album',' album ',False,5));self.assertTrue(fade_allowed('','',True,5));self.assertFalse(fade_allowed('A','B',True,0));self.assertFalse(fade_allowed('A','B',True,float('nan')))
    def test_gains(self):
        self.assertAlmostEqual(fade_gains(0)[0],1);self.assertAlmostEqual(fade_gains(1)[1],1);self.assertAlmostEqual(sum(x*x for x in fade_gains(.5)),1);self.assertEqual(fade_gains(float('nan')),(1.,0.))
    def test_round_trip(self):
        with tempfile.TemporaryDirectory() as tmp:
            root=Path(tmp);a=root/'a.wav';b=root/'b.wav';a.touch();b.touch();q=Queue();q.add([a,b,a]);q.set_album(1,'Second');q.select(1);q.save(root/'saved.json');r=Queue();r.restore(root/'saved.json');self.assertEqual(r.files,[a,b]);self.assertEqual(r.index,1);self.assertEqual(r.album(1),'Second');a.unlink();r.restore(root/'saved.json');self.assertEqual(r.files,[b]);self.assertEqual(r.index,0)
    def test_invalid_session(self):
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp)/'saved.json';p.write_text('{}');q=Queue();self.assertRaises(ValueError,q.restore,p)
if __name__=='__main__': unittest.main()
