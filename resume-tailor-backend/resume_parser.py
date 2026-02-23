import io

from pypdf import PdfReader
from docx import Document


def extract_text(file_bytes: bytes, filename: str) -> str:
    """Accepts raw file bytes + filename. Returns extracted plain text."""
    filename_lower = filename.lower()

    if filename_lower.endswith(".pdf"):
        return _extract_pdf(file_bytes)
    elif filename_lower.endswith(".docx"):
        return _extract_docx(file_bytes)
    elif filename_lower.endswith(".txt"):
        return file_bytes.decode("utf-8", errors="ignore")
    else:
        raise ValueError(f"Unsupported file type: {filename}")


def _extract_pdf(file_bytes: bytes) -> str:
    reader = PdfReader(io.BytesIO(file_bytes))
    pages = []
    for page in reader.pages:
        text = page.extract_text()
        if text:
            pages.append(text)
    raw = "\n".join(pages)
    return _clean_text(raw)


def _extract_docx(file_bytes: bytes) -> str:
    doc = Document(io.BytesIO(file_bytes))
    paragraphs = [p.text for p in doc.paragraphs if p.text.strip()]
    raw = "\n".join(paragraphs)
    return _clean_text(raw)


def _clean_text(text: str) -> str:
    lines = [line.strip() for line in text.splitlines()]
    lines = [line for line in lines if line]
    return "\n".join(lines)
