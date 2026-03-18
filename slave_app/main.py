import os
import time
from datetime import datetime
from pathlib import Path

import requests

from kivy.app import App
from kivy.storage.jsonstore import JsonStore
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.scrollview import ScrollView
from kivy.uix.textinput import TextInput
from kivy.uix.checkbox import CheckBox

try:
    from android.storage import app_storage_path
except Exception:
    app_storage_path = None

try:
    from .notification import MileageReminder
except Exception:
    from notification import MileageReminder

try:
    from pypdf import PdfReader, PdfWriter
except Exception:
    PdfReader = None
    PdfWriter = None

API_URL = "https://script.google.com/macros/s/AKfycbxFQLZU-sg8Gg58J2dE-Bbt2jTyXrdcd1DOUM78vcqFLa789gpeOC9S4MyjGHpQ12_l/exec"


def pdf_safe_text(value):
    import unicodedata

    text = "" if value is None else str(value)
    normalized = unicodedata.normalize("NFKD", text)
    return normalized.encode("latin-1", "ignore").decode("latin-1")


class SimplePdfDocument:
    def __init__(self, width=595.28, height=841.89):
        self.width = width
        self.height = height
        self.operations = []
        self.font_family = "Helvetica"
        self.font_size = 12
        self.line_width = 1

    def set_font(self, family="Helvetica", style="", size=12):
        self.font_family = family
        self.font_size = size

    def set_line_width(self, width):
        self.line_width = width

    def line(self, x1, y1, x2, y2):
        self.operations.append(f"{self.line_width:.2f} w {x1:.2f} {y1:.2f} m {x2:.2f} {y2:.2f} l S")

    def rect(self, x, y, w, h):
        self.operations.append(f"{self.line_width:.2f} w {x:.2f} {y:.2f} {w:.2f} {h:.2f} re S")

    def text(self, x, y, value):
        txt = pdf_safe_text(value).replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
        self.operations.append(f"BT /F1 {self.font_size:.2f} Tf 1 0 0 1 {x:.2f} {y:.2f} Tm ({txt}) Tj ET")

    def save(self, file_path):
        stream = "\n".join(self.operations).encode("latin-1", "ignore")
        objects = [
            b"<< /Type /Catalog /Pages 2 0 R >>",
            b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            (
                f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 {self.width:.2f} {self.height:.2f}] "
                f"/Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>"
            ).encode("latin-1"),
            b"<< /Length " + str(len(stream)).encode("ascii") + b" >>\nstream\n" + stream + b"\nendstream",
            b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
        ]

        pdf = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
        offsets = [0]
        for idx, obj in enumerate(objects, start=1):
            offsets.append(len(pdf))
            pdf.extend(f"{idx} 0 obj\n".encode("ascii"))
            pdf.extend(obj)
            pdf.extend(b"\nendobj\n")

        xref_start = len(pdf)
        pdf.extend(f"xref\n0 {len(objects) + 1}\n".encode("ascii"))
        pdf.extend(b"0000000000 65535 f \n")
        for off in offsets[1:]:
            pdf.extend(f"{off:010d} 00000 n \n".encode("ascii"))
        pdf.extend(
            f"trailer\n<< /Size {len(objects) + 1} /Root 1 0 R >>\nstartxref\n{xref_start}\n%%EOF\n".encode("ascii")
        )

        Path(file_path).write_bytes(pdf)


def pdf_safe_text(value):
    import unicodedata

    text = "" if value is None else str(value)
    normalized = unicodedata.normalize("NFKD", text)
    return normalized.encode("latin-1", "ignore").decode("latin-1")


class SimplePdfDocument:
    def __init__(self, width=595.28, height=841.89):
        self.width = width
        self.height = height
        self.operations = []
        self.font_family = "Helvetica"
        self.font_size = 12
        self.line_width = 1

    def set_font(self, family="Helvetica", style="", size=12):
        self.font_family = family
        self.font_size = size

    def set_line_width(self, width):
        self.line_width = width

    def line(self, x1, y1, x2, y2):
        self.operations.append(f"{self.line_width:.2f} w {x1:.2f} {y1:.2f} m {x2:.2f} {y2:.2f} l S")

    def rect(self, x, y, w, h):
        self.operations.append(f"{self.line_width:.2f} w {x:.2f} {y:.2f} {w:.2f} {h:.2f} re S")

    def text(self, x, y, value):
        txt = pdf_safe_text(value).replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
        self.operations.append(f"BT /F1 {self.font_size:.2f} Tf 1 0 0 1 {x:.2f} {y:.2f} Tm ({txt}) Tj ET")

    def save(self, file_path):
        stream = "\n".join(self.operations).encode("latin-1", "ignore")
        objects = [
            b"<< /Type /Catalog /Pages 2 0 R >>",
            b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            (
                f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 {self.width:.2f} {self.height:.2f}] "
                f"/Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>"
            ).encode("latin-1"),
            b"<< /Length " + str(len(stream)).encode("ascii") + b" >>\nstream\n" + stream + b"\nendstream",
            b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
        ]

        pdf = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
        offsets = [0]
        for idx, obj in enumerate(objects, start=1):
            offsets.append(len(pdf))
            pdf.extend(f"{idx} 0 obj\n".encode("ascii"))
            pdf.extend(obj)
            pdf.extend(b"\nendobj\n")

        xref_start = len(pdf)
        pdf.extend(f"xref\n0 {len(objects) + 1}\n".encode("ascii"))
        pdf.extend(b"0000000000 65535 f \n")
        for off in offsets[1:]:
            pdf.extend(f"{off:010d} 00000 n \n".encode("ascii"))
        pdf.extend(
            f"trailer\n<< /Size {len(objects) + 1} /Root 1 0 R >>\nstartxref\n{xref_start}\n%%EOF\n".encode("ascii")
        )

        Path(file_path).write_bytes(pdf)


def pdf_safe_text(value):
    import unicodedata

    text = "" if value is None else str(value)
    normalized = unicodedata.normalize("NFKD", text)
    return normalized.encode("latin-1", "ignore").decode("latin-1")


def documents_dir():
    if app_storage_path:
        shared_documents = Path("/storage/emulated/0/Documents")
        if shared_documents.exists() or os.name == "posix":
            return shared_documents
        return Path(app_storage_path())
    return Path.home() / "Documents"


