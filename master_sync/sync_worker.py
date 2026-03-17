import time
from .download_logs import get_logs
from .config import SYNC_INTERVAL


def update_mileage(conn, registration, mileage):

    try:

        cursor = conn.cursor()

        cursor.execute(
            "UPDATE cars SET mileage=? WHERE registration=?",
            (mileage, registration)
        )

        conn.commit()

    except Exception as e:

        print("DB UPDATE ERROR:", e)


def start_sync(conn):

    print("SYNC WORKER STARTED")

    while True:

        try:

            logs = get_logs()

            if logs:

                for log in logs:

                    registration = log["registration"]
                    mileage = log["mileage"]

                    update_mileage(conn, registration, mileage)

        except Exception as e:

            print("SYNC LOOP ERROR:", e)

        time.sleep(SYNC_INTERVAL)
