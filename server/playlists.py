"""Opt-in, unlisted playlist snapshots. Public track IDs only; no audio, cookies or local paths."""
import re, secrets, threading, time

class PlaylistError(ValueError): pass

class Playlists:
    TTL = 7 * 86400
    def __init__(self, clock=time.time):
        self.clock=clock; self.entries={}; self.lock=threading.RLock()
    def _clean(self):
        now=self.clock()
        self.entries={k:v for k,v in self.entries.items() if v['expires']>now}
    def create(self, body):
        if not isinstance(body,dict): raise PlaylistError('Invalid input')
        name=body.get('name'); tracks=body.get('tracks')
        if not isinstance(name,str) or not name.strip() or len(name)>100 or not isinstance(tracks,list) or not 1<=len(tracks)<=100: raise PlaylistError('Invalid playlist')
        ids=[]
        for track in tracks:
            if not isinstance(track,str) or not re.fullmatch(r'[A-Za-z0-9_-]{11}',track): raise PlaylistError('Invalid public video ID')
            if track not in ids: ids.append(track)
        with self.lock:
            self._clean()
            if len(self.entries)>=500: raise PlaylistError('Server capacity reached')
            code=secrets.token_urlsafe(24); revoke=secrets.token_urlsafe(32)
            self.entries[code]={'name':name.strip(),'tracks':ids,'expires':int(self.clock()+self.TTL),'revoke':revoke}
            return {'code':code,'revoke':revoke,'expires':self.entries[code]['expires']}
    def get(self, code):
        with self.lock:
            self._clean(); row=self.entries.get(code)
            if not row: raise PlaylistError('Not found or expired')
            return {k:(list(v) if isinstance(v,list) else v) for k,v in row.items() if k!='revoke'}
    def delete(self, body):
        if not isinstance(body,dict): raise PlaylistError('Invalid input')
        code=body.get('code'); token=body.get('revoke')
        if not isinstance(code,str) or not isinstance(token,str): raise PlaylistError('Invalid token')
        with self.lock:
            self._clean(); row=self.entries.get(code)
            if not row or not secrets.compare_digest(row['revoke'],token): raise PlaylistError('Invalid token or expired')
            del self.entries[code]; return {'ok':True}