class SimplePdfDocument:
    def __init__(self, width=595.28, height=841.89):
        self.width = width
        self.height = height
        self.operations = []
        self.font_family = "Helvetica"
        self.font_size = 12
        self.line_width = 1

    def set_font(self, family="Helvetica", style="", size=12):
        self.font_family = family
        self.font_size = size

    def set_line_width(self, width):
        self.line_width = width

    def line(self, x1, y1, x2, y2):
        self.operations.append(f"{self.line_width:.2f} w {x1:.2f} {y1:.2f} m {x2:.2f} {y2:.2f} l S")

    def rect(self, x, y, w, h):
        self.operations.append(f"{self.line_width:.2f} w {x:.2f} {y:.2f} {w:.2f} {h:.2f} re S")

    def text(self, x, y, value):
        txt = pdf_safe_text(value).replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
        self.operations.append(f"BT /F1 {self.font_size:.2f} Tf 1 0 0 1 {x:.2f} {y:.2f} Tm ({txt}) Tj ET")

    def save(self, file_path):
        stream = "\n".join(self.operations).encode("latin-1", "ignore")
        objects = [
            b"<< /Type /Catalog /Pages 2 0 R >>",
            b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            (
                f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 {self.width:.2f} {self.height:.2f}] "
                f"/Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>"
            ).encode("latin-1"),
            b"<< /Length " + str(len(stream)).encode("ascii") + b" >>\nstream\n" + stream + b"\nendstream",
            b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
        ]

        pdf = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
        offsets = [0]
        for idx, obj in enumerate(objects, start=1):
            offsets.append(len(pdf))
            pdf.extend(f"{idx} 0 obj\n".encode("ascii"))
            pdf.extend(obj)
            pdf.extend(b"\nendobj\n")

        xref_start = len(pdf)
        pdf.extend(f"xref\n0 {len(objects) + 1}\n".encode("ascii"))
        pdf.extend(b"0000000000 65535 f \n")
        for off in offsets[1:]:
            pdf.extend(f"{off:010d} 00000 n \n".encode("ascii"))
        pdf.extend(
            f"trailer\n<< /Size {len(objects) + 1} /Root 1 0 R >>\nstartxref\n{xref_start}\n%%EOF\n".encode("ascii")
        )

        Path(file_path).write_bytes(pdf)


def pdf_safe_text(value):
    import unicodedata

    text = "" if value is None else str(value)
    normalized = unicodedata.normalize("NFKD", text)
    return normalized.encode("latin-1", "ignore").decode("latin-1")


def documents_dir():
    if app_storage_path:
        shared_documents = Path("/storage/emulated/0/Documents")
        if shared_documents.exists() or os.name == "posix":
            return shared_documents
        return Path(app_storage_path())
    return Path.home() / "Documents"


class SimplePdfDocument:
    def __init__(self, width=595.28, height=841.89):
        self.width = width
        self.height = height
        self.operations = []
        self.font_family = "Helvetica"
        self.font_size = 12
        self.line_width = 1

    def set_font(self, family="Helvetica", style="", size=12):
        self.font_family = family
        self.font_size = size

    def set_line_width(self, width):
        self.line_width = width

    def line(self, x1, y1, x2, y2):
        self.operations.append(f"{self.line_width:.2f} w {x1:.2f} {y1:.2f} m {x2:.2f} {y2:.2f} l S")

    def rect(self, x, y, w, h):
        self.operations.append(f"{self.line_width:.2f} w {x:.2f} {y:.2f} {w:.2f} {h:.2f} re S")

    def text(self, x, y, value):
        txt = pdf_safe_text(value).replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
        self.operations.append(f"BT /F1 {self.font_size:.2f} Tf 1 0 0 1 {x:.2f} {y:.2f} Tm ({txt}) Tj ET")

    def save(self, file_path):
        stream = "\n".join(self.operations).encode("latin-1", "ignore")
        objects = [
            b"<< /Type /Catalog /Pages 2 0 R >>",
            b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            (
                f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 {self.width:.2f} {self.height:.2f}] "
                f"/Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>"
            ).encode("latin-1"),
            b"<< /Length " + str(len(stream)).encode("ascii") + b" >>\nstream\n" + stream + b"\nendstream",
            b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
        ]

        pdf = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
        offsets = [0]
        for idx, obj in enumerate(objects, start=1):
            offsets.append(len(pdf))
            pdf.extend(f"{idx} 0 obj\n".encode("ascii"))
            pdf.extend(obj)
            pdf.extend(b"\nendobj\n")

        xref_start = len(pdf)
        pdf.extend(f"xref\n0 {len(objects) + 1}\n".encode("ascii"))
        pdf.extend(b"0000000000 65535 f \n")
        for off in offsets[1:]:
            pdf.extend(f"{off:010d} 00000 n \n".encode("ascii"))
        pdf.extend(
            f"trailer\n<< /Size {len(objects) + 1} /Root 1 0 R >>\nstartxref\n{xref_start}\n%%EOF\n".encode("ascii")
        )

        Path(file_path).write_bytes(pdf)


def pdf_safe_text(value):
    import unicodedata

    text = "" if value is None else str(value)
    normalized = unicodedata.normalize("NFKD", text)
    return normalized.encode("latin-1", "ignore").decode("latin-1")


def documents_dir():
    if app_storage_path:
        shared_documents = Path("/storage/emulated/0/Documents")
        if shared_documents.exists() or os.name == "posix":
            return shared_documents
        return Path(app_storage_path())
    return Path.home() / "Documents"


