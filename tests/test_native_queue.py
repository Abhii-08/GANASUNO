import sys
import tempfile
import unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'desktop'))
from native_queue import Queue


class NativeQueueTests(unittest.TestCase):
    def test_queue(self):
        with tempfile.TemporaryDirectory() as root:
            a, b, c = [Path(root) / (name + '.wav') for name in 'abc']
            for f in (a, b, c):
                f.touch()
            q = Queue()
            self.assertIsNone(q.step(1))
            q.add([a, b, c, a, Path(root) / 'missing'])
            self.assertEqual(q.files, [a, b, c])
            self.assertEqual(q.index, 0)
            self.assertEqual(q.step(1), b)
            self.assertFalse(q.remove(0))
            self.assertEqual(q.index, 0)
            self.assertTrue(q.remove(0))
            self.assertEqual(q.select(0), c)
            self.assertIsNone(q.step(1))
            self.assertTrue(q.remove(0))
            self.assertEqual(q.index, -1)


if __name__ == '__main__':
    unittest.main()
