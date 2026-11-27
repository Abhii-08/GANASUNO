"""GANASUNO private listening rooms; stdlib only. No audio or credentials transferred."""
import argparse, json, re, secrets, ssl, threading, time, html, math
from playlists import Playlists, PlaylistError
from lyric_votes import LyricVotes
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

class RoomError(Exception): pass
class Rooms:
    def __init__(self, clock=time.monotonic):
        self.clock=clock; self.rooms={}; self.lock=threading.RLock()
    def call(self, action, body):
        if not isinstance(body,dict): raise RoomError('Invalid request')
        with self.lock:
            now=self.clock()
            self.rooms={k:v for k,v in self.rooms.items() if now-v['seen']<21600}
            name=str(body.get('name','Guest')).strip()[:40] or 'Guest'
            identity=body.get('identity','')
            if identity and (not isinstance(identity,str) or not re.fullmatch(r'[a-f0-9]{64}',identity)): raise RoomError('Invalid identity')
            if action=='create':
                if len(self.rooms)>=100: raise RoomError('Room capacity reached')
                code=secrets.token_hex(8); token=secrets.token_urlsafe(32)
                self.rooms[code]={'host':token,'members':{token:{'id':secrets.token_hex(6),'name':name,'approved':True,'identity':identity}},'blocked':set(),'identity_required':bool(identity),'auto':False,'state':None,'suggestions':[],'comments':[],'seen':now}
                return {'code':code,'token':token}
            code=body.get('code'); room=self.rooms.get(code) if isinstance(code,str) else None
            if not room: raise RoomError('Room not found or expired')
            if action=='join':
                if room['identity_required'] and not identity: raise RoomError('Installation token required')
                if identity and identity in room['blocked']: raise RoomError('Blocked')
                if len(room['members'])>=16: raise RoomError('Room full')
                token=secrets.token_urlsafe(32); room['members'][token]={'id':secrets.token_hex(6),'name':name,'approved':room['auto'],'identity':identity}; room['seen']=now
                return {'code':code,'token':token}
            token=body.get('token'); member=room['members'].get(token) if isinstance(token,str) else None
            if not member: raise RoomError('Membership ended')
            host=token==room['host']; room['seen']=now
            def require_host():
                if not host: raise RoomError('Host only')
            def target():
                pair=next(((t,m) for t,m in room['members'].items() if m['id']==body.get('member')),None)
                if not pair or pair[0]==token: raise RoomError('Invalid member')
                return pair
            def track():
                video=body.get('video')
                if not isinstance(video,str) or not re.fullmatch(r'[A-Za-z0-9_-]{11}',video): raise RoomError('Invalid track ID')
                return {'video':video,'title':str(body.get('title',''))[:150],'artist':str(body.get('artist',''))[:100]}
            if action=='state':
                s=room['state']; result={'host':host,'approved':member['approved'],'auto':room['auto'],'members':[], 'suggestions':[], 'track':None}
                if member['approved']:
                    result['members']=[dict({k:v for k,v in m.items() if k!='identity'},host=t==room['host']) for t,m in room['members'].items() if host or m['approved']]
                    if host: result['suggestions']=list(room['suggestions'])
                    if s:
                        age=max(0,now-s['at'])
                        result['track']=dict(s,position=min(86400000,s['position']+(int(min(age,15)*1000*s.get('speed',1.0)) if s['playing'] else 0)),playing=s['playing'] and age<=15)
                return result
            if action=='leave':
                if host: del self.rooms[code]
                else: del room['members'][token]
            elif action=='publish':
                require_host(); s=track(); position=body.get('position'); playing=body.get('playing')
                if type(position) is not int or not 0<=position<=86400000 or type(playing) is not bool: raise RoomError('Invalid playback state')
                speed=body.get('speed',1.0)
                if type(speed) not in (int,float) or not 0.5<=speed<=2.0 or not math.isfinite(speed): raise RoomError('Invalid speed')
                age=body.get('sampleAgeMs',0)
                if type(age) is not int or not 0<=age<=2000: raise RoomError('Invalid sample age')
                if playing: position=min(86400000,position+int(age*speed))
                room['state']=dict(s,position=position,playing=playing,at=now,speed=speed)
            elif action=='auto':
                require_host(); room['auto']=body.get('enabled') is True
            elif action in ('approve','kick','block','transfer'):
                require_host(); t,m=target()
                if action=='approve': m['approved']=True
                elif action=='kick': del room['members'][t]
                elif action=='block':
                    if not m['identity']: raise RoomError('Member does not support blocking')
                    room['blocked'].add(m['identity'])
                    room['members']={token:entry for token,entry in room['members'].items() if entry['identity']!=m['identity'] or token==room['host']}
                elif m['approved']: room['host']=t
                else: raise RoomError('Approve member first')
            elif action=='suggest':
                if not member['approved']: raise RoomError('Approval required')
                s=track(); s['from']=member['name']; room['suggestions']=(room['suggestions']+[s])[-30:]
            elif action=='comment_list':
                if not member['approved']: raise RoomError('Approval required')
                video=track()['video']
                return {'comments':[dict(c) for c in room['comments'] if c['video']==video][-30:]}
            elif action=='comment_add':
                if not member['approved']: raise RoomError('Approval required')
                video=track()['video']; position=body.get('position'); text=body.get('text',''); reaction=body.get('reaction','')
                if type(position) is not int or not 0<=position<=86400000 or not isinstance(text,str) or len(text)>250 or not isinstance(reaction,str) or reaction not in ('','heart','star','fire') or not (text.strip() or reaction): raise RoomError('Invalid comment')
                if len(room['comments'])>=200: raise RoomError('Comment capacity reached')
                comment={'id':secrets.token_hex(12),'video':video,'position':position,'text':text.strip(),'reaction':reaction,'author':member['name'],'member':member['id']}
                room['comments'].append(comment); return {'comment':dict(comment)}
            elif action=='comment_delete':
                if not member['approved']: raise RoomError('Approval required')
                comment=next((c for c in room['comments'] if c['id']==body.get('id')),None)
                if not comment or (not host and comment['member']!=member['id']): raise RoomError('Not allowed')
                room['comments'].remove(comment)
            elif action=='clear_suggestions': require_host(); room['suggestions']=[]
            else: raise RoomError('Unknown action')
            return {'ok':True}

