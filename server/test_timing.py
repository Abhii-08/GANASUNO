import unittest
from rooms import Rooms, RoomError
class TimingTest(unittest.TestCase):
    def test_sample_age(self):
        now=[100.]; r=Rooms(clock=lambda:now[0]); h=r.call('create',{'name':'host'}); h.update(video='abcdefghijk',position=1000,playing=True,speed=1.5,sampleAgeMs=200)
        r.call('publish',h); now[0]+=2; self.assertEqual(r.call('state',h)['track']['position'],4300)
        h['playing']=False;r.call('publish',h);now[0]+=1;self.assertEqual(r.call('state',h)['track']['position'],1000)
        for invalid in [-1,2001,True,1.2]:
            h['sampleAgeMs']=invalid
            with self.assertRaises(RoomError): r.call('publish',h)
