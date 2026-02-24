package com.craftcv.app.data.models

import com.google.gson.annotations.SerializedName

data class TailorResponse(
    @SerializedName("tailored_bullets")      val tailoredBullets: List<String>           = emptyList(),
    @SerializedName("missing_keywords")      val missingKeywords: List<String>           = emptyList(),
    @SerializedName("ats_score")             val atsScore: Int                           = 0,
    @SerializedName("ats_reason")            val atsReason: String                       = "",
    @SerializedName("ats_checklist")         val atsChecklist: List<Map<String, Any>>    = emptyList(),
    @SerializedName("cover_letter")          val coverLetter: String                     = "",
    @SerializedName("subject_line")          val subjectLine: String                     = "",
    @SerializedName("role_requirements")     val roleRequirements: List<String>          = emptyList(),
    @SerializedName("suggested_roles")       val suggestedRoles: List<String>            = emptyList(),
    @SerializedName("uses_remaining")        val usesRemaining: Int                      = 0,
    @SerializedName("is_pro")                val isPro: Boolean                          = false,
    @SerializedName("professional_summary")  val professionalSummary: String              = "",
    @SerializedName("contact_info")          val contactInfo: ContactInfo                = ContactInfo(),
    val experience: List<ExperienceEntry>                                                = emptyList(),
    val education: List<EducationEntry>                                                  = emptyList(),
    val skills: List<String>                                                             = emptyList(),
    val provider: String = "",
)

data class EducationEntry(
    val institution: String = "",
    val degree: String = "",
    @SerializedName("graduation_date") val graduationDate: String = "",
)

data class ContactInfo(
    @SerializedName("full_name")     val fullName: String     = "",
    val email: String                                         = "",
    val phone: String                                         = "",
    @SerializedName("current_title") val currentTitle: String = "",
    val location: String                                      = "",
    @SerializedName("linkedin_url")  val linkedinUrl: String  = "",
)

data class ExperienceEntry(
    @SerializedName("job_title") val jobTitle: String       = "",
    val company: String                                     = "",
    val dates: String                                       = "",
    val bullets: List<String>                                = emptyList(),
)

data class CoverLetterResponse(
    @SerializedName("cover_letter") val coverLetter: String   = "",
    @SerializedName("subject_line") val subjectLine: String   = "",
    val provider: String = "",
)

data class RewriteResponse(
    @SerializedName("rewritten_bullet") val rewrittenBullet: String = "",
    val provider: String  = "",
)

data class QuickMatchResponse(
    @SerializedName("match_score")       val matchScore: Int          = 0,
    @SerializedName("matched_keywords")  val matchedKeywords: List<String> = emptyList(),
    @SerializedName("missing_keywords")  val missingKeywords: List<String> = emptyList(),
    @SerializedName("total_keywords")    val totalKeywords: Int       = 0,
    @SerializedName("job_title_hint")    val jobTitleHint: String     = "",
)

data class HistoryItem(
    @SerializedName("session_id")     val sessionId: String  = "",
    @SerializedName("resume_snippet") val resumeSnippet: String = "",
    @SerializedName("job_title_hint") val jobTitleHint: String = "",
    @SerializedName("created_at")     val createdAt: String  = "",
)

data class UserStatus(
    @SerializedName("is_pro")          val isPro: Boolean     = false,
    @SerializedName("monthly_uses")    val monthlyUses: Int   = 0,
    @SerializedName("uses_remaining")  val usesRemaining: Int = 5,
)

data class UpgradeResponse(
    val status: String                      = "",
    @SerializedName("device_id") val deviceId: String = "",
    @SerializedName("is_pro") val isPro: Boolean = false,
)

data class HealthResponse(val status: String = "")

data class ErrorResponse(val detail: String = "")

// Local-only — not sent to API
data class UserProfile(
    val fullName: String        = "",
    val currentTitle: String    = "",
    val location: String        = "",
    val yearsExperience: String = "",
    val skills: List<String>    = emptyList(),
    val education: String       = "",
    val targetRole: String      = "",
    val resumeUriString: String = "",
)
