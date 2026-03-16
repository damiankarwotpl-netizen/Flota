from .google_api import connect
from .config import QUEUE_SHEET


class MileageSync:

    def __init__(self, conn):

        self.conn = conn
        sheet = connect()

        self.queue = sheet.worksheet(QUEUE_SHEET)

    def sync(self):

        rows = self.queue.get_all_records()

        for i, row in enumerate(rows):

            if int(row["processed"]) == 0:

                plate = row["plate"]
                mileage = int(row["mileage"])

                self.update_mileage(plate, mileage)

                self.queue.update_cell(i + 2, 6, 1)

    def update_mileage(self, plate, mileage):

        cur = self.conn.cursor()

        cur.execute(
            "UPDATE cars SET mileage=? WHERE registration=?",
            (mileage, plate)
        )

        self.conn.commit()
