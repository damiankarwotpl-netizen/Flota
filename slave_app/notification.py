import threading
import time

from plyer import notification


def notify():

    notification.notify(
        title="Fleet Mileage",
        message="Wpisz przebieg samochodu",
        timeout=10
    )


def start_weekly_notification():

    def worker():

        while True:

            notify()

            # 7 dni
            time.sleep(604800)

    thread = threading.Thread(target=worker, daemon=True)

    thread.start()
