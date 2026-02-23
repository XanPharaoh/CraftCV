import re


def _sanitize_input(text: str) -> str:
    """Strip characters and patterns that could be used for prompt injection."""
    # Remove common prompt injection markers
    text = re.sub(r"(?i)(ignore\s+(all\s+)?previous\s+instructions|system\s*:)", "", text)
    # Remove control characters (except newlines/tabs)
    text = re.sub(r"[\x00-\x08\x0b\x0c\x0e-\x1f\x7f]", "", text)
    return text.strip()


def build_tailor_prompt(resume_text: str, job_description: str, is_pro: bool, location: str = "") -> str:
    resume_text = _sanitize_input(resume_text)
    job_description = _sanitize_input(job_description)
    location = _sanitize_input(location)
    keyword_count = 15 if is_pro else 10
    ats_detail = (
        "Provide a detailed 20-point ATS checklist as a list of pass/fail items."
        if is_pro
        else "Provide an ATS score out of 100 with a one-sentence reason."
    )
    paragraph_count = "5" if is_pro else "3"

    location_block = ""
    if location.strip():
        location_block = f"""\nCANDIDATE LOCATION: {location}
IMPORTANT: Use currency, salary conventions, and professional norms appropriate to this location. Do NOT default to US-centric currencies or standards unless the candidate is US-based.
"""

    return f"""
You are an expert resume writer, career coach, and ATS optimization specialist.

CANDIDATE RESUME:
\"\"\"
{resume_text[:3000]}
\"\"\"

JOB DESCRIPTION:
\"\"\"
{job_description[:2000]}
\"\"\"
{location_block}

Your job is to tailor this candidate's resume for this specific role. You must produce a professional-grade structured resume AND a cover letter.

TASKS:
1. Write a professional_summary (3-4 impactful sentences):
   - Lead with years of experience and primary expertise area
   - Highlight 2-3 key achievements most relevant to this target role
   - End with a forward-looking value proposition for the employer
   - Do NOT use generic filler like "seeking new challenges" or "passionate professional"

2. Analyze ALL distinct roles/positions in the candidate's resume and create
   structured experience entries. For EACH role found, produce:
   - job_title: the position title
   - company: the company or organization name
   - dates: the employment period (e.g. "Jan 2020 – Present")
   - bullets: 2-4 powerful, tailored bullet points for that specific role

   Create at least 2 experience entries (up to 5). If the resume is vague
   about specific roles, infer reasonable entries from context.

   Each bullet must:
   - Start with a powerful action verb
   - Include at least one metric or measurable result (numbers, %, $)
   - Incorporate keywords and language from the job description
   - Be 1-2 lines long

3. Also provide tailored_bullets — a flat list of ALL bullets from every
   experience entry combined.

4. List the top {keyword_count} keywords/phrases from the JD that are
   MISSING or UNDERUSED in the resume.

5. {ats_detail}

6. Write a compelling {paragraph_count}-paragraph cover letter:
   - Do NOT start with "I am writing to apply" — use a strong hook
   - Reference 2-3 specific details from the job description
   - Highlight the candidate's most relevant achievements
   - {"Include a paragraph about cultural fit and why this company specifically." if is_pro else ""}
   - End with a confident, clear call to action
   - Sound human, not robotic

7. Suggest a professional email subject line for the application.

8. List 3-5 key requirements this role typically demands.

9. Suggest 3-5 alternative roles the candidate would also qualify for.

10. Extract the candidate's contact information from their resume.
    Look for their full name, email address, phone number, current/most recent
    job title, location/city, and LinkedIn URL. Return whatever you can find.
    If a field is not present, return an empty string for it.

11. Extract ALL of the candidate's education history into an array of objects.
    Each object should have: `institution`, `degree`, `graduation_date`.

12. Extract ALL of the candidate's core skills into a flat array of strings.

IMPORTANT: Respond ONLY with valid JSON. No explanation outside the JSON.
Use exactly this structure:

{{
  "contact_info": {{
    "full_name": "Jane Smith",
    "email": "jane.smith@email.com",
    "phone": "+1 (555) 123-4567",
    "current_title": "Senior Software Engineer",
    "location": "New York, NY",
    "linkedin_url": "linkedin.com/in/janesmith"
  }},
  "professional_summary": "Results-driven software engineer with 8+ years of experience in full-stack development...",
  "experience": [
    {{
      "job_title": "Senior Software Engineer",
      "company": "TechCorp Inc.",
      "dates": "2020 – Present",
      "bullets": [
        "Led cross-functional team of 8 engineers to deliver...",
        "Architected microservice platform reducing latency by 40%..."
      ]
    }},
    {{
      "job_title": "Software Engineer",
      "company": "StartupXYZ",
      "dates": "2017 – 2020",
      "bullets": [
        "Developed real-time analytics pipeline processing 2M events/day...",
        "Implemented CI/CD pipeline cutting deployment time by 60%..."
      ]
    }}
  ],
  "education": [
    {{
      "institution": "University of Technology",
      "degree": "B.S. Computer Science",
      "graduation_date": "May 2017"
    }}
  ],
  "skills": ["Python", "React", "Docker", "AWS"],
  "tailored_bullets": [
    "Led cross-functional team of 8 engineers to deliver...",
    "Architected microservice platform reducing latency by 40%...",
    "Developed real-time analytics pipeline processing 2M events/day...",
    "Implemented CI/CD pipeline cutting deployment time by 60%..."
  ],
  "missing_keywords": ["keyword1", "keyword2"],
  "ats_score": 74,
  "ats_reason": "Resume lacks mention of agile methodology and CI/CD.",
  "ats_checklist": [],
  "cover_letter": "Full cover letter text here...",
  "subject_line": "Application for [Role] — [Candidate Name or Relevant Skill]",
  "role_requirements": ["Requirement 1", "Requirement 2"],
  "suggested_roles": ["Role 1", "Role 2"]
}}

For free tier, ats_checklist stays an empty array.
For pro tier, populate ats_checklist with objects like:
{{"item": "Contains measurable achievements", "pass": true}}
"""


