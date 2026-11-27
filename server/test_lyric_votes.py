import unittest,tempfile,os
from lyric_votes import LyricVotes
class VoteTests(unittest.TestCase):
    def setUp(self): self.s=LyricVotes();self.a='a'*64;self.b='b'*64;self.l='c'*64
    def tearDown(self): self.s.db.close()
    def vote(self,voter,value=None,lyric=None): return self.s.call({'lyric':lyric or self.l,'voter':voter,'value':value})
    def test_replace_remove(self):
        self.assertEqual(self.vote(self.a,1),{'up':1,'down':0,'mine':1});self.assertEqual(self.vote(self.a,1)['up'],1);self.assertEqual(self.vote(self.a,-1),{'up':0,'down':1,'mine':-1});self.assertEqual(self.vote(self.a,0),{'up':0,'down':0,'mine':0})
    def test_two_voters(self):
        self.vote(self.a,1);self.assertEqual(self.vote(self.b,-1),{'up':1,'down':1,'mine':-1});self.assertEqual(self.vote(self.a),{'up':1,'down':1,'mine':1})
    def test_versions(self):
        self.vote(self.a,1);self.assertEqual(self.vote(self.a,lyric='d'*64)['up'],0)
    def test_validation(self):
        for val in (True,2,'1',[],{}):
            with self.assertRaises(ValueError):self.vote(self.a,val)
        with self.assertRaises(ValueError):self.vote('short',1)
        with self.assertRaises(ValueError):self.vote(self.a,1,'short')
    def test_persistence(self):
        with tempfile.TemporaryDirectory() as d:
            f=os.path.join(d,'votes.db');s=LyricVotes(f);s.call({'lyric':self.l,'voter':self.a,'value':1});s.db.close();s=LyricVotes(f);self.assertEqual(s.call({'lyric':self.l,'voter':self.a})['up'],1);s.db.close()
    def test_http(self):
        import threading,json,urllib.request
        from rooms import Server
        server=Server(('127.0.0.1',0));t=threading.Thread(target=server.serve_forever,daemon=True);t.start()
        try:
            req=urllib.request.Request(f'http://127.0.0.1:{server.server_port}/v1/lyrics/vote',json.dumps({'lyric':self.l,'voter':self.a,'value':1}).encode(),{'Content-Type':'application/json'})
            with urllib.request.urlopen(req,timeout=3) as response:self.assertEqual(json.load(response),{'up':1,'down':0,'mine':1})
        finally:server.shutdown();server.server_close();t.join()
if __name__=='__main__':unittest.main()
