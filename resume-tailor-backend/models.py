import uuid

from sqlalchemy import Column, String, Integer, Boolean, DateTime, Text
from sqlalchemy.sql import func

from database import Base


class User(Base):
    __tablename__ = "users"

    id           = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    device_id    = Column(String, unique=True, index=True, nullable=False)
    is_pro       = Column(Boolean, default=False)
    monthly_uses = Column(Integer, default=0)
    last_reset   = Column(DateTime, default=func.now())
    created_at   = Column(DateTime, default=func.now())
    play_purchase_token = Column(String, nullable=True)
    pro_product_id      = Column(String, nullable=True)


class TailorSession(Base):
    __tablename__ = "tailor_sessions"

    id               = Column(String, primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id          = Column(String, index=True)
    resume_snippet   = Column(Text)
    job_title_hint   = Column(String)
    bullets_output   = Column(Text)
    keywords_output  = Column(Text)
    contact_info_out = Column(Text)
    summary_out      = Column(Text)
    experience_out   = Column(Text)
    education_out    = Column(Text)
    skills_out       = Column(Text)
    cover_letter_out = Column(Text)
    provider_used    = Column(String)
    created_at       = Column(DateTime, default=func.now())
