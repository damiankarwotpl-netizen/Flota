import requests

from .config import API_URL


def _to_int(value, default=0):
    try:
        return int(float(str(value).replace(',', '.').strip()))
    except Exception:
        return default


def _extract_rows(data):
    if isinstance(data, list):
        return data
    if isinstance(data, dict):
        for key in ('rows', 'data', 'logs', 'items'):
            rows = data.get(key)
            if isinstance(rows, list):
                return rows
    return []


def get_logs():
    try:
        response = requests.post(API_URL, json={'action': 'get_logs'}, timeout=10)
        response.raise_for_status()
        data = response.json()

        rows = _extract_rows(data)
        if not rows:
            return []

        # common format from Apps Script: first row contains headers
        if rows and isinstance(rows[0], list):
            rows = rows[1:]

        logs = []
        for row in rows:
            if isinstance(row, dict):
                registration = str(row.get('registration', '')).strip().upper()
                mileage = _to_int(row.get('mileage', 0), 0)
                if registration:
                    logs.append({
                        'timestamp': row.get('timestamp', ''),
                        'driver': row.get('driver', ''),
                        'registration': registration,
                        'mileage': mileage,
                    })
                continue

            if not isinstance(row, list) or len(row) < 4:
                continue

            registration = str(row[2]).strip().upper()
            if not registration:
                continue

            logs.append({
                'timestamp': row[0],
                'driver': row[1],
                'registration': registration,
                'mileage': _to_int(row[3], 0),
            })

        return logs
    except Exception as e:
        print('SYNC ERROR:', e)
        return []
