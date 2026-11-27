import unittest, threading, json, urllib.request, urllib.error
from playlists import Playlists, PlaylistError
from rooms import Server
class TestPlaylists(unittest.TestCase):
 def setUp(self): self.now=1000;self.p=Playlists(lambda:self.now)
 def make(self): return self.p.create({'name':'<Test>','tracks':['abcdefghijk','12345678901','abcdefghijk']})
 def test_create(self):
    r=self.make(); self.assertEqual(len(r['code']),32);self.assertEqual(self.p.get(r['code'])['tracks'],['abcdefghijk','12345678901']);self.assertNotIn('revoke',self.p.get(r['code']))
 def test_delete(self):
    r=self.make();self.assertRaises(PlaylistError,self.p.delete,{'code':r['code'],'revoke':'wrong'});self.p.delete(r);self.assertRaises(PlaylistError,self.p.get,r['code'])
 def test_expiry(self):
    r=self.make();self.now+=Playlists.TTL;self.assertRaises(PlaylistError,self.p.get,r['code'])
 def test_invalid(self):
    for body in ({'name':'x','tracks':['file:///private']},{'name':'','tracks':['abcdefghijk']},{'name':'x','tracks':[]},{'name':'x','tracks':['abcdefghijk']*101},None): self.assertRaises(PlaylistError,self.p.create,body)
 def test_copy(self):
    r=self.make();self.p.get(r['code'])['tracks'].clear();self.assertEqual(len(self.p.get(r['code'])['tracks']),2)
 def test_http(self):
    server=Server(('127.0.0.1',0));t=threading.Thread(target=server.serve_forever,daemon=True);t.start();base=f'http://127.0.0.1:{server.server_port}'
    try:
        def post(path,obj):return json.load(urllib.request.urlopen(urllib.request.Request(base+path,json.dumps(obj).encode(),{'Content-Type':'application/json'})))
        r=post('/v1/playlists/create',{'name':'<script>alert(1)</script>','tracks':['abcdefghijk']});link=base+'/p/'+r['code'];self.assertEqual(json.load(urllib.request.urlopen(link+'/json'))['tracks'],['abcdefghijk']);page=urllib.request.urlopen(link).read().decode();self.assertIn('&lt;script&gt;',page);self.assertNotIn('<script>',page);self.assertNotIn(r['revoke'],page);post('/v1/playlists/delete',r);self.assertRaises(urllib.error.HTTPError,urllib.request.urlopen,link)
    finally:server.shutdown();server.server_close();t.join()
if __name__=='__main__':unittest.main()
