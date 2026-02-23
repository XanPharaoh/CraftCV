import json
import logging
import os
import re
from collections import Counter
from contextlib import asynccontextmanager

from dotenv import load_dotenv
from fastapi import FastAPI, File, Form, Header, UploadFile, Depends, HTTPException, Request
from fastapi.responses import Response
from fastapi.middleware.cors import CORSMiddleware
from slowapi import Limiter, _rate_limit_exceeded_handler
from slowapi.errors import RateLimitExceeded
from slowapi.util import get_remote_address
from sqlalchemy.orm import Session

from database import engine, get_db, Base
from models import TailorSession
from api_carousel import call_llm
from resume_parser import extract_text
from prompt_builder import (
    build_tailor_prompt,
    build_cover_letter_prompt,
    build_bullet_rewrite_prompt,
)
from docx_generator import generate_resume_docx
from usage_tracker import (
    check_and_increment,
    get_user_status,
    upgrade_to_pro,
    get_or_create_user,
)

load_dotenv()

# ── Structured logging ──
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler("app.log"),
    ],
)
logger = logging.getLogger(__name__)

# ── File upload guards ──
MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024  # 5 MB
ALLOWED_EXTENSIONS = {".pdf", ".docx", ".txt"}


# ── Create DB tables on startup ──
@asynccontextmanager
async def lifespan(app: FastAPI):
    Base.metadata.create_all(bind=engine)
    logger.info("Database tables created")
    yield


app = FastAPI(title="Resume Tailor API", version="1.0.0", lifespan=lifespan)

# ── Rate limiter ──
limiter = Limiter(key_func=get_remote_address)
app.state.limiter = limiter
app.add_exception_handler(RateLimitExceeded, _rate_limit_exceeded_handler)

app.add_middleware(
    CORSMiddleware,
    allow_origins=os.getenv("CORS_ORIGINS", "*").split(","),
    allow_methods=["*"],
    allow_headers=["*"],
)


# ── Helpers ──
def _extract_job_title(job_description: str) -> str:
    """Best-effort extraction of job title from the first few lines of a JD."""
    lines = [l.strip() for l in job_description.strip().splitlines() if l.strip()]
    for line in lines[:5]:
        # Common patterns: "Job Title: ...", "Position: ...", or just a short title line
        m = re.match(r"^(?:job\s*title|position|role)\s*[:\-–]\s*(.+)", line, re.IGNORECASE)
        if m:
            return m.group(1).strip()[:120]
    # Fallback: use first short line (< 80 chars) as title hint
    for line in lines[:3]:
        if 5 < len(line) < 80:
            return line[:120]
    return ""


def _extract_keywords(text: str) -> list[str]:
    """Extract significant keywords from text via frequency analysis."""
    # Common stop words to ignore
    stop = {
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
        "of", "with", "by", "from", "is", "are", "was", "were", "be", "been",
        "being", "have", "has", "had", "do", "does", "did", "will", "would",
        "could", "should", "may", "might", "shall", "can", "this", "that",
        "these", "those", "i", "you", "he", "she", "it", "we", "they", "me",
        "him", "her", "us", "them", "my", "your", "his", "its", "our", "their",
        "what", "which", "who", "whom", "when", "where", "why", "how",
        "not", "no", "nor", "as", "if", "then", "than", "too", "very",
        "just", "about", "up", "out", "so", "also", "each", "all", "any",
        "both", "few", "more", "most", "other", "some", "such", "only",
        "same", "into", "over", "after", "before", "between", "through",
        "during", "above", "below", "again", "further", "once", "here",
        "there", "own", "able", "experience", "work", "working", "role",
        "team", "years", "including", "must", "well", "using",
    }
    words = re.findall(r"\b[a-zA-Z][a-zA-Z+#.]{1,}\b", text.lower())
    words = [w for w in words if w not in stop and len(w) > 2]
    counter = Counter(words)
    return [word for word, _ in counter.most_common(30)]
