import gspread
from oauth2client.service_account import ServiceAccountCredentials
from .config import SERVICE_ACCOUNT_FILE, GOOGLE_SHEET_ID

scope = [
    "https://spreadsheets.google.com/feeds",
    "https://www.googleapis.com/auth/drive"
]

def connect():

    creds = ServiceAccountCredentials.from_json_keyfile_name(
        SERVICE_ACCOUNT_FILE,
        scope
    )

    client = gspread.authorize(creds)

    return client.open_by_key(GOOGLE_SHEET_ID)
