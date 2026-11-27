"""Optional local desktop window for the offline player. No network or account integration."""
from pathlib import Path


class WindowControls:
    def __init__(self):
        self._window = None

    def _attach(self, window):
        self._window = window

    def mini(self, enabled):
        if type(enabled) is not bool or self._window is None:
            return False
        self._window.resize(480 if enabled else 1120, 450 if enabled else 800)
        return True

    def quit(self):
        if self._window is None:
            return False
        self._window.destroy()
        return True


def main():
    try:
        import webview
    except ImportError:
        raise SystemExit('Install the optional desktop dependency: python -m pip install -r requirements.txt')
    controls = WindowControls()
    # A fixed local HTML entry point; no arbitrary URL or command interface.
    window = webview.create_window('GANASUNO Offline Player', str(Path(__file__).with_name('index.html').resolve()), js_api=controls, width=1120, height=800, min_size=(400, 380))
    controls._attach(window)
    webview.start(private_mode=False)


if __name__ == '__main__':
    main()
