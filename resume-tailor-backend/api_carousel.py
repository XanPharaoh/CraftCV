import hashlib
import os
import logging
import time
from collections import defaultdict
from threading import Lock

from dotenv import load_dotenv

load_dotenv()
logger = logging.getLogger(__name__)

PROVIDERS = ["groq", "longcat", "mistral", "openrouter", "gemini"]

# ── Response cache (hash → (response, timestamp)) ──
_cache: dict[str, tuple[str, float]] = {}
_cache_lock = Lock()
CACHE_TTL_SECONDS = 86400  # 24 hours

# ── Provider success tracking for priority scoring ──
_provider_stats: dict[str, dict[str, int]] = defaultdict(lambda: {"success": 0, "fail": 0})
_stats_lock = Lock()

LLM_TIMEOUT_SECONDS = 30


def _cache_key(prompt: str) -> str:
    return hashlib.sha256(prompt.encode("utf-8")).hexdigest()


def _get_cached(key: str) -> str | None:
    with _cache_lock:
        if key in _cache:
            response, ts = _cache[key]
            if time.time() - ts < CACHE_TTL_SECONDS:
                return response
            del _cache[key]
    return None


def _set_cached(key: str, response: str) -> None:
    with _cache_lock:
        # Evict oldest entries if cache grows too large (keep < 500)
        if len(_cache) >= 500:
            oldest_key = min(_cache, key=lambda k: _cache[k][1])
            del _cache[oldest_key]
        _cache[key] = (response, time.time())


def _get_sorted_providers() -> list[str]:
    """Sort providers by success rate (highest first). New providers get priority."""
    with _stats_lock:
        def score(p: str) -> float:
            stats = _provider_stats[p]
            total = stats["success"] + stats["fail"]
            if total == 0:
                return 1.0  # untried → high priority
            return stats["success"] / total
        return sorted(PROVIDERS, key=score, reverse=True)


def _record_provider_result(provider: str, success: bool) -> None:
    with _stats_lock:
        key = "success" if success else "fail"
        _provider_stats[provider][key] += 1


def call_llm(prompt: str) -> tuple[str, str]:
    """
    Returns (response_text, provider_name_used).
    Checks cache first, then cycles through providers (sorted by reliability).
    """
    key = _cache_key(prompt)
    cached = _get_cached(key)
    if cached:
        logger.info("[Carousel] Cache hit")
        return cached, "cache"

    last_error = None
    sorted_providers = _get_sorted_providers()

    for provider in sorted_providers:
        try:
            result = _call_provider(provider, prompt)
            if result and len(result.strip()) > 10:
                logger.info(f"[Carousel] Successfully used provider: {provider}")
                _record_provider_result(provider, True)
                response = result.strip()
                _set_cached(key, response)
                return response, provider
        except Exception as e:
            last_error = e
            _record_provider_result(provider, False)
            logger.warning(f"[Carousel] {provider} failed: {type(e).__name__}: {e}")
            continue

    raise Exception(f"All providers failed. Last error: {last_error}")


def _call_provider(provider: str, prompt: str) -> str:
    if provider == "gemini":
        return _call_gemini(prompt)
    elif provider == "groq":
        return _call_groq(prompt)
    elif provider == "openrouter":
        return _call_openrouter(prompt)
    elif provider == "mistral":
        return _call_mistral(prompt)
    elif provider == "longcat":
        return _call_longcat(prompt)
    return ""


def _call_gemini(prompt: str) -> str:
    import google.generativeai as genai

    key = os.getenv("GEMINI_API_KEY")
    if not key:
        raise Exception("No Gemini key")
    genai.configure(api_key=key)
    model = genai.GenerativeModel("gemini-2.0-flash")
    response = model.generate_content(
        prompt,
        request_options={"timeout": LLM_TIMEOUT_SECONDS},
    )
    return response.text


def _call_groq(prompt: str) -> str:
    from groq import Groq

    key = os.getenv("GROQ_API_KEY")
    if not key:
        raise Exception("No Groq key")
    client = Groq(api_key=key, timeout=LLM_TIMEOUT_SECONDS)
    resp = client.chat.completions.create(
        model="llama-3.3-70b-versatile",
        messages=[{"role": "user", "content": prompt}],
        temperature=0.7,
        max_tokens=2048,
    )
    return resp.choices[0].message.content


def _call_openrouter(prompt: str) -> str:
    from openai import OpenAI

    key = os.getenv("OPENROUTER_API_KEY")
    if not key:
        raise Exception("No OpenRouter key")
    client = OpenAI(
        base_url="https://openrouter.ai/api/v1",
        api_key=key,
        timeout=LLM_TIMEOUT_SECONDS,
    )
    resp = client.chat.completions.create(
        model="meta-llama/llama-3.1-8b-instruct:free",
        messages=[{"role": "user", "content": prompt}],
        max_tokens=2048,
    )
    return resp.choices[0].message.content


def _call_mistral(prompt: str) -> str:
    from mistralai import Mistral

    key = os.getenv("MISTRAL_API_KEY")
    if not key:
        raise Exception("No Mistral key")
    client = Mistral(api_key=key, timeout_ms=LLM_TIMEOUT_SECONDS * 1000)
    resp = client.chat.complete(
        model="open-mistral-7b",
        messages=[{"role": "user", "content": prompt}],
    )
    return resp.choices[0].message.content


def _call_longcat(prompt: str) -> str:
    from openai import OpenAI

    key = os.getenv("LONGCAT_API_KEY")
    if not key:
        raise Exception("No Longcat key")
    client = OpenAI(
        base_url="https://api.longcat.chat/openai",
        api_key=key,
        timeout=LLM_TIMEOUT_SECONDS,
    )
    resp = client.chat.completions.create(
        model="LongCat-Flash-Chat",
        messages=[{"role": "user", "content": prompt}],
        max_tokens=2048,
    )
    return resp.choices[0].message.content