def _clean_json_text(text: str) -> str:
    """Clean common LLM JSON formatting issues."""
    # Strip markdown code fences
    text = re.sub(r"^```(?:json)?\s*", "", text, flags=re.MULTILINE)
    text = re.sub(r"\s*```\s*$", "", text, flags=re.MULTILINE)
    # Remove trailing commas before } or ]
    text = re.sub(r",\s*([}\]])", r"\1", text)
    # Remove control characters except newlines and tabs
    text = re.sub(r"[\x00-\x08\x0b\x0c\x0e-\x1f]", "", text)
    return text.strip()


def _extract_json_object(text: str) -> str | None:
    """Find balanced JSON object in text using brace counting."""
    start = text.find("{")
    if start == -1:
        return None
    depth = 0
    in_string = False
    escape_next = False
    for i, ch in enumerate(text[start:], start):
        if escape_next:
            escape_next = False
            continue
        if ch == "\\" and in_string:
            escape_next = True
            continue
        if ch == '"':
            in_string = not in_string
            continue
        if in_string:
            continue
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return text[start:i + 1]
    return None


def safe_parse_json(text: str) -> dict:
    """Defensively parse JSON from LLM output that may include markdown fences or extra text."""
    text = text.strip()
    cleaned = _clean_json_text(text)

    # Try direct parse first
    try:
        return json.loads(cleaned)
    except json.JSONDecodeError:
        pass

    # Try extracting balanced JSON object
    extracted = _extract_json_object(cleaned)
    if extracted:
        try:
            return json.loads(extracted)
        except json.JSONDecodeError:
            # Try cleaning the extracted text too
            try:
                return json.loads(_clean_json_text(extracted))
            except json.JSONDecodeError:
                pass

    # Last resort: return a safe default so the app doesn't crash
    logger.warning(f"Failed to parse LLM JSON. Raw preview: {text[:200]}")
    return {
        "professional_summary": "",
        "experience": [],
        "education": [],
        "skills": [],
        "tailored_bullets": ["Could not parse AI response. Please try again."],
        "missing_keywords": [],
        "ats_score": 0,
        "ats_reason": "Parse error — retry.",
        "ats_checklist": [],
        "cover_letter": text[:500],
        "subject_line": "Application",
        "role_requirements": [],
        "suggested_roles": [],
    }


# ─────────────────────────────────────────────
# ROUTES
# ─────────────────────────────────────────────

@app.get("/")
def root():
    return {"status": "Resume Tailor API is live 🚀"}


@app.get("/status/{device_id}")
def user_status(device_id: str, db: Session = Depends(get_db)):
    return get_user_status(db, device_id)


# ── Quick match — free, unlimited, no LLM ──
@app.post("/quick-match")
@limiter.limit("30/minute")
async def quick_match(
    request: Request,
    job_description: str    = Form(...),
    resume_file: UploadFile = File(None),
    resume_text: str        = Form(""),
):
    """Instant keyword overlap analysis — no LLM call, no usage count."""
    if resume_file and resume_file.filename:
        ext = os.path.splitext(resume_file.filename)[1].lower()
        if ext not in ALLOWED_EXTENSIONS:
            raise HTTPException(status_code=400, detail=f"File type '{ext}' not supported.")
        file_bytes = await resume_file.read()
        if len(file_bytes) > MAX_FILE_SIZE_BYTES:
            raise HTTPException(status_code=413, detail="File too large. Maximum 5MB.")
        parsed_resume = extract_text(file_bytes, resume_file.filename)
    elif resume_text.strip():
        parsed_resume = resume_text.strip()
    else:
        raise HTTPException(status_code=400, detail="Provide either a resume file or resume_text.")

    jd_keywords = _extract_keywords(job_description)
    resume_lower = parsed_resume.lower()

    matched = [kw for kw in jd_keywords if kw in resume_lower]
    missing = [kw for kw in jd_keywords if kw not in resume_lower]

    total = len(jd_keywords) if jd_keywords else 1
    score = round(len(matched) / total * 100)

    return {
        "match_score": score,
        "matched_keywords": matched,
        "missing_keywords": missing,
        "total_keywords": total,
        "job_title_hint": _extract_job_title(job_description),
    }