class SimplePdfDocument:
    def __init__(self, width=595.28, height=841.89):
        self.width = width
        self.height = height
        self.operations = []
        self.images = []
        self.font_family = "Helvetica"
        self.font_size = 12
        self.line_width = 1

    def set_font(self, family="Helvetica", style="", size=12):
        self.font_family = family
        self.font_size = size

    def set_line_width(self, width):
        self.line_width = width

    def line(self, x1, y1, x2, y2):
        self.operations.append(f"{self.line_width:.2f} w {x1:.2f} {y1:.2f} m {x2:.2f} {y2:.2f} l S")

    def rect(self, x, y, w, h):
        self.operations.append(f"{self.line_width:.2f} w {x:.2f} {y:.2f} {w:.2f} {h:.2f} re S")

    def text(self, x, y, value):
        txt = pdf_safe_text(value).replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
        self.operations.append(f"BT /F1 {self.font_size:.2f} Tf 1 0 0 1 {x:.2f} {y:.2f} Tm ({txt}) Tj ET")

    def image_jpeg(self, path, x, y, w, h, name=None):
        path = Path(path)
        if not path.exists():
            return False
        img_w, img_h = self._jpeg_size(path)
        img_name = name or f"Im{len(self.images) + 1}"
        self.images.append({
            "name": img_name,
            "path": path,
            "width": img_w,
            "height": img_h,
        })
        self.operations.append(f"q {w:.2f} 0 0 {h:.2f} {x:.2f} {y:.2f} cm /{img_name} Do Q")
        return True

    def _jpeg_size(self, path):
        data = path.read_bytes()
        idx = 2
        while idx < len(data):
            if data[idx] != 0xFF:
                idx += 1
                continue
            marker = data[idx + 1]
            idx += 2
            if marker in (0xD8, 0xD9):
                continue
            size = int.from_bytes(data[idx:idx + 2], "big")
            if marker in (0xC0, 0xC1, 0xC2, 0xC3):
                height = int.from_bytes(data[idx + 3:idx + 5], "big")
                width = int.from_bytes(data[idx + 5:idx + 7], "big")
                return width, height
            idx += size

    def save(self, file_path):
        stream = "\n".join(self.operations).encode("latin-1", "ignore")
        xobject_entries = []
        image_objects = []
        next_obj_id = 6
        for image in self.images:
            xobject_entries.append(f"/{image['name']} {next_obj_id} 0 R")
            img_data = image["path"].read_bytes()
            image_objects.append(
                (
                    f"<< /Type /XObject /Subtype /Image /Width {image['width']} /Height {image['height']} "
                    f"/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length {len(img_data)} >>\nstream\n"
                ).encode("latin-1") + img_data + b"\nendstream"
            )
            next_obj_id += 1

        resources = "<< /Font << /F1 5 0 R >>"
        if xobject_entries:
            resources += " /XObject << " + " ".join(xobject_entries) + " >>"
        resources += " >>"

        objects = [
            b"<< /Type /Catalog /Pages 2 0 R >>",
            b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            (
                f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 {self.width:.2f} {self.height:.2f}] "
                f"/Contents 4 0 R /Resources {resources} >>"
            ).encode("latin-1"),
            b"<< /Length " + str(len(stream)).encode("ascii") + b" >>\nstream\n" + stream + b"\nendstream",
            b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
        ] + image_objects

        pdf = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
        offsets = [0]
        for idx, obj in enumerate(objects, start=1):
            offsets.append(len(pdf))
            pdf.extend(f"{idx} 0 obj\n".encode("ascii"))
            pdf.extend(obj)
            pdf.extend(b"\nendobj\n")

        xref_start = len(pdf)
        pdf.extend(f"xref\n0 {len(objects) + 1}\n".encode("ascii"))
        pdf.extend(b"0000000000 65535 f \n")
        for off in offsets[1:]:
            pdf.extend(f"{off:010d} 00000 n \n".encode("ascii"))
        pdf.extend(
            f"trailer\n<< /Size {len(objects) + 1} /Root 1 0 R >>\nstartxref\n{xref_start}\n%%EOF\n".encode("ascii")
        )

        Path(file_path).write_bytes(pdf)


def vehicle_protocol_template_path():
    candidates = [
        Path(__file__).with_name("vehicle_protocol_template.jpg"),
        Path(__file__).with_name("vehicle_protocol_template.jpeg"),
    ]
    for path in candidates:
        if path.exists():
            return path
    return None


def pdf_safe_text(value):
    import unicodedata

    text = "" if value is None else str(value)
    normalized = unicodedata.normalize("NFKD", text)
    return normalized.encode("latin-1", "ignore").decode("latin-1")


def documents_dir():
    if app_storage_path:
        shared_documents = Path("/storage/emulated/0/Documents")
        if shared_documents.exists() or os.name == "posix":
            return shared_documents
        return Path(app_storage_path())
    return Path.home() / "Documents"


class SimplePdfDocument:
    def __init__(self, width=595.28, height=841.89):
        self.width = width
        self.height = height
        self.operations = []
        self.images = []
        self.font_family = "Helvetica"
        self.font_size = 12
        self.line_width = 1

    def set_font(self, family="Helvetica", style="", size=12):
        self.font_family = family
        self.font_size = size

    def set_line_width(self, width):
        self.line_width = width

    def line(self, x1, y1, x2, y2):
        self.operations.append(f"{self.line_width:.2f} w {x1:.2f} {y1:.2f} m {x2:.2f} {y2:.2f} l S")

    def rect(self, x, y, w, h):
        self.operations.append(f"{self.line_width:.2f} w {x:.2f} {y:.2f} {w:.2f} {h:.2f} re S")

    def text(self, x, y, value):
        txt = pdf_safe_text(value).replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
        self.operations.append(f"BT /F1 {self.font_size:.2f} Tf 1 0 0 1 {x:.2f} {y:.2f} Tm ({txt}) Tj ET")

    def image_jpeg(self, path, x, y, w, h, name=None):
        path = Path(path)
        if not path.exists():
            return False
        img_w, img_h = self._jpeg_size(path)
        img_name = name or f"Im{len(self.images) + 1}"
        self.images.append({
            "name": img_name,
            "path": path,
            "width": img_w,
            "height": img_h,
        })
        self.operations.append(f"q {w:.2f} 0 0 {h:.2f} {x:.2f} {y:.2f} cm /{img_name} Do Q")
        return True

    def _jpeg_size(self, path):
        data = path.read_bytes()
        idx = 2
        while idx < len(data):
            if data[idx] != 0xFF:
                idx += 1
                continue
            marker = data[idx + 1]
            idx += 2
            if marker in (0xD8, 0xD9):
                continue
            size = int.from_bytes(data[idx:idx + 2], "big")
            if marker in (0xC0, 0xC1, 0xC2, 0xC3):
                height = int.from_bytes(data[idx + 3:idx + 5], "big")
                width = int.from_bytes(data[idx + 5:idx + 7], "big")
                return width, height
            idx += size

    def save(self, file_path):
        stream = "\n".join(self.operations).encode("latin-1", "ignore")
        xobject_entries = []
        image_objects = []
        next_obj_id = 6
        for image in self.images:
            xobject_entries.append(f"/{image['name']} {next_obj_id} 0 R")
            img_data = image["path"].read_bytes()
            image_objects.append(
                (
                    f"<< /Type /XObject /Subtype /Image /Width {image['width']} /Height {image['height']} "
                    f"/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length {len(img_data)} >>\nstream\n"
                ).encode("latin-1") + img_data + b"\nendstream"
            )
            next_obj_id += 1

        resources = "<< /Font << /F1 5 0 R >>"
        if xobject_entries:
            resources += " /XObject << " + " ".join(xobject_entries) + " >>"
        resources += " >>"

        objects = [
            b"<< /Type /Catalog /Pages 2 0 R >>",
            b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            (
                f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 {self.width:.2f} {self.height:.2f}] "
                f"/Contents 4 0 R /Resources {resources} >>"
            ).encode("latin-1"),
            b"<< /Length " + str(len(stream)).encode("ascii") + b" >>\nstream\n" + stream + b"\nendstream",
            b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
        ] + image_objects

        pdf = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
        offsets = [0]
        for idx, obj in enumerate(objects, start=1):
            offsets.append(len(pdf))
            pdf.extend(f"{idx} 0 obj\n".encode("ascii"))
            pdf.extend(obj)
            pdf.extend(b"\nendobj\n")

        xref_start = len(pdf)
        pdf.extend(f"xref\n0 {len(objects) + 1}\n".encode("ascii"))
        pdf.extend(b"0000000000 65535 f \n")
        for off in offsets[1:]:
            pdf.extend(f"{off:010d} 00000 n \n".encode("ascii"))
        pdf.extend(
            f"trailer\n<< /Size {len(objects) + 1} /Root 1 0 R >>\nstartxref\n{xref_start}\n%%EOF\n".encode("ascii")
        )

        Path(file_path).write_bytes(pdf)


