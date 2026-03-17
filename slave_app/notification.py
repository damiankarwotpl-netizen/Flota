import threading
import time
from datetime import datetime

from plyer import notification


FIRST_REMINDER_AFTER_SECONDS = 7 * 24 * 3600
NEXT_REMINDER_EVERY_SECONDS = 6 * 3600
CHECK_INTERVAL_SECONDS = 60


class MileageReminder:
    def __init__(self, state_getter, state_setter):
        self._get = state_getter
        self._set = state_setter
        self._thread = None
        self._running = False

    def start(self):
        if self._thread and self._thread.is_alive():
            return
        self._running = True
        self._thread = threading.Thread(target=self._worker, daemon=True)
        self._thread.start()

    def stop(self):
        self._running = False

    def mark_mileage_updated(self):
        now = int(time.time())
        self._set('last_mileage_update_ts', now)
        self._set('last_notified_slot', -1)

    def _notify(self):
        notification.notify(
            title='Fleet Mileage',
            message='Wpisz aktualny przebieg samochodu',
            timeout=10,
        )

    def _worker(self):
        while self._running:
            try:
                last_update = int(self._get('last_mileage_update_ts', 0) or 0)
                if last_update <= 0:
                    time.sleep(CHECK_INTERVAL_SECONDS)
                    continue

                now = int(time.time())
                start = last_update + FIRST_REMINDER_AFTER_SECONDS
                if now < start:
                    time.sleep(CHECK_INTERVAL_SECONDS)
                    continue

                slot = int((now - start) // NEXT_REMINDER_EVERY_SECONDS)
                last_notified_slot = int(self._get('last_notified_slot', -1) or -1)

                if slot > last_notified_slot:
                    self._notify()
                    self._set('last_notified_slot', slot)
            except Exception:
                # notification thread should never crash app
                pass

            time.sleep(CHECK_INTERVAL_SECONDS)


def iso_now():
    return datetime.utcnow().isoformat() + 'Z'