# ── Main tailor endpoint ──
@app.post("/tailor")
@limiter.limit("10/minute")
async def tailor_resume(
    request: Request,
    device_id: str          = Form(...),
    job_description: str    = Form(...),
    resume_file: UploadFile = File(None),
    resume_text: str        = Form(""),
    location: str           = Form(""),
    db: Session             = Depends(get_db),
):
    # 1. Check usage
    usage = check_and_increment(db, device_id)
    if not usage["allowed"]:
        raise HTTPException(status_code=402, detail=usage)

    # 2. Get resume text
    if resume_file and resume_file.filename:
        ext = os.path.splitext(resume_file.filename)[1].lower()
        if ext not in ALLOWED_EXTENSIONS:
            raise HTTPException(
                status_code=400,
                detail=f"File type '{ext}' not supported. Use PDF, DOCX, or TXT.",
            )

        file_bytes = await resume_file.read()

        if len(file_bytes) > MAX_FILE_SIZE_BYTES:
            raise HTTPException(
                status_code=413,
                detail="File too large. Maximum size is 5MB.",
            )

        parsed_resume = extract_text(file_bytes, resume_file.filename)
    elif resume_text.strip():
        parsed_resume = resume_text.strip()
    else:
        raise HTTPException(
            status_code=400,
            detail="Provide either a resume file or resume_text.",
        )

    # 3. Determine tier
    user_status_data = get_user_status(db, device_id)
    is_pro = user_status_data["is_pro"]

    # 4. Build prompt and call LLM (combined: tailor + cover letter + insights)
    prompt = build_tailor_prompt(parsed_resume, job_description, is_pro, location)
    raw_response, provider = call_llm(prompt)
    logger.info(f"Tailor request for device={device_id} served by {provider}")

    # 5. Parse JSON from LLM (defensive)
    data = safe_parse_json(raw_response)

    # 6. Extract job title hint
    job_title = _extract_job_title(job_description)

    # 7. Save session
    session = TailorSession(
        user_id         = device_id,
        resume_snippet  = parsed_resume[:200],
        job_title_hint  = job_title,
        bullets_output  = json.dumps(data.get("tailored_bullets", [])),
        keywords_output = json.dumps(data.get("missing_keywords", [])),
        contact_info_out= json.dumps(data.get("contact_info", {})),
        summary_out     = data.get("professional_summary", ""),
        experience_out  = json.dumps(data.get("experience", [])),
        education_out   = json.dumps(data.get("education", [])),
        skills_out      = json.dumps(data.get("skills", [])),
        cover_letter_out= data.get("cover_letter", ""),
        provider_used   = provider,
    )
    db.add(session)
    db.commit()

    # Build flat tailored_bullets from experience entries if not present
    tailored_bullets = data.get("tailored_bullets", [])
    experience = data.get("experience", [])
    if not tailored_bullets and experience:
        tailored_bullets = []
        for entry in experience:
            tailored_bullets.extend(entry.get("bullets", []))

    # Extract contact info (with safe defaults)
    contact_info = data.get("contact_info", {})
    if not isinstance(contact_info, dict):
        contact_info = {}
    safe_contact = {
        "full_name":     contact_info.get("full_name", ""),
        "email":         contact_info.get("email", ""),
        "phone":         contact_info.get("phone", ""),
        "current_title": contact_info.get("current_title", ""),
        "location":      contact_info.get("location", ""),
        "linkedin_url":  contact_info.get("linkedin_url", ""),
    }

    return {
        "session_id":           session.id,
        "contact_info":         safe_contact,
        "professional_summary": data.get("professional_summary", ""),
        "experience":           experience,
        "education":            data.get("education", []),
        "skills":               data.get("skills", []),
        "tailored_bullets":     tailored_bullets,
        "missing_keywords":     data.get("missing_keywords", []),
        "ats_score":            data.get("ats_score", 0),
        "ats_reason":           data.get("ats_reason", ""),
        "ats_checklist":        data.get("ats_checklist", []),
        "cover_letter":         data.get("cover_letter", ""),
        "subject_line":         data.get("subject_line", ""),
        "role_requirements":    data.get("role_requirements", []),
        "suggested_roles":      data.get("suggested_roles", []),
        "provider_used":        provider,
        "uses_remaining":       usage["uses_remaining"],
        "is_pro":               is_pro,
    }