def vehicle_protocol_template_path():
    candidates = [
        Path(__file__).with_name("assets").joinpath("vehicle_protocol_template.jpg"),
        Path(__file__).with_name("assets").joinpath("vehicle_protocol_template.jpeg"),
        Path(__file__).with_name("vehicle_protocol_template.jpg"),
        Path(__file__).with_name("vehicle_protocol_template.jpeg"),
    ]
    for path in candidates:
        if path.exists():
            return path
    return None


def draw_vehicle_protocol_template(pdf):
    W, H = pdf.width, pdf.height

    def box(x, y0, w, h):
        pdf.rect(x, H - y0 - h, w, h)

    def txt(x, y0, t, size=9, bold=False):
        pdf.set_font("Helvetica", "B" if bold else "", size)
        pdf.text(x, H - y0, pdf_safe_text(t))

    pdf.set_line_width(1)
    txt(180, 44, "Miesieczny Protokol Stanu Pojazdu", 15, True)
    txt(180, 58, "(Minor Damage Register)", 9)

    box(38, 74, 96, 96)
    txt(55, 124, "FUTURE", 16, True)
    txt(48, 142, "GROUP", 16, True)

    top_x = 314
    top_y = 70
    col_w = [52, 52, 58, 90]
    labels = ["Marka", "Rejestracja", "Liczba miejsc", "Wypelnione przez"]
    x = top_x
    for idx, label in enumerate(labels):
        box(x, top_y, col_w[idx], 36)
        txt(x + 4, top_y + 11, label, 7)
        x += col_w[idx]

    left_label_x = 188
    left_box_x = 258
    txt(left_label_x, 125, "Bez uszkodzen", 9)
    box(left_box_x, 112, 28, 18)
    box(left_box_x + 28, 112, 28, 18)
    txt(left_box_x + 6, 125, "TAK", 8, True)
    txt(left_box_x + 34, 125, "NIE", 8, True)
    txt(left_label_x, 154, "Nowe uszkodzenia", 9)
    box(left_box_x, 142, 56, 18)
    txt(left_label_x, 184, "Przebieg", 9)
    box(left_box_x, 172, 56, 18)
    txt(left_label_x, 213, "Wskaznik paliwa", 9)
    box(left_box_x, 201, 56, 18)
    txt(left_label_x, 242, "Rodzaj paliwa", 9)
    box(left_box_x, 230, 56, 18)
    txt(left_label_x, 271, "Poziom oleju", 9)
    box(left_box_x, 259, 56, 18)
    txt(left_label_x, 300, "Czy autobus wysprzatany?", 9)
    box(left_box_x, 288, 56, 18)
    txt(left_label_x, 329, "Czy autobus jest umyty?", 9)
    box(left_box_x, 317, 56, 18)
    txt(left_label_x, 366, "Producent i typ opony", 9)
    box(left_box_x, 346, 56, 42)

    car_x = 312
    box(car_x, 112, 214, 86)
    txt(car_x + 78, 155, "WIDOK BOK", 12, True)
    box(car_x + 10, 208, 94, 70)
    txt(car_x + 26, 246, "WIDOK PRZOD", 10, True)
    box(car_x + 118, 208, 94, 70)
    txt(car_x + 140, 246, "WIDOK TYL", 10, True)
    box(car_x + 10, 290, 202, 98)
    txt(car_x + 72, 340, "WIDOK GORA", 12, True)

    box(38, 402, 250, 86)
    txt(44, 446, "Stan opon:", 10, True)
    tire_rows = ["Lewy przedni", "Prawy przedni", "Prawy tylny", "Lewy tylny"]
    row_y = 416
    for label in tire_rows:
        txt(168, row_y + 12, label, 9)
        box(258, row_y, 56, 18)
        row_y += 20

    txt(336, 432, "Kiedy nalezy dokonac", 9)
    txt(336, 445, "przegladu technicznego?", 9)
    box(424, 420, 56, 24)
    txt(336, 472, "Kiedy nalezy dokonac", 9)
    txt(336, 485, "przegladu / service?", 9)
    box(424, 460, 56, 24)

    questions = [
        "Czy jest dowod rejestracyjny?",
        "Czy jest trojkat ostrzegawczy?",
        "Czy sa kamizelki ostrzegawcze?",
        "Czy jest apteczka?",
        "Czy jest kolo zapasowe?",
    ]
    q_y = 510
    for question in questions:
        txt(38, q_y + 16, question, 9)
        box(258, q_y, 28, 18)
        box(286, q_y, 28, 18)
        txt(264, q_y + 13, "TAK", 8, True)
        txt(292, q_y + 13, "NIE", 8, True)
        q_y += 52

    box(336, 510, 190, 190)
    txt(342, 525, "Uwagi:", 10, True)
    box(424, 720, 56, 24)
    txt(336, 730, "Od kiedy?", 9)
    txt(38, 795, "Dokument generowany automatycznie z modulu Raport stanu samochodu.", 8)


