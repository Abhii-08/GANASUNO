"""Persistent local-file queue and crossfade decisions; independent of Qt."""
import json
import math
import os
from pathlib import Path


def fade_allowed(first, second, keep_albums, seconds):
    if not math.isfinite(seconds) or seconds <= 0:
        return False
    a, b = (str(x).strip().casefold() for x in (first, second))
    return not (keep_albums and a and a == b)


def fade_gains(fraction):
    fraction = min(1., max(0., fraction)) if math.isfinite(fraction) else 0.
    return math.cos(fraction * math.pi / 2), math.sin(fraction * math.pi / 2)


class Queue:
    def __init__(self):
        self.files = []
        self.index = -1
        self.albums = {}
        self.position = 0
        self.volume = 65
        self.fade_seconds = 5
        self.keep_albums = True

    def add(self, paths):
        for path in paths:
            file = Path(path).resolve()
            if file.is_file() and file not in self.files and len(self.files) < 2000:
                self.files.append(file)
        if self.index < 0 and self.files:
            self.index = 0

    def import_m3u(self, path):
        """Import only existing local files; never dereference network URLs."""
        path = Path(path)
        if path.stat().st_size > 1_000_000:
            raise ValueError('Playlist is too large')
        lines = path.read_text(encoding='utf-8-sig').splitlines()
        candidates = []
        for line in lines[:10000]:
            line = line.strip()
            if not line or line.startswith('#') or '\x00' in line or '://' in line or line.startswith(('//', '\\\\')):
                continue
            # Reject URI schemes, but allow Windows drive letters.
            if ':' in line and not (os.name == 'nt' and len(line) > 2 and line[1] == ':' and line[2] in '/\\'):
                continue
            candidate = Path(line)
            if not candidate.is_absolute():
                candidate = path.parent / candidate
            if candidate.suffix.lower() not in ('.mp3','.wav','.flac','.m4a','.ogg','.opus','.aac','.wma'):
                continue
            try:
                if candidate.is_file(): candidates.append(candidate)
            except OSError:
                continue
        before = len(self.files)
        self.add(candidates)
        return len(self.files) - before

    def export_m3u(self, path):
        path = Path(path)
        if path.resolve() in self.files:
            raise ValueError('Cannot overwrite an audio file')
        if path.suffix.lower() not in ('.m3u', '.m3u8'):
            raise ValueError('Choose an M3U or M3U8 file')
        lines = ['#EXTM3U']
        for item in self.files:
            value = str(item)
            if '\n' in value or '\r' in value or '\x00' in value:
                continue
            lines.append(value)
        path.write_text('\n'.join(lines)+'\n', encoding='utf-8')

    def select(self, index):
        if 0 <= index < len(self.files):
            if index != self.index:
                self.position = 0
            self.index = index
            return self.files[index]
        return None

    def step(self, delta):
        return self.select(self.index + delta)

    def remove(self, index):
        if not 0 <= index < len(self.files):
            return False
        current = index == self.index
        self.albums.pop(self.files[index], None)
        self.files.pop(index)
        if current:
            self.position = 0
        if not self.files:
            self.index = -1
        elif index < self.index:
            self.index -= 1
        elif current:
            self.index = min(index, len(self.files) - 1)
        return current

    def set_album(self, index, name):
        if 0 <= index < len(self.files):
            self.albums[self.files[index]] = str(name).strip()[:200]

    def album(self, index):
        return self.albums.get(self.files[index], '') if 0 <= index < len(self.files) else ''

    def save(self, path):
        path = Path(path)
        path.parent.mkdir(parents=True, exist_ok=True)
        tmp = path.with_suffix('.tmp')
        data = {'version': 2, 'position': self.position, 'volume': self.volume, 'fade_seconds': self.fade_seconds, 'keep_albums': self.keep_albums, 'files': [str(p) for p in self.files], 'selected': str(self.files[self.index]) if 0 <= self.index < len(self.files) else '', 'albums': {str(p): a for p, a in self.albums.items() if p in self.files}}
        tmp.write_text(json.dumps(data, ensure_ascii=False), encoding='utf-8')
        os.replace(tmp, path)

    def restore(self, path):
        path = Path(path)
        if not path.is_file():
            return
        if path.stat().st_size > 4_000_000:
            raise ValueError('Session too large')
        data = json.loads(path.read_text(encoding='utf-8'))
        if not isinstance(data, dict) or data.get('version') not in (1, 2) or not isinstance(data.get('files'), list):
            raise ValueError('Invalid session')
        fresh = Queue()
        fresh.add([p for p in data['files'][:2000] if isinstance(p, str) and '\x00' not in p])
        tags = data.get('albums', {})
        if isinstance(tags, dict):
            for i, p in enumerate(fresh.files):
                if isinstance(tags.get(str(p)), str):
                    fresh.set_album(i, tags[str(p)])
        selected = data.get('selected')
        if isinstance(selected, str):
            fresh.index = next((i for i,p in enumerate(fresh.files) if str(p) == selected), fresh.index)
        def bounded(key, default, maximum):
            val = data.get(key, default)
            return min(max(val, 0), maximum) if type(val) is int else default
        fresh.position = bounded('position', 0, 86_400_000) if fresh.index >= 0 and str(fresh.files[fresh.index]) == selected else 0
        fresh.volume = bounded('volume', 65, 100)
        fresh.fade_seconds = bounded('fade_seconds', 5, 15)
        fresh.keep_albums = data.get('keep_albums') if type(data.get('keep_albums')) is bool else True
        self.__dict__.update(fresh.__dict__)
