"""Native Qt local player with two decks and a persistent local queue."""
import sys
from native_queue import Queue, fade_allowed, fade_gains
try:
    from PySide6.QtCore import Qt, QUrl, QTimer, QStandardPaths
    from PySide6.QtMultimedia import QAudioOutput, QMediaPlayer
    from PySide6.QtWidgets import (QApplication, QWidget, QVBoxLayout, QHBoxLayout, QLabel,
        QPushButton, QListWidget, QSlider, QFileDialog, QSpinBox, QCheckBox, QInputDialog)
except ImportError:
    raise SystemExit('Install optional native dependencies: python -m pip install -r requirements-native.txt')
from pathlib import Path


class Player(QWidget):
    def __init__(self):
        super().__init__()
        self.setWindowTitle('GANASUNO Native — Local music')
        self.resize(800, 600)
        self.queue = Queue()
        self.session = Path(QStandardPaths.writableLocation(QStandardPaths.StandardLocation.AppDataLocation)) / 'queue-v23.json'
        self.decks = [QMediaPlayer(self), QMediaPlayer(self)]
        self.outputs = [QAudioOutput(self), QAudioOutput(self)]
        for p, a in zip(self.decks, self.outputs):
            p.setAudioOutput(a)
        self.active = 0
        self.volume = .65
        self.fade = None
        self.pending = None
        self.resume_position = None
        self.timer = QTimer(self)
        self.timer.setInterval(40)
        self.timer.timeout.connect(self.tick)
        layout = QVBoxLayout(self)
        self.title = QLabel('Choose audio files')
        layout.addWidget(self.title)
        self.list = QListWidget()
        layout.addWidget(self.list)
        self.list.itemDoubleClicked.connect(lambda _: self.load(self.list.currentRow()))
        self.seek = QSlider(Qt.Orientation.Horizontal)
        self.seek.setRange(0, 0)
        self.seek.sliderMoved.connect(self.seek_to)
        layout.addWidget(self.seek)
        self.time = QLabel('0:00 / 0:00')
        layout.addWidget(self.time)
        controls = QHBoxLayout()
        for label, callback in [('Add files', self.add), ('Import M3U', self.import_playlist), ('Export M3U', self.export_playlist), ('Remove', self.remove), ('Album tag', self.edit_album), ('Previous', lambda: self.step(-1)), ('Play / Pause', self.toggle), ('Next', lambda: self.step(1)), ('Stop', self.stop)]:
            button = QPushButton(label)
            button.clicked.connect(lambda checked=False, action=callback: action())
            controls.addWidget(button)
        layout.addLayout(controls)
        self.volume_slider = QSlider(Qt.Orientation.Horizontal)
        self.volume_slider.setRange(0, 100); self.volume_slider.setValue(65)
        self.volume_slider.valueChanged.connect(self.set_volume)
        layout.addWidget(QLabel('Volume')); layout.addWidget(self.volume_slider)
        transition = QHBoxLayout()
        transition.addWidget(QLabel('Crossfade (seconds; 0 = off)'))
        self.seconds = QSpinBox(); self.seconds.setRange(0, 15); self.seconds.setValue(5)
        transition.addWidget(self.seconds)
        self.keep = QCheckBox('Keep same album continuous'); self.keep.setChecked(True)
        transition.addWidget(self.keep); layout.addLayout(transition)
        for i, p in enumerate(self.decks):
            p.mediaStatusChanged.connect(lambda status, d=i: self.status(d, status))
            p.errorOccurred.connect(lambda _error, text, d=i: self.error(d, text))
        try:
            self.queue.restore(self.session)
        except (ValueError, OSError):
            self.title.setText('Saved session could not be read; choose files again')
        self.refresh(); self.volume_slider.setValue(self.queue.volume); self.set_volume(self.queue.volume)
        self.seconds.setValue(self.queue.fade_seconds); self.keep.setChecked(self.queue.keep_albums)
        self.seconds.valueChanged.connect(lambda _: self.persist())
        self.keep.toggled.connect(lambda _: self.persist())
        if self.queue.index >= 0:
            self.title.setText('Ready to resume: ' + self.queue.files[self.queue.index].name)
        self.timer.start()

    @property
    def player(self):
        return self.decks[self.active]

    def persist(self):
        try:
            if self.queue.index >= 0 and not self.player.source().isEmpty() and self.resume_position is None:
                self.queue.position = max(0, self.player.position())
            self.queue.volume = round(self.volume * 100)
            self.queue.fade_seconds = self.seconds.value()
            self.queue.keep_albums = self.keep.isChecked()
            self.queue.save(self.session)
        except OSError:
            self.title.setText('Could not save local session')

    def refresh(self):
        self.list.clear()
        self.list.addItems([p.name + (f' · {self.queue.album(i)}' if self.queue.album(i) else '') for i,p in enumerate(self.queue.files)])
        self.list.setCurrentRow(self.queue.index)

    def add(self):
        paths, _ = QFileDialog.getOpenFileNames(self, 'Select local audio', '', 'Audio (*.mp3 *.flac *.wav *.m4a *.ogg *.opus *.aac);;All files (*)')
        self.queue.add(paths); self.refresh(); self.persist()

    def import_playlist(self):
        path, _ = QFileDialog.getOpenFileName(self, 'Import local playlist', '', 'Playlists (*.m3u *.m3u8)')
        if not path: return
        try:
            count = self.queue.import_m3u(path)
            self.refresh(); self.persist(); self.title.setText(f'Imported {count} new local files')
        except (OSError, ValueError):
            self.title.setText('Could not import playlist; use UTF-8 local M3U files')

    def export_playlist(self):
        path, _ = QFileDialog.getSaveFileName(self, 'Export local queue', 'ganasuno.m3u8', 'Playlists (*.m3u8 *.m3u)')
        if not path: return
        try:
            self.queue.export_m3u(path); self.title.setText('Local queue exported')
        except (OSError, ValueError):
            self.title.setText('Could not export playlist')

    def edit_album(self):
        i = self.list.currentRow()
        if i < 0: return
        name, ok = QInputDialog.getText(self, 'Album continuity tag', 'Album name (blank clears)', text=self.queue.album(i))
        if ok:
            self.cancel_transition(); self.queue.set_album(i, name); self.refresh(); self.persist()

    def cancel_transition(self):
        self.fade = self.pending = None
        self.decks[1-self.active].stop()
        self.decks[1-self.active].setSource(QUrl())
        self.outputs[1-self.active].setVolume(0.)
        self.outputs[self.active].setVolume(self.volume)

    def load(self, index, resume=False):
        previous = self.queue.index
        path = self.queue.select(index)
        if path is None: return
        self.resume_position = self.queue.position if resume and previous == index and self.queue.position > 0 else None
        if self.resume_position is None: self.queue.position = 0
        self.cancel_transition(); self.list.setCurrentRow(index)
        self.title.setText(path.name); self.player.setSource(QUrl.fromLocalFile(str(path)))
        self.player.play()
        if self.resume_position is None: self.persist()

    def remove(self):
        self.resume_position = None
        self.cancel_transition()
        if self.queue.remove(self.list.currentRow()):
            self.player.stop(); self.player.setSource(QUrl()); self.title.setText('Choose a track')
        self.refresh(); self.persist()

    def step(self, delta):
        self.load(self.queue.index + delta)

    def stop(self):
        self.resume_position = None
        self.cancel_transition(); self.player.stop()

    def seek_to(self, ms):
        self.resume_position = None
        self.cancel_transition(); self.player.setPosition(ms)

    def toggle(self):
        if self.player.playbackState() == QMediaPlayer.PlaybackState.PlayingState:
            self.cancel_transition(); self.player.pause()
        elif self.player.source().isEmpty():
            self.load(self.queue.index, resume=True)
        else:
            self.player.play()

    def set_volume(self, n):
        self.volume = n / 100
        if self.fade is None: self.outputs[self.active].setVolume(self.volume)

    def error(self, deck, text):
        if deck == self.active:
            self.cancel_transition(); self.title.setText('Playback error: ' + text)
        else:
            self.cancel_transition()

    def status(self, deck, status):
        if deck != self.active: return
        if self.resume_position is not None and status in (QMediaPlayer.MediaStatus.LoadedMedia, QMediaPlayer.MediaStatus.BufferedMedia):
            pos = self.resume_position
            if self.player.isSeekable() and self.player.duration() > 0:
                self.resume_position = None
                self.player.setPosition(min(pos, max(0, self.player.duration()-1)))
        if status == QMediaPlayer.MediaStatus.EndOfMedia:
            if self.fade is not None:
                self.finish_fade()
            elif self.queue.index + 1 < len(self.queue.files):
                self.step(1)

    def finish_fade(self):
        old = self.active
        self.active = 1-old
        self.fade = self.pending = None
        self.queue.select(self.queue.index + 1)
        self.decks[old].stop(); self.decks[old].setSource(QUrl()); self.outputs[old].setVolume(0.)
        self.outputs[self.active].setVolume(self.volume)
        self.list.setCurrentRow(self.queue.index)
        self.title.setText(self.queue.files[self.queue.index].name)
        self.persist()

    def tick(self):
        p = self.player
        duration, position = p.duration(), p.position()
        if self.resume_position is not None and duration > 0 and p.isSeekable():
            pos = self.resume_position; self.resume_position = None
            p.setPosition(min(pos, max(0, duration-1))); position = p.position()
        if not self.seek.isSliderDown():
            self.seek.setMaximum(max(0, duration)); self.seek.setValue(position)
        def fmt(ms):
            seconds = max(0, ms) // 1000
            return f'{seconds // 60}:{seconds % 60:02d}'
        self.time.setText(f'{fmt(position)} / {fmt(duration)}')
        if p.playbackState() != QMediaPlayer.PlaybackState.PlayingState: return
        if self.fade is not None:
            start, length = self.fade
            progress = (position - start) / max(1, length)
            a,b = fade_gains(progress)
            self.outputs[self.active].setVolume(a*self.volume)
            self.outputs[1-self.active].setVolume(b*self.volume)
            if progress >= 1: self.finish_fade()
            return
        i = self.queue.index
        if i < 0 or i+1 >= len(self.queue.files) or duration <= 0: return
        length = self.seconds.value()*1000
        if not fade_allowed(self.queue.album(i), self.queue.album(i+1), self.keep.isChecked(), float(length)): return
        length = min(length, duration//2)
        if length < 250 or duration-position > length+1500: return
        other = self.decks[1-self.active]
        if self.pending is None:
            self.pending = i+1
            self.outputs[1-self.active].setVolume(0.)
            other.setSource(QUrl.fromLocalFile(str(self.queue.files[i+1])))
        if duration-position <= length and other.mediaStatus() in (QMediaPlayer.MediaStatus.LoadedMedia, QMediaPlayer.MediaStatus.BufferedMedia):
            other.play()
            self.fade = (position, max(1, duration-position))

    def closeEvent(self, event):
        self.timer.stop(); self.persist(); self.stop(); event.accept()


if __name__ == '__main__':
    app = QApplication(sys.argv)
    app.setApplicationName('GANASUNO Native')
    widget = Player(); widget.show(); sys.exit(app.exec())
