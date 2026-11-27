"""Opt-in anonymous rating of an exact lyric version. No lyric text or account data."""
import re, sqlite3, threading

class LyricVotes:
    def __init__(self, path=':memory:'):
        self.db=sqlite3.connect(path,check_same_thread=False)
        self.lock=threading.RLock()
        self.db.execute('CREATE TABLE IF NOT EXISTS votes (lyric TEXT NOT NULL, voter TEXT NOT NULL, value INTEGER NOT NULL CHECK(value IN (-1,1)), PRIMARY KEY(lyric,voter))')
        self.db.commit()
    def call(self, body):
        if not isinstance(body,dict): raise ValueError('Invalid input')
        lyric=body.get('lyric'); voter=body.get('voter'); value=body.get('value')
        if not isinstance(lyric,str) or not re.fullmatch(r'[a-f0-9]{64}',lyric): raise ValueError('Invalid lyric version')
        if not isinstance(voter,str) or not re.fullmatch(r'[a-f0-9]{64}',voter): raise ValueError('Invalid voter token')
        if value is not None and (type(value) is not int or value not in (-1,0,1)): raise ValueError('Invalid vote')
        with self.lock,self.db:
            if value==0:self.db.execute('DELETE FROM votes WHERE lyric=? AND voter=?',(lyric,voter))
            elif value is not None:
                exists=self.db.execute('SELECT 1 FROM votes WHERE lyric=? AND voter=?',(lyric,voter)).fetchone()
                if not exists and self.db.execute('SELECT COUNT(*) FROM votes').fetchone()[0]>=100000: raise ValueError('Capacity reached')
                self.db.execute('INSERT INTO votes VALUES (?,?,?) ON CONFLICT(lyric,voter) DO UPDATE SET value=excluded.value',(lyric,voter,value))
            up,down=self.db.execute('SELECT COALESCE(SUM(value=1),0), COALESCE(SUM(value=-1),0) FROM votes WHERE lyric=?',(lyric,)).fetchone()
            row=self.db.execute('SELECT value FROM votes WHERE lyric=? AND voter=?',(lyric,voter)).fetchone()
            return {'up':up,'down':down,'mine':row[0] if row else 0}
