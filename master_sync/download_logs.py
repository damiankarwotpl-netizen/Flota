import requests
from .config import API_URL


def get_logs():

    try:

        response = requests.post(
            API_URL,
            json={
                "action": "get_logs"
            },
            timeout=10
        )

        data = response.json()

        if not data:
            return []

        logs = []

        # pomijamy pierwszy wiersz (nagłówki)
        for row in data[1:]:

            if len(row) < 4:
                continue

            log = {
                "timestamp": row[0],
                "driver": row[1],
                "registration": row[2],
                "mileage": row[3]
            }

            logs.append(log)

        return logs

    except Exception as e:

        print("SYNC ERROR:", e)

        return []