# ── Cover letter endpoint ──
@app.post("/cover-letter")
@limiter.limit("10/minute")
async def generate_cover_letter(
    request: Request,
    device_id: str       = Form(...),
    job_description: str = Form(...),
    resume_text: str     = Form(...),
    tone: str            = Form("professional"),
    location: str        = Form(""),
    db: Session          = Depends(get_db),
):
    user_status_data = get_user_status(db, device_id)
    is_pro = user_status_data["is_pro"]

    prompt = build_cover_letter_prompt(resume_text, job_description, tone, is_pro, location)
    raw_response, provider = call_llm(prompt)
    logger.info(f"Cover letter for device={device_id} served by {provider}")

    data = safe_parse_json(raw_response)

    # Save cover letter to latest session
    session = (
        db.query(TailorSession)
        .filter(TailorSession.user_id == device_id)
        .order_by(TailorSession.created_at.desc())
        .first()
    )
    if session:
        session.cover_letter_out = data.get("cover_letter", "")
        db.commit()

    return {
        "cover_letter":  data.get("cover_letter", ""),
        "subject_line":  data.get("subject_line", ""),
        "provider_used": provider,
        "is_pro":        is_pro,
    }


# ── DOCX download endpoint (Pro only) ──
@app.post("/generate-docx")
@limiter.limit("5/minute")
async def generate_docx(
    request: Request,
    device_id: str            = Form(...),
    bullets: str              = Form("[]"),
    cover_letter: str         = Form(""),
    template: str             = Form("professional"),
    full_name: str            = Form(""),
    current_title: str        = Form(""),
    location: str             = Form(""),
    education: str            = Form(""),
    skills: str               = Form("[]"),
    target_role: str          = Form(""),
    professional_summary: str = Form(""),
    experience: str           = Form("[]"),
    db: Session               = Depends(get_db),
):
    user_status_data = get_user_status(db, device_id)
    if not user_status_data["is_pro"]:
        raise HTTPException(
            status_code=403,
            detail="DOCX download is a Pro feature. Please upgrade.",
        )

    try:
        bullets_list = json.loads(bullets)
        skills_list = json.loads(skills)
        experience_list = json.loads(experience)
    except json.JSONDecodeError:
        raise HTTPException(status_code=400, detail="Invalid JSON in request fields.")

    if template not in ("professional", "modern", "minimal"):
        template = "professional"

    docx_bytes = generate_resume_docx(
        full_name             = full_name,
        current_title         = current_title,
        location              = location,
        education             = education,
        skills                = skills_list,
        target_role           = target_role,
        bullets               = bullets_list,
        cover_letter          = cover_letter,
        template              = template,
        professional_summary  = professional_summary,
        experience            = experience_list,
    )

    return Response(
        content    = docx_bytes,
        media_type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        headers    = {"Content-Disposition": f"attachment; filename=resume_{template}.docx"},
    )


# ── Bullet rewriter (Pro only) ──
@app.post("/rewrite-bullet")
@limiter.limit("10/minute")
async def rewrite_bullet(
    request: Request,
    device_id: str       = Form(...),
    bullet: str          = Form(...),
    job_description: str = Form(...),
    db: Session          = Depends(get_db),
):
    user_status_data = get_user_status(db, device_id)
    if not user_status_data["is_pro"]:
        raise HTTPException(
            status_code=403,
            detail="Bullet rewriter is a Pro feature.",
        )

    prompt = build_bullet_rewrite_prompt(bullet, job_description)
    raw_response, provider = call_llm(prompt)

    data = safe_parse_json(raw_response)

    return {
        "rewritten_bullet": data.get("rewritten_bullet", ""),
        "provider_used":    provider,
    }