def build_cover_letter_prompt(
    resume_text: str,
    job_description: str,
    tone: str,
    is_pro: bool,
    location: str = "",
) -> str:
    resume_text = _sanitize_input(resume_text)
    job_description = _sanitize_input(job_description)
    tone = _sanitize_input(tone)
    location = _sanitize_input(location)
    paragraph_count = "5" if is_pro else "3"
    tone_guide = {
        "professional": "formal, confident, and polished",
        "conversational": "warm, personable, and approachable",
        "enthusiastic": "energetic, passionate, and highly motivated",
    }.get(tone, "professional, confident, and polished")

    location_block = ""
    if location.strip():
        location_block = f"""\nCANDIDATE LOCATION: {location}
IMPORTANT: Use locally appropriate language, currency, and professional conventions for this location.
"""

    return f"""
You are an expert cover letter writer. Write a compelling cover letter.

CANDIDATE RESUME SUMMARY:
\"\"\"
{resume_text[:2000]}
\"\"\"

JOB DESCRIPTION:
\"\"\"
{job_description[:1500]}
\"\"\"
{location_block}

TONE: {tone_guide}
LENGTH: Exactly {paragraph_count} paragraphs.

RULES:
- Do NOT start with "I am writing to apply" — use a strong hook instead
- Reference 2–3 specific details from the job description
- Highlight the candidate's most relevant achievements
- {"Include a paragraph about cultural fit and why this company specifically." if is_pro else ""}
- End with a confident, clear call to action
- Sound human, not robotic

Respond ONLY with valid JSON:
{{
  "cover_letter": "Full cover letter text here...",
  "subject_line": "Application for [Role] — [Candidate Name or Relevant Skill]"
}}
"""


def build_bullet_rewrite_prompt(bullet: str, job_description: str) -> str:
    bullet = _sanitize_input(bullet)
    job_description = _sanitize_input(job_description)
    return f"""
Rewrite this resume bullet point to be stronger, more impactful,
and better aligned with the job description.

ORIGINAL BULLET: {bullet}

JOB DESCRIPTION CONTEXT:
{job_description[:500]}

Rules:
- Start with a stronger action verb
- Add or improve a metric/result if possible
- Keep it to 1–2 lines
- Match keywords from the JD

Respond ONLY with JSON:
{{"rewritten_bullet": "..."}}
"""
