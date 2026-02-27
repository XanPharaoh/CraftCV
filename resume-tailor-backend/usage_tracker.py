from datetime import datetime, timezone

from sqlalchemy.orm import Session

from models import User

FREE_MONTHLY_LIMIT = 2


def get_or_create_user(db: Session, device_id: str) -> User:
    user = db.query(User).filter(User.device_id == device_id).first()
    if not user:
        user = User(device_id=device_id)
        db.add(user)
        db.commit()
        db.refresh(user)
    return user


def check_and_increment(db: Session, device_id: str) -> dict:
    """
    Returns {"allowed": bool, "uses_remaining": int, "is_pro": bool}
    Resets monthly counter if a new calendar month has started.
    """
    user = get_or_create_user(db, device_id)

    now = datetime.now(timezone.utc)
    last_reset = user.last_reset

    # Make last_reset timezone-aware for comparison
    if last_reset.tzinfo is None:
        last_reset = last_reset.replace(tzinfo=timezone.utc)

    # Reset counter on new month
    if now.month != last_reset.month or now.year != last_reset.year:
        user.monthly_uses = 0
        user.last_reset   = now
        db.commit()

    # Pro users always allowed
    if user.is_pro:
        user.monthly_uses += 1
        db.commit()
        return {"allowed": True, "uses_remaining": 999, "is_pro": True}

    # Free tier limit
    if user.monthly_uses >= FREE_MONTHLY_LIMIT:
        return {"allowed": False, "uses_remaining": 0, "is_pro": False}

    user.monthly_uses += 1
    db.commit()
    return {
        "allowed":        True,
        "uses_remaining": FREE_MONTHLY_LIMIT - user.monthly_uses,
        "is_pro":         False,
    }


def upgrade_to_pro(db: Session, device_id: str) -> User:
    user = get_or_create_user(db, device_id)
    user.is_pro = True
    db.commit()
    db.refresh(user)
    return user


def grant_ad_use(db: Session, device_id: str) -> dict:
    """Grant +1 use after user watched rewarded ads."""
    user = get_or_create_user(db, device_id)
    if user.is_pro:
        return {"granted": True, "uses_remaining": 999, "is_pro": True}
    if user.monthly_uses > 0:
        user.monthly_uses -= 1
        db.commit()
    uses_remaining = max(0, FREE_MONTHLY_LIMIT - user.monthly_uses)
    return {"granted": True, "uses_remaining": uses_remaining, "is_pro": False}


def get_user_status(db: Session, device_id: str) -> dict:
    user = get_or_create_user(db, device_id)
    uses_remaining = 999 if user.is_pro else max(0, FREE_MONTHLY_LIMIT - user.monthly_uses)
    return {
        "is_pro":        user.is_pro,
        "monthly_uses":  user.monthly_uses,
        "uses_remaining": uses_remaining,
    }

