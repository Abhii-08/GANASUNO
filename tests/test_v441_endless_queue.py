"""Verify production paths are wired to the endless-queue policy."""
import unittest
from pathlib import Path

BASE = Path(__file__).resolve().parents[1] / "app/src/main/java/com/abhi/ganasuno"
SERVICE = (BASE / "player/PlaybackService.kt").read_text(encoding="utf-8")
ACTIVITY = (BASE / "MainActivity.kt").read_text(encoding="utf-8")


class EndlessQueue(unittest.TestCase):
    def test_next_paths(self):
        for expected in (
            "b.miniNext.setOnClickListener { skipTrack(true) }",
            'transport(R.drawable.ic_next,"Next track"){skipTrack(true)}',
            "else skipTrack(true)",
            "1->skipTrack(true);2->closeMini()",
            "PlayerHub.extendQueueForNext",
        ):
            self.assertIn(expected, ACTIVITY)
        self.assertIn("PlayerHub.extendQueueForNext = { callback -> extendQueue(player, callback) }", SERVICE)

    def test_refill_and_retry(self):
        for expected in (
            'LibraryStore.option("endless") == 1',
            "EndlessQueuePolicy.shouldRefill",
            "getNextWindowIndex(index, Player.REPEAT_MODE_OFF, p.shuffleModeEnabled)",
            "mixRetryAt = SystemClock.elapsedRealtime() + EndlessQueuePolicy.RETRY_MS",
            "onNext == null && SystemClock.elapsedRealtime() < mixRetryAt",
        ):
            self.assertIn(expected, SERVICE)
        self.assertNotIn("attemptedMix", SERVICE)
        self.assertNotRegex(SERVICE, r"mediaItemCount\s*>=\s*500")

    def test_safe_append(self):
        for expected in (
            "revision != mixRevision",
            "p.currentMediaItem !== item",
            "LibraryStore.allowed(it.id)",
            "p.addMediaItems",
            "pendingNext = null",
            "PlayerHub.roomActive",
            "recentMixIds",
            "EndlessQueuePolicy.trimPrefix",
        ):
            self.assertIn(expected, SERVICE)
