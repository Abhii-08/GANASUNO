import sys, unittest
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'desktop'))
from launcher import WindowControls

class FakeWindow:
    def __init__(self):self.size=None;self.destroyed=False
    def resize(self,w,h):self.size=(w,h)
    def destroy(self):self.destroyed=True

class TestWindow(unittest.TestCase):
    def test_unattached(self):
        c=WindowControls();self.assertFalse(c.mini(True));self.assertFalse(c.quit())
    def test_resize(self):
        c=WindowControls();w=FakeWindow();c._attach(w)
        self.assertTrue(c.mini(True));self.assertEqual(w.size,(480,450))
        self.assertTrue(c.mini(False));self.assertEqual(w.size,(1120,800))
        self.assertFalse(c.mini('true'));self.assertEqual(w.size,(1120,800))
    def test_quit(self):
        c=WindowControls();w=FakeWindow();c._attach(w);self.assertTrue(c.quit());self.assertTrue(w.destroyed)