class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        try:
            n=int(self.headers.get('Content-Length','0'))
            if not 0<n<=16384: raise RoomError('Invalid body size')
            if not self.path.startswith('/v1/'): raise RoomError('Invalid route')
            body=json.loads(self.rfile.read(n))
            if self.path=='/v1/playlists/create': result=self.server.playlists.create(body)
            elif self.path=='/v1/playlists/delete': result=self.server.playlists.delete(body)
            elif self.path=='/v1/lyrics/vote': result=self.server.lyric_votes.call(body)
            else: result=self.server.rooms.call(self.path[4:],body)
            status=200
        except (RoomError,ValueError,TypeError):
            status=400; result={'error':'Request rejected; check room, approval, or input'}
        data=json.dumps(result).encode(); self.send_response(status);self.send_header('Content-Type','application/json');self.send_header('Cache-Control','no-store');self.send_header('Content-Length',str(len(data)));self.end_headers();self.wfile.write(data)
    def do_GET(self):
        match=re.fullmatch(r'/p/([A-Za-z0-9_-]{32})(/json)?',self.path)
        try:
            if not match: raise PlaylistError('Not found')
            row=self.server.playlists.get(match.group(1))
            if match.group(2):
                data=json.dumps(row).encode(); mime='application/json'
            else:
                title=html.escape(row['name']); links=''.join(f'<li><a href="https://music.youtube.com/watch?v={i}" rel="noreferrer">{i}</a></li>' for i in row['tracks'])
                data=(f'<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width"><title>{title}</title><h1>{title}</h1><p>GANASUNO shared playlist · {len(row["tracks"])} public tracks. Expires after seven days or on server restart.</p><ol>{links}</ol></html>').encode(); mime='text/html; charset=utf-8'
            status=200
        except PlaylistError:
            status=404;data=b'Playlist not found or expired';mime='text/plain'
        self.send_response(status);self.send_header('Content-Type',mime);self.send_header('Cache-Control','no-store');self.send_header('Referrer-Policy','no-referrer');self.send_header('X-Content-Type-Options','nosniff');self.send_header('Content-Security-Policy',"default-src 'none'; base-uri 'none'; frame-ancestors 'none'");self.send_header('Content-Length',str(len(data)));self.end_headers();self.wfile.write(data)
    def log_message(self,*args): pass # no room metadata or tokens in request logs

class Server(ThreadingHTTPServer):
    daemon_threads=True
    def __init__(self,address): super().__init__(address,Handler);self.rooms=Rooms();self.playlists=Playlists();self.lyric_votes=LyricVotes()
    def server_close(self):
        super().server_close();self.lyric_votes.db.close()
    def get_request(self):
        s,a=super().get_request();s.settimeout(10);return s,a

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--host',default='127.0.0.1');p.add_argument('--port',type=int,default=8765);p.add_argument('--cert');p.add_argument('--key');p.add_argument('--votes-db',default='lyric-votes.sqlite3');a=p.parse_args()
    if bool(a.cert)!=bool(a.key): p.error('Provide both cert and key')
    if a.host not in ('127.0.0.1','localhost','::1') and not a.cert: p.error('Non-loopback requires TLS; otherwise use an HTTPS reverse proxy')
    server=Server((a.host,a.port));server.lyric_votes.db.close();server.lyric_votes=LyricVotes(a.votes_db)
    if a.cert:
        ctx=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER);ctx.load_cert_chain(a.cert,a.key);server.socket=ctx.wrap_socket(server.socket,server_side=True)
    print(f'Room server listening on {a.host}:{a.port}');server.serve_forever()
