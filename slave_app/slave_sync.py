import gspread
from datetime import datetime
from oauth2client.service_account import ServiceAccountCredentials

SHEET_ID = "TU_WSTAW_ID_ARKUSZA"

scope = [
    "https://spreadsheets.google.com/feeds",
    "https://www.googleapis.com/auth/drive"
]

def connect():

    creds = ServiceAccountCredentials.from_json_keyfile_name(
        "credentials.json",
        scope
    )

    client = gspread.authorize(creds)

    return client.open_by_key(SHEET_ID)


class SlaveSync:

    def get_car(self, name, surname):

        sheet = connect().worksheet("car_assignments")

        rows = sheet.get_all_records()

        for r in rows:

            if r["name"] == name and r["surname"] == surname:

                return r["plate"]

        return None


    def send(self, plate, mileage):

        queue = connect().worksheet("sync_queue")

        queue.append_row([

            "",
            "mileage_update",
            plate,
            mileage,
            datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
            0

        ])
