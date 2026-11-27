import unittest, threading, urllib.request, json
from rooms import Rooms, RoomError, Server
class TestRooms(unittest.TestCase):
 def setUp(self):
    self.now=1000;self.r=Rooms(lambda:self.now);self.h=self.r.call('create',{'name':'Host'});self.g=self.r.call('join',{'code':self.h['code'],'name':'Guest'})
 def call(self,action,who,**extra):return self.r.call(action,dict(who,**extra))
 def approve(self):
    m=self.call('state',self.h)['members'][1]['id'];self.call('approve',self.h,member=m);return m
 def test_approval(self):
    self.assertFalse(self.call('state',self.g)['approved']);self.approve();self.assertTrue(self.call('state',self.g)['approved'])
 def test_sync(self):
    self.approve();self.call('publish',self.h,video='abcdefghijk',position=2000,playing=True);self.now+=3;self.assertEqual(self.call('state',self.g)['track']['position'],5000)
 def test_paused(self):
    self.call('publish',self.h,video='abcdefghijk',position=2000,playing=False);self.now+=3;self.assertEqual(self.call('state',self.h)['track']['position'],2000)
 def test_permissions(self):
    with self.assertRaises(RoomError):self.call('publish',self.g,video='abcdefghijk',position=1,playing=True)
    with self.assertRaises(RoomError):self.call('suggest',self.g,video='abcdefghijk')
 def test_transfer(self):
    m=self.approve();self.call('transfer',self.h,member=m);self.assertTrue(self.call('state',self.g)['host'])
 def test_kick(self):
    m=self.approve();self.call('kick',self.h,member=m)
    with self.assertRaises(RoomError):self.call('state',self.g)
 def test_suggestions(self):
    self.approve();self.call('suggest',self.g,video='abcdefghijk',title='Track');self.assertEqual(self.call('state',self.h)['suggestions'][0]['title'],'Track')
 def test_auto(self):
    self.call('auto',self.h,enabled=True);g=self.r.call('join',{'code':self.h['code']});self.assertTrue(self.call('state',g)['approved'])
 def test_expiry(self):
    self.now+=21601
    with self.assertRaises(RoomError):self.call('state',self.h)
 def test_stale(self):
    self.call("publish",self.h,video="abcdefghijk",position=2000,playing=True);self.now+=30;state=self.call("state",self.h)["track"];self.assertFalse(state["playing"]);self.assertEqual(state["position"],17000)
 def test_block_installation(self):
    host=self.r.call('create',{'identity':'a'*64});guest=self.r.call('join',{'code':host['code'],'identity':'b'*64,'name':'Guest'})
    m=self.call('state',host)['members'][1];self.assertNotIn('identity',m)
    with self.assertRaises(RoomError):self.call('block',guest,member=self.call('state',host)['members'][0]['id'])
    self.call('block',host,member=m['id'])
    with self.assertRaises(RoomError):self.call('state',guest)
    with self.assertRaises(RoomError):self.r.call('join',{'code':host['code'],'identity':'b'*64,'name':'Changed name'})
    with self.assertRaises(RoomError):self.r.call('join',{'code':host['code']})
    self.assertIn('token',self.r.call('join',{'code':host['code'],'identity':'c'*64}))
 def test_block_multiple_sessions(self):
    host=self.r.call('create',{'identity':'a'*64});g1=self.r.call('join',{'code':host['code'],'identity':'b'*64});g2=self.r.call('join',{'code':host['code'],'identity':'b'*64})
    self.call('block',host,member=self.call('state',host)['members'][1]['id'])
    for guest in (g1,g2):
      with self.assertRaises(RoomError):self.call('state',guest)
 def test_http(self):
    s=Server(('127.0.0.1',0));t=threading.Thread(target=s.serve_forever,daemon=True);t.start()
    try:
      req=urllib.request.Request(f'http://127.0.0.1:{s.server_port}/v1/create',data=b'{"name":"test"}',headers={'Content-Type':'application/json'})
      with urllib.request.urlopen(req,timeout=2) as res:self.assertEqual(len(json.load(res)['code']),16)
    finally:s.shutdown();s.server_close();t.join()
if __name__=='__main__': unittest.main()
