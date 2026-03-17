import time

from .config import SYNC_INTERVAL
from .download_logs import get_logs


def update_mileage(conn, registration, mileage):
    try:
        cursor = conn.cursor()
        cursor.execute(
            'UPDATE cars SET mileage = CASE WHEN mileage > ? THEN mileage ELSE ? END WHERE UPPER(registration)=?',
            (mileage, mileage, str(registration).upper()),
        )
        conn.commit()
    except Exception as e:
        print('DB UPDATE ERROR:', e)


def start_sync(conn):
    print('SYNC WORKER STARTED')

    while True:
        try:
            logs = get_logs()
            for log in logs:
                registration = str(log.get('registration', '')).strip().upper()
                mileage = int(log.get('mileage', 0) or 0)
                if registration:
                    update_mileage(conn, registration, max(0, mileage))
        except Exception as e:
            print('SYNC LOOP ERROR:', e)

        time.sleep(SYNC_INTERVAL)
