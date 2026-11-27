import unittest
from rooms import Rooms,RoomError
class Comments(unittest.TestCase):
 def setUp(self):
  self.r=Rooms();self.h=self.r.call('create',{'name':'Host'});self.g=self.r.call('join',{'name':'Guest','code':self.h['code']});self.v='abcdefghijk'
 def call(self,action,user,**extra):return self.r.call(action,dict(user,**extra))
 def approve(self):self.call('approve',self.h,member=self.call('state',self.h)['members'][1]['id'])
 def add(self,user,**extra):return self.call('comment_add',user,**dict({'video':self.v,'position':1200,'text':'Nice','reaction':'heart'},**extra))['comment']
 def test_pending(self):
  for action in ['comment_add','comment_list','comment_delete']:
   with self.assertRaises(RoomError):self.call(action,self.g,video=self.v,position=0,text='Hi')
 def test_shared(self):
  self.approve();c=self.add(self.g);self.assertEqual(self.call('comment_list',self.h,video=self.v)['comments'],[c]);self.assertNotIn('token',c);self.assertEqual(c['author'],'Guest')
 def test_track_scope(self):
  self.add(self.h);self.assertEqual(self.call('comment_list',self.h,video='abcdefghijx')['comments'],[])
 def test_delete(self):
  self.approve();c=self.add(self.h)
  with self.assertRaises(RoomError):self.call('comment_delete',self.g,id=c['id'])
  other=self.add(self.g);self.call('comment_delete',self.g,id=other['id']);self.call('comment_delete',self.h,id=c['id']);self.assertEqual(self.call('comment_list',self.h,video=self.v)['comments'],[])
 def test_invalid(self):
  for values in [{'position':-1},{'position':True},{'text':'x'*251},{'reaction':'bad'},{'text':'','reaction':''},{'video':'invalid'}]:
   with self.assertRaises(RoomError):self.add(self.h,**values)
 def test_room_scope(self):
  self.add(self.h);h=self.r.call('create',{});self.assertEqual(self.call('comment_list',h,video=self.v)['comments'],[])
if __name__=='__main__':unittest.main()