def pdf_safe_text(value):
    import unicodedata

    text = "" if value is None else str(value)
    normalized = unicodedata.normalize("NFKD", text)
    return normalized.encode("latin-1", "ignore").decode("latin-1")


def documents_dir():
    if app_storage_path:
        shared_documents = Path("/storage/emulated/0/Documents")
        if shared_documents.exists() or os.name == "posix":
            return shared_documents
        return Path(app_storage_path())
    return Path.home() / "Documents"


class SimplePdfDocument:
    def __init__(self, width=595.28, height=841.89):
        self.width = width
        self.height = height
        self.operations = []
        self.images = []
        self.font_family = "Helvetica"
        self.font_size = 12
        self.line_width = 1

    def set_font(self, family="Helvetica", style="", size=12):
        self.font_family = family
        self.font_size = size

    def set_line_width(self, width):
        self.line_width = width

    def line(self, x1, y1, x2, y2):
        self.operations.append(f"{self.line_width:.2f} w {x1:.2f} {y1:.2f} m {x2:.2f} {y2:.2f} l S")

    def rect(self, x, y, w, h):
        self.operations.append(f"{self.line_width:.2f} w {x:.2f} {y:.2f} {w:.2f} {h:.2f} re S")

    def text(self, x, y, value):
        txt = pdf_safe_text(value).replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")
        self.operations.append(f"BT /F1 {self.font_size:.2f} Tf 1 0 0 1 {x:.2f} {y:.2f} Tm ({txt}) Tj ET")

    def image_jpeg(self, path, x, y, w, h, name=None):
        path = Path(path)
        if not path.exists():
            return False
        img_w, img_h = self._jpeg_size(path)
        img_name = name or f"Im{len(self.images) + 1}"
        self.images.append({
            "name": img_name,
            "path": path,
            "width": img_w,
            "height": img_h,
        })
        self.operations.append(f"q {w:.2f} 0 0 {h:.2f} {x:.2f} {y:.2f} cm /{img_name} Do Q")
        return True

    def _jpeg_size(self, path):
        data = path.read_bytes()
        idx = 2
        while idx < len(data):
            if data[idx] != 0xFF:
                idx += 1
                continue
            marker = data[idx + 1]
            idx += 2
            if marker in (0xD8, 0xD9):
                continue
            size = int.from_bytes(data[idx:idx + 2], "big")
            if marker in (0xC0, 0xC1, 0xC2, 0xC3):
                height = int.from_bytes(data[idx + 3:idx + 5], "big")
                width = int.from_bytes(data[idx + 5:idx + 7], "big")
                return width, height
            idx += size

    def save(self, file_path):
        stream = "\n".join(self.operations).encode("latin-1", "ignore")
        xobject_entries = []
        image_objects = []
        next_obj_id = 6
        for image in self.images:
            xobject_entries.append(f"/{image['name']} {next_obj_id} 0 R")
            img_data = image["path"].read_bytes()
            image_objects.append(
                (
                    f"<< /Type /XObject /Subtype /Image /Width {image['width']} /Height {image['height']} "
                    f"/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length {len(img_data)} >>\nstream\n"
                ).encode("latin-1") + img_data + b"\nendstream"
            )
            next_obj_id += 1

        resources = "<< /Font << /F1 5 0 R >>"
        if xobject_entries:
            resources += " /XObject << " + " ".join(xobject_entries) + " >>"
        resources += " >>"

        objects = [
            b"<< /Type /Catalog /Pages 2 0 R >>",
            b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            (
                f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 {self.width:.2f} {self.height:.2f}] "
                f"/Contents 4 0 R /Resources {resources} >>"
            ).encode("latin-1"),
            b"<< /Length " + str(len(stream)).encode("ascii") + b" >>\nstream\n" + stream + b"\nendstream",
            b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
        ] + image_objects

        pdf = bytearray(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
        offsets = [0]
        for idx, obj in enumerate(objects, start=1):
            offsets.append(len(pdf))
            pdf.extend(f"{idx} 0 obj\n".encode("ascii"))
            pdf.extend(obj)
            pdf.extend(b"\nendobj\n")

        xref_start = len(pdf)
        pdf.extend(f"xref\n0 {len(objects) + 1}\n".encode("ascii"))
        pdf.extend(b"0000000000 65535 f \n")
        for off in offsets[1:]:
            pdf.extend(f"{off:010d} 00000 n \n".encode("ascii"))
        pdf.extend(
            f"trailer\n<< /Size {len(objects) + 1} /Root 1 0 R >>\nstartxref\n{xref_start}\n%%EOF\n".encode("ascii")
        )

        Path(file_path).write_bytes(pdf)


def vehicle_protocol_template_path():
    base = Path(__file__).resolve().parent
    roots = [
        base / "assets",
        base,
        base.parent / "assets",
        base.parent,
        Path.cwd() / "assets",
        Path.cwd(),
    ]
    names = [
        "vehicle_protocol_template.pdf",
        "vehicle_protocol_template.jpg",
        "vehicle_protocol_template.jpeg",
    ]
    checked = set()
    for root_path in roots:
        for name in names:
            path = (root_path / name).resolve()
            if path in checked:
                continue
            checked.add(path)
            if path.exists():
                return path
    return None


def vehicle_protocol_output_filename(registration):
    date_part = datetime.now().strftime("%Y-%m-%d")
    raw = pdf_safe_text(registration or "")
    safe = "".join(ch if ch.isalnum() else "_" for ch in raw).strip("_")
    if not safe:
        safe = "brak_rejestracji"
    return f"{date_part}_{safe}.pdf"


def draw_vehicle_protocol_template(pdf):
    W, H = pdf.width, pdf.height

    def box(x, y0, w, h):
        pdf.rect(x, H - y0 - h, w, h)

    def txt(x, y0, t, size=9, bold=False):
        pdf.set_font("Helvetica", "B" if bold else "", size)
        pdf.text(x, H - y0, pdf_safe_text(t))

    pdf.set_line_width(1)
    txt(180, 44, "Miesieczny Protokol Stanu Pojazdu", 15, True)
    txt(180, 58, "(Minor Damage Register)", 9)

    box(38, 74, 96, 96)
    txt(55, 124, "FUTURE", 16, True)
    txt(48, 142, "GROUP", 16, True)

    top_x = 314
    top_y = 70
    col_w = [52, 52, 58, 90]
    labels = ["Marka", "Rejestracja", "Liczba miejsc", "Wypelnione przez"]
    x = top_x
    for idx, label in enumerate(labels):
        box(x, top_y, col_w[idx], 36)
        txt(x + 4, top_y + 11, label, 7)
        x += col_w[idx]

    left_label_x = 188
    left_box_x = 258
    txt(left_label_x, 125, "Bez uszkodzen", 9)
    box(left_box_x, 112, 28, 18)
    box(left_box_x + 28, 112, 28, 18)
    txt(left_box_x + 6, 125, "TAK", 8, True)
    txt(left_box_x + 34, 125, "NIE", 8, True)
    txt(left_label_x, 154, "Nowe uszkodzenia", 9)
    box(left_box_x, 142, 56, 18)
    txt(left_label_x, 184, "Przebieg", 9)
    box(left_box_x, 172, 56, 18)
    txt(left_label_x, 213, "Wskaznik paliwa", 9)
    box(left_box_x, 201, 56, 18)
    txt(left_label_x, 242, "Rodzaj paliwa", 9)
    box(left_box_x, 230, 56, 18)
    txt(left_label_x, 271, "Poziom oleju", 9)
    box(left_box_x, 259, 56, 18)
    txt(left_label_x, 300, "Czy autobus wysprzatany?", 9)
    box(left_box_x, 288, 56, 18)
    txt(left_label_x, 329, "Czy autobus jest umyty?", 9)
    box(left_box_x, 317, 56, 18)
    txt(left_label_x, 366, "Producent i typ opony", 9)
    box(left_box_x, 346, 56, 42)

    car_x = 312
    box(car_x, 112, 214, 86)
    txt(car_x + 78, 155, "WIDOK BOK", 12, True)
    box(car_x + 10, 208, 94, 70)
    txt(car_x + 26, 246, "WIDOK PRZOD", 10, True)
    box(car_x + 118, 208, 94, 70)
    txt(car_x + 140, 246, "WIDOK TYL", 10, True)
    box(car_x + 10, 290, 202, 98)
    txt(car_x + 72, 340, "WIDOK GORA", 12, True)

    box(38, 402, 250, 86)
    txt(44, 446, "Stan opon:", 10, True)
    tire_rows = ["Lewy przedni", "Prawy przedni", "Prawy tylny", "Lewy tylny"]
    row_y = 416
    for label in tire_rows:
        txt(168, row_y + 12, label, 9)
        box(258, row_y, 56, 18)
        row_y += 20

    txt(336, 432, "Kiedy nalezy dokonac", 9)
    txt(336, 445, "przegladu technicznego?", 9)
    box(424, 420, 56, 24)
    txt(336, 472, "Kiedy nalezy dokonac", 9)
    txt(336, 485, "przegladu / service?", 9)
    box(424, 460, 56, 24)

    questions = [
        "Czy jest dowod rejestracyjny?",
        "Czy jest trojkat ostrzegawczy?",
        "Czy sa kamizelki ostrzegawcze?",
        "Czy jest apteczka?",
        "Czy jest kolo zapasowe?",
    ]
    q_y = 510
    for question in questions:
        txt(38, q_y + 16, question, 9)
        box(258, q_y, 28, 18)
        box(286, q_y, 28, 18)
        txt(264, q_y + 13, "TAK", 8, True)
        txt(292, q_y + 13, "NIE", 8, True)
        q_y += 52

    box(336, 510, 190, 190)
    txt(342, 525, "Uwagi:", 10, True)
    box(424, 720, 56, 24)
    txt(336, 730, "Od kiedy?", 9)
    txt(38, 795, "Dokument generowany automatycznie z modulu Raport stanu samochodu.", 8)


def load_pdf_template_page_size(template_path):
    if PdfReader is None:
        return None
    reader = PdfReader(str(template_path))
    page = reader.pages[0]
    return float(page.mediabox.width), float(page.mediabox.height)


def merge_pdf_template_with_overlay(template_path, overlay_path, output_path):
    if PdfReader is None or PdfWriter is None:
        raise RuntimeError("Brak biblioteki pypdf do obslugi szablonu PDF.")

    template_reader = PdfReader(str(template_path))
    overlay_reader = PdfReader(str(overlay_path))
    template_page = template_reader.pages[0]
    template_page.merge_page(overlay_reader.pages[0])

    writer = PdfWriter()
    writer.add_page(template_page)
    with Path(output_path).open("wb") as handle:
        writer.write(handle)


class LoginScreen(BoxLayout):
    def __init__(self, app, **kwargs):
        super().__init__(orientation="vertical", **kwargs)
        self.app = app

        self.status = Label(text="")
        self.add_widget(Label(text="Login"))
        self.login = TextInput()
        self.add_widget(self.login)

        self.add_widget(Label(text="Password"))
        self.password = TextInput(password=True)
        self.add_widget(self.password)

        btn = Button(text="Login")
        btn.bind(on_press=self.do_login)
        self.add_widget(btn)
        self.add_widget(self.status)

    def do_login(self, _instance):
        login = self.login.text.strip()
        password = self.password.text
        try:
            data = api_post({
                "action": "login",
                "login": login,
                "password": password,
            })

            if data.get("status") != "ok":
                self.status.text = "Błędny login/hasło"
                return

            self.app.set_auth(
                login=data.get("login", login),
                password=password,
                driver=data.get("name", ""),
                registration=data.get("registration", ""),
            )

            if int(data.get("change_password", 0)) == 1:
                self.app.show_change_password()
            else:
                self.app.show_mileage_screen()
                self.app.ensure_reminder_started()
        except Exception as exc:
            self.status.text = f"Błąd połączenia: {str(exc)[:60]}"


class ChangePassword(BoxLayout):
    def __init__(self, app, **kwargs):
        super().__init__(orientation="vertical", **kwargs)
        self.app = app

        self.status = Label(text="")
        self.add_widget(Label(text="Change password"))
        self.password = TextInput(password=True)
        self.add_widget(self.password)

        btn = Button(text="Save")
        btn.bind(on_press=self.change)

        self.add_widget(btn)
        self.add_widget(self.status)

    def change(self, _instance):
        try:
            new_password = self.password.text.strip()
            if not new_password:
                self.status.text = "Hasło nie może być puste"
                return

            api_post({
                "action": "change_password",
                "login": self.app.login,
                "password": new_password,
            })

            self.app.update_password(new_password)
            self.app.show_mileage_screen()
            self.app.ensure_reminder_started()
        except Exception as exc:
            self.status.text = f"Błąd zmiany hasła: {str(exc)[:60]}"


class MileageScreen(BoxLayout):
    def __init__(self, app, **kwargs):
        super().__init__(orientation="vertical", **kwargs)
        self.app = app

        self.status = Label(text="")
        self.add_widget(Label(text="Car"))
        self.add_widget(Label(text=self.app.registration))

        self.add_widget(Label(text="Mileage"))
        self.mileage = TextInput()
        self.add_widget(self.mileage)

        btn = Button(text="Save mileage")
        btn.bind(on_press=self.save)

        protocol_btn = Button(text="Raport stanu samochodu")
        protocol_btn.bind(on_press=lambda _x: self.app.show_vehicle_report_screen())

        self.add_widget(btn)
        self.add_widget(protocol_btn)
        self.add_widget(self.status)

    def save(self, _instance):
        try:
            mileage = int(self.mileage.text.strip())
            response = api_post({
                "action": "add_mileage",
                "driver": self.app.login,
                "registration": self.app.registration,
                "mileage": mileage,
            })

            if response.get("status") != "ok":
                self.status.text = response.get("message", "Błąd zapisu")
                return

            self.app.mark_mileage_updated()
            self.mileage.text = ""
            self.status.text = "Zapisano"
        except Exception as exc:
            self.status.text = f"Błąd zapisu: {str(exc)[:60]}"


class VehicleReportScreen(BoxLayout):
    def __init__(self, app, **kwargs):
        super().__init__(orientation="vertical", spacing=6, **kwargs)
        self.app = app
        self.status = Label(text="")

        self.inputs = {
            "marka": TextInput(hint_text="Marka", multiline=False),
            "rej": TextInput(text=self.app.registration, hint_text="Rejestracja", multiline=False),
            "przebieg": TextInput(hint_text="Przebieg", multiline=False),
            "olej": TextInput(hint_text="Poziom oleju", multiline=False),
            "paliwo": TextInput(hint_text="Wskaźnik paliwa", multiline=False),
            "rodzaj_paliwa": TextInput(hint_text="Rodzaj paliwa", multiline=False),
            "lp": TextInput(hint_text="Lewy przedni", multiline=False),
            "pp": TextInput(hint_text="Prawy przedni", multiline=False),
            "lt": TextInput(hint_text="Lewy tylny", multiline=False),
            "pt": TextInput(hint_text="Prawy tylny", multiline=False),
            "uszkodzenia": TextInput(hint_text="Nowe uszkodzenia"),
            "od_kiedy": TextInput(hint_text="Od kiedy?", multiline=False),
            "serwis": TextInput(hint_text="Przegląd / Service", multiline=False),
            "przeglad": TextInput(hint_text="Przegląd techniczny", multiline=False),
            "uwagi": TextInput(hint_text="Uwagi"),
        }
        self.checks = {
            "trojkat": CheckBox(),
            "kamizelki": CheckBox(),
            "kolo": CheckBox(),
            "dowod": CheckBox(),
            "apteczka": CheckBox(),
        }

        self.add_widget(Label(text="Raport stanu samochodu", size_hint_y=None, height=36))

        scroll = ScrollView()
        fields = BoxLayout(orientation="vertical", spacing=4, size_hint_y=None)
        fields.bind(minimum_height=fields.setter("height"))

        order = [
            "marka", "rej", "przebieg", "olej", "paliwo", "rodzaj_paliwa",
            "lp", "pp", "lt", "pt", "uszkodzenia", "od_kiedy", "serwis", "przeglad", "uwagi"
        ]
        for key in order:
            inp = self.inputs[key]
            inp.size_hint_y = None
            inp.height = 42 if not inp.multiline else 84
            fields.add_widget(inp)

        for key, label in [
            ("trojkat", "Trójkąt"),
            ("kamizelki", "Kamizelki"),
            ("kolo", "Koło zapasowe"),
            ("dowod", "Dowód rejestracyjny"),
            ("apteczka", "Apteczka"),
        ]:
            row = BoxLayout(size_hint_y=None, height=42)
            row.add_widget(Label(text=label))
            row.add_widget(self.checks[key])
            fields.add_widget(row)

        scroll.add_widget(fields)
        self.add_widget(scroll)

        actions = BoxLayout(size_hint_y=None, height=44, spacing=6)
        back_btn = Button(text="Wróć")
        back_btn.bind(on_press=lambda _x: self.app.show_mileage_screen())
        save_btn = Button(text="Zapisz PDF")
        save_btn.bind(on_press=self.save)
        actions.add_widget(back_btn)
        actions.add_widget(save_btn)
        self.add_widget(actions)
        self.add_widget(self.status)

    def save(self, _instance):
        data = {k: w.text for k, w in self.inputs.items()}
        data.update({k: w.active for k, w in self.checks.items()})
        try:
            output = self._generate_pdf(data)
            self.status.text = f"Zapisano: {output}"
        except Exception as exc:
            self.status.text = f"Błąd PDF: {pdf_safe_text(str(exc))[:60]}"

    def _generate_pdf(self, d):
        base_dir = documents_dir()
        base_dir.mkdir(parents=True, exist_ok=True)
        file_path = base_dir / vehicle_protocol_output_filename(d.get("rej"))

        template_path = vehicle_protocol_template_path()
        is_pdf_template = bool(template_path and template_path.suffix.lower() == ".pdf")
        pdf_size = load_pdf_template_page_size(template_path) if is_pdf_template else None
        pdf = SimplePdfDocument(*(pdf_size or (595.28, 841.89)))
        W, H = pdf.width, pdf.height
        if template_path and not is_pdf_template:
            pdf.image_jpeg(template_path, 0, 0, W, H, "ImTemplate")
        elif not template_path:
            draw_vehicle_protocol_template(pdf)

        def box(x, y0, w, h):
            pdf.rect(x, H - y0 - h, w, h)

        def hline(x1, x2, y0):
            pdf.line(x1, H - y0, x2, H - y0)

        def txt(x, y0, t, size=9, bold=False):
            pdf.set_font("Helvetica", "B" if bold else "", size)
            pdf.text(x, H - y0, pdf_safe_text(t))

        def checkbox(x, y0, checked):
            box(x, y0, 12, 12)
            if checked:
                txt(x + 3, y0 + 10, "X", 9, True)
        txt(427, 436, d["przeglad"], 8)
        txt(427, 476, d["serwis"], 8)
        txt(370, 705, d["od_kiedy"], 8)
        txt(261, 154, d["uszkodzenia"], 8)
        txt(372, 546, d["uwagi"], 8)

        if not is_pdf_template:
            txt(336, 472, "Kiedy nalezy dokonac", 9)
            txt(336, 485, "przegladu / Service?", 9)
            box(424, 460, 56, 24)

            questions = [
                ("Czy dostepny jest dowod rejestracyjny pojazdu?", d["dowod"]),
                ("Czy w samochodzie znajduje sie trojkat ostrzegawczy?", d["trojkat"]),
                ("Czy w samochodzie jest wystarczajaco duzo kamizelek", d["kamizelki"]),
                ("odblaskowych?", None),
                ("Czy dostepna jest apteczka pierwszej pomocy?", d["apteczka"]),
                ("Czy w pojezdzie znajduje sie kolo zapasowe?", d["kolo"]),
                ("Czy na wyswietlaczu aktywne sa jakies lampki", None),
                ("ostrzegawcze - jesli tak, to ktore i od kiedy?", None),
            ]
            yq = 520
            for label, state in questions:
                txt(66, yq, label, 8)
                if state is not None:
                    txt(260, yq - 8, "TAK / NIE", 8, True)
                    box(258, yq - 2, 56, 18)
                    if state:
                        txt(262, yq + 10, "TAK", 8, True)
                yq += 26

            txt(312, 520, "Inne uwagi / komentarze:", 9, True)
            box(366, 532, 190, 142)
            txt(316, 704, "Od kiedy?", 8, True)
            box(366, 690, 110, 22)

            hline(38, W - 38, 728)
            txt(170, 748, "Protokoly sa przekazywane w kazdy pierwszy poniedzialek miesiaca na adres email:", 8, True)
            txt(40, 780, "WAZNA INFORMACJA", 9, True)
            txt(290, 780, "magdalena.matusiewicz@future-group.pl", 8)
            txt(372, 796, "oraz", 8, True)
            txt(304, 812, "justyna.kucharska@future-group.pl", 8)

        if is_pdf_template:
            overlay_path = file_path.with_name(f"{file_path.stem}_overlay.pdf")
            try:
                pdf.save(overlay_path)
                merge_pdf_template_with_overlay(template_path, overlay_path, file_path)
            finally:
                if overlay_path.exists():
                    overlay_path.unlink()
        else:
            pdf.save(file_path)
        return file_path


class SlaveApp(App):
    login = None
    driver = None
    registration = None

    def build(self):
        self.root = BoxLayout()

        state_path = os.path.join(self.user_data_dir, 'slave_state.json')
        self.store = JsonStore(state_path)

        self.reminder = MileageReminder(self._get_state_value, self._set_state_value)

        auto_state = self._try_auto_login()
        if auto_state == 'mileage':
            self.show_mileage_screen()
            self.ensure_reminder_started()
        elif auto_state == 'change_password':
            self.show_change_password()
        else:
            self.show_login_screen()

        return self.root

    def _get_state_value(self, key, default=None):
        if self.store.exists('state'):
            return self.store.get('state').get(key, default)
        return default

    def _set_state_value(self, key, value):
        data = self.store.get('state') if self.store.exists('state') else {}
        data[key] = value
        self.store.put('state', **data)

    def set_auth(self, login, password, driver, registration):
        self.login = str(login or '').strip()
        self.driver = str(driver or '').strip()
        self.registration = str(registration or '').strip().upper()

        self._set_state_value('login', self.login)
        self._set_state_value('password', password)
        self._set_state_value('driver', self.driver)
        self._set_state_value('registration', self.registration)

        # start 7-day reminder window from first successful auth if not set yet
        if not self._get_state_value('last_mileage_update_ts', 0):
            self._set_state_value('last_mileage_update_ts', int(time.time()))
            self._set_state_value('last_notified_slot', -1)

    def update_password(self, password):
        self._set_state_value('password', password)

    def mark_mileage_updated(self):
        self.reminder.mark_mileage_updated()

    def ensure_reminder_started(self):
        self.reminder.start()

    def show_login_screen(self):
        self.root.clear_widgets()
        self.root.add_widget(LoginScreen(self))

    def show_change_password(self):
        self.root.clear_widgets()
        self.root.add_widget(ChangePassword(self))

    def show_mileage_screen(self):
        self.root.clear_widgets()
        self.root.add_widget(MileageScreen(self))

    def show_vehicle_report_screen(self):
        self.root.clear_widgets()
        self.root.add_widget(VehicleReportScreen(self))

    def _try_auto_login(self):
        login = self._get_state_value('login', '').strip()
        password = self._get_state_value('password', '')
        if not login or not password:
            return 'none'

        try:
            data = api_post({
                'action': 'login',
                'login': login,
                'password': password,
            })
            if data.get('status') != 'ok':
                return 'none'
            if int(data.get('change_password', 0)) == 1:
                # wymagane ręczne ustawienie nowego hasła
                self.set_auth(login, password, data.get('name', ''), data.get('registration', ''))
                return 'change_password'

            self.set_auth(login, password, data.get('name', ''), data.get('registration', ''))
            return 'mileage'
        except Exception:
            return 'none'


if __name__ == "__main__":
    SlaveApp().run()
