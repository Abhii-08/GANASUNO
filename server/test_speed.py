import unittest
from rooms import Rooms, RoomError

class TestSpeed(unittest.TestCase):
    def setUp(self):
        self.now=1000
        self.rooms=Rooms(lambda:self.now)
        self.host=self.rooms.call('create',{'name':'Host'})
    def publish(self,**extra):
        return self.rooms.call('publish',dict(self.host,video='abcdefghijk',position=2000,playing=True,**extra))
    def state(self):
        return self.rooms.call('state',self.host)['track']
    def test_double_speed(self):
        self.publish(speed=2);self.now+=3
        self.assertEqual(self.state()['position'],8000)
        self.assertEqual(self.state()['speed'],2)
    def test_half_speed(self):
        self.publish(speed=.5);self.now+=3
        self.assertEqual(self.state()['position'],3500)
    def test_legacy_speed(self):
        self.publish();self.now+=3
        self.assertEqual(self.state()['position'],5000)
    def test_invalid(self):
        for speed in (True,0,2.1,float('nan'),float('inf'),'2',10**400):
            with self.subTest(speed=speed),self.assertRaises(RoomError):self.publish(speed=speed)
    def test_stale(self):
        self.publish(speed=2);self.now+=20
        self.assertFalse(self.state()['playing']);self.assertEqual(self.state()['position'],32000)
    def test_pause(self):
        self.rooms.call('publish',dict(self.host,video='abcdefghijk',position=2000,playing=False,speed=2));self.now+=3
        self.assertEqual(self.state()['position'],2000)
