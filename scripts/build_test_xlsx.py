#!/usr/bin/env python3
"""
Build a single XLSX test workbook from CSV fixtures in assets/flota_test_database_pack/.
No external dependencies required.
"""

from __future__ import annotations

import csv
import os
import zipfile
from xml.sax.saxutils import escape


REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
SRC_DIR = os.path.join(REPO_ROOT, "assets", "flota_test_database_pack")
OUT_XLSX = os.path.join(REPO_ROOT, "assets", "flota_test_database_generated.xlsx")

SHEET_MAP = [
    ("kontakty.csv", "Kontakty"),
    ("pracownicy.csv", "Pracownicy"),
    ("zaklady.csv", "Zakłady"),
    ("rozmiary.csv", "Rozmiary"),
    ("samochody.csv", "Samochody"),
    ("zamowienia_ubrania.csv", "Zamówienia Ubrania"),
    ("historia_ubran.csv", "Historia Ubrań"),
    ("place_podglad.csv", "Płace Podgląd"),
]


def col_name(idx: int) -> str:
    name = ""
    while idx > 0:
        idx, rem = divmod(idx - 1, 26)
        name = chr(65 + rem) + name
    return name


def sheet_xml(rows: list[list[str]]) -> str:
    output = [
        '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
        '<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>',
    ]
    for r_idx, row in enumerate(rows, start=1):
        output.append(f'<row r="{r_idx}">')
        for c_idx, value in enumerate(row, start=1):
            ref = f"{col_name(c_idx)}{r_idx}"
            output.append(
                f'<c r="{ref}" t="inlineStr"><is><t>{escape(value)}</t></is></c>',
            )
        output.append("</row>")
    output.append("</sheetData></worksheet>")
    return "".join(output)


def read_csv_rows(path: str) -> list[list[str]]:
    with open(path, "r", encoding="utf-8", newline="") as fh:
        reader = csv.reader(fh)
        return [list(map(str, row)) for row in reader]


def build_xlsx() -> str:
    sheet_rows: list[tuple[str, list[list[str]]]] = []
    for file_name, sheet_name in SHEET_MAP:
        csv_path = os.path.join(SRC_DIR, file_name)
        if not os.path.exists(csv_path):
            raise FileNotFoundError(f"Brak pliku CSV: {csv_path}")
        sheet_rows.append((sheet_name, read_csv_rows(csv_path)))

    with zipfile.ZipFile(OUT_XLSX, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.writestr(
            "[Content_Types].xml",
            (
                '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
                '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
                '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
                '<Default Extension="xml" ContentType="application/xml"/>'
                '<Override PartName="/xl/workbook.xml" '
                'ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>'
                '<Override PartName="/docProps/core.xml" '
                'ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>'
                '<Override PartName="/docProps/app.xml" '
                'ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>'
                + "".join(
                    f'<Override PartName="/xl/worksheets/sheet{i}.xml" '
                    'ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>'
                    for i in range(1, len(sheet_rows) + 1)
                )
                + "</Types>"
            ),
        )
        zf.writestr(
            "_rels/.rels",
            (
                '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
                '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
                '<Relationship Id="rId1" '
                'Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" '
                'Target="xl/workbook.xml"/>'
                '<Relationship Id="rId2" '
                'Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" '
                'Target="docProps/core.xml"/>'
                '<Relationship Id="rId3" '
                'Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" '
                'Target="docProps/app.xml"/>'
                "</Relationships>"
            ),
        )
        zf.writestr(
            "docProps/core.xml",
            (
                '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
                '<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" '
                'xmlns:dc="http://purl.org/dc/elements/1.1/">'
                "<dc:title>Flota test database</dc:title>"
                "<dc:creator>Flota Codex helper</dc:creator>"
                "</cp:coreProperties>"
            ),
        )
        zf.writestr(
            "docProps/app.xml",
            (
                '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
                '<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties">'
                "<Application>Flota Codex helper</Application>"
                "</Properties>"
            ),
        )

        workbook_xml = [
            '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
            '<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" '
            'xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>',
        ]
        workbook_rels = [
            '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>',
            '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">',
        ]

        for idx, (sheet_name, rows) in enumerate(sheet_rows, start=1):
            workbook_xml.append(f'<sheet name="{escape(sheet_name)}" sheetId="{idx}" r:id="rId{idx}"/>')
            workbook_rels.append(
                f'<Relationship Id="rId{idx}" '
                'Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" '
                f'Target="worksheets/sheet{idx}.xml"/>',
            )
            zf.writestr(f"xl/worksheets/sheet{idx}.xml", sheet_xml(rows))

        workbook_xml.append("</sheets></workbook>")
        workbook_rels.append("</Relationships>")
        zf.writestr("xl/workbook.xml", "".join(workbook_xml))
        zf.writestr("xl/_rels/workbook.xml.rels", "".join(workbook_rels))

    return OUT_XLSX


if __name__ == "__main__":
    out = build_xlsx()
    print(f"Gotowe: {out}")