# ── Upgrade user to Pro (guarded by shared secret) ──
@app.post("/upgrade")
@limiter.limit("3/minute")
def upgrade_user(
    request: Request,
    device_id: str        = Form(...),
    x_upgrade_secret: str = Header(...),
    db: Session           = Depends(get_db),
):
    expected = os.getenv("UPGRADE_SECRET", "")
    if not expected or x_upgrade_secret != expected:
        raise HTTPException(status_code=403, detail="Invalid upgrade secret.")

    user = upgrade_to_pro(db, device_id)
    logger.info(f"User {device_id} upgraded to Pro")
    return {"success": True, "is_pro": user.is_pro}


# ── Verify Google Play purchase and grant Pro ──
@app.post("/verify-purchase")
@limiter.limit("5/minute")
def verify_purchase(
    request: Request,
    device_id:      str = Form(...),
    purchase_token: str = Form(...),
    product_id:     str = Form(...),
    db: Session = Depends(get_db),
):
    """
    Validates a Google Play subscription purchase server-side and grants Pro access.
    Requires GOOGLE_PLAY_SERVICE_ACCOUNT_JSON (base64-encoded) and
    GOOGLE_PLAY_PACKAGE_NAME env vars for real validation; falls back to trusting
    the token when those are absent (useful for initial testing).
    """
    VALID_PRODUCT_IDS = {"craftcv_pro_monthly", "craftcv_pro_yearly"}
    if product_id not in VALID_PRODUCT_IDS:
        raise HTTPException(status_code=400, detail="Unknown product_id.")

    service_account_b64 = os.getenv("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON", "")
    package_name        = os.getenv("GOOGLE_PLAY_PACKAGE_NAME", "com.resumetailor.app")

    if service_account_b64:
        # Full server-side validation via Google Play Developer API
        try:
            import base64
            from google.oauth2 import service_account
            from googleapiclient.discovery import build as google_build

            sa_json = base64.b64decode(service_account_b64).decode("utf-8")
            sa_info = json.loads(sa_json)
            credentials = service_account.Credentials.from_service_account_info(
                sa_info,
                scopes=["https://www.googleapis.com/auth/androidpublisher"],
            )
            service = google_build("androidpublisher", "v3", credentials=credentials, cache_discovery=False)
            result = (
                service.purchases()
                .subscriptions()
                .get(
                    packageName=package_name,
                    subscriptionId=product_id,
                    token=purchase_token,
                )
                .execute()
            )
            # paymentState: 1 = received, 2 = free trial, 0 = pending
            payment_state = result.get("paymentState", 0)
            if payment_state not in (1, 2):
                raise HTTPException(status_code=402, detail="Purchase not completed.")
        except HTTPException:
            raise
        except Exception as exc:
            logger.error(f"Google Play verification failed: {exc}")
            raise HTTPException(status_code=502, detail="Could not verify purchase with Google Play.")

    # Grant Pro and persist token
    user = upgrade_to_pro(db, device_id)
    user.play_purchase_token = purchase_token
    user.pro_product_id      = product_id
    db.commit()

    logger.info(f"User {device_id} verified purchase for {product_id} → Pro granted")
    return {"status": "upgraded", "device_id": device_id, "is_pro": True}


# ── Session history (Pro only) ──
@app.get("/history/{device_id}")
def get_history(
    device_id: str,
    db: Session = Depends(get_db),
):
    user_status_data = get_user_status(db, device_id)
    if not user_status_data["is_pro"]:
        raise HTTPException(status_code=403, detail="History is a Pro feature.")

    sessions = (
        db.query(TailorSession)
        .filter(TailorSession.user_id == device_id)
        .order_by(TailorSession.created_at.desc())
        .limit(10)
        .all()
    )

    return [
        {
            "session_id":     s.id,
            "resume_snippet": s.resume_snippet,
            "job_title_hint": s.job_title_hint,
            "created_at":     s.created_at.isoformat(),
        }
        for s in sessions
    ]
