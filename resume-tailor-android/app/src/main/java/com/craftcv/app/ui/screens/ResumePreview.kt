package com.craftcv.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcv.app.data.models.ContactInfo
import com.craftcv.app.data.models.EducationEntry
import com.craftcv.app.data.models.ExperienceEntry
import com.craftcv.app.data.models.UserProfile

/**
 * Visual resume preview that renders a realistic mock of the final DOCX
 * right inside the app. Styled to look like a real document on paper.
 */
@Composable
fun ResumePreviewCard(
    profile: UserProfile?,
    selectedBullets: List<String>,
    selectedKeywords: List<String>,
    template: String,
    modifier: Modifier = Modifier,
    experience: List<ExperienceEntry> = emptyList(),
    professionalSummary: String = "",
    contactInfo: ContactInfo = ContactInfo(),
    educationEntries: List<EducationEntry> = emptyList(),
    activeSkills: List<String> = emptyList(),
    onEditSection: (String) -> Unit = {},
) {
    // Merge activeData (from draft) with profile — draft takes priority for live editing
    val name = contactInfo.fullName.ifBlank { profile?.fullName?.ifBlank { "Your Name" } ?: "Your Name" }
    val title = contactInfo.currentTitle.ifBlank { profile?.currentTitle?.ifBlank { profile.targetRole } ?: "" }
    val location = contactInfo.location.ifBlank { profile?.location ?: "" }
    val education = if (educationEntries.isNotEmpty()) {
        educationEntries.joinToString("\n") { entry ->
            buildString {
                append(entry.degree)
                if (entry.institution.isNotBlank()) append(" \u2014 ${entry.institution}")
                if (entry.graduationDate.isNotBlank()) append(" (${entry.graduationDate})")
            }
        }
    } else {
        profile?.education ?: ""
    }
    val skills = activeSkills.ifEmpty { profile?.skills ?: emptyList() }
    val targetRole = profile?.targetRole ?: ""

    // Paper shadow + white card to look like a real document
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(4.dp)),
        shape = RoundedCornerShape(4.dp),
        color = Color.White,
        border = BorderStroke(0.5.dp, Color(0xFFDDDDDD)),
    ) {
        when (template) {
            "modern" -> ModernPreview(name, title, location, education, skills, targetRole, selectedBullets, selectedKeywords, experience, professionalSummary, onEditSection)
            "minimal" -> MinimalPreview(name, title, location, education, skills, targetRole, selectedBullets, selectedKeywords, experience, professionalSummary, onEditSection)
            else -> ProfessionalPreview(name, title, location, education, skills, targetRole, selectedBullets, selectedKeywords, experience, professionalSummary, onEditSection)
        }
    }
}

// ── Professional Template Preview ──────────────────────────────────────────

@Composable
private fun ProfessionalPreview(
    name: String, title: String, location: String, education: String,
    skills: List<String>, targetRole: String,
    bullets: List<String>, keywords: List<String>,
    experience: List<ExperienceEntry> = emptyList(),
    professionalSummary: String = "",
    onEditSection: (String) -> Unit,
) {
    val headerColor = Color(0xFF2C3E50)
    val ink = Color(0xFF333333)
    val gray = Color(0xFF7F8C8D)

    Column(modifier = Modifier.padding(24.dp)) {
        // Name & Contact
        Column(modifier = Modifier.fillMaxWidth().clickable { onEditSection("contact") }) {
            Text(
                name.uppercase(),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = headerColor,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

        // Contact line
        val contactParts = listOfNotNull(
            title.ifBlank { null },
            location.ifBlank { null },
        )
        if (contactParts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                contactParts.joinToString("  ·  "),
                fontSize = 10.sp,
                color = gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        }
        
        // Header divider
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(thickness = 2.dp, color = headerColor)
        Spacer(modifier = Modifier.height(12.dp))

        // Professional Summary
        if (professionalSummary.isNotBlank()) {
            Column(modifier = Modifier.fillMaxWidth().clickable { onEditSection("summary") }) {
                SectionHeader("PROFESSIONAL SUMMARY", headerColor)
                Spacer(modifier = Modifier.height(6.dp))
                Text(professionalSummary, fontSize = 10.sp, color = ink, lineHeight = 15.sp)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Core Competencies
        if (skills.isNotEmpty() || keywords.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().clickable { onEditSection("skills") }) {
                val allSkills = (skills + keywords).distinct()
                SectionHeader("CORE COMPETENCIES", headerColor)
                Spacer(modifier = Modifier.height(6.dp))
                val rows = allSkills.chunked(3)
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        row.forEach { skill ->
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Text("▸", fontSize = 8.sp, color = headerColor)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(skill, fontSize = 9.sp, color = ink, maxLines = 1)
                            }
                        }
                        repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Professional Experience
        if (experience.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().clickable { onEditSection("experience") }) {
                SectionHeader("PROFESSIONAL EXPERIENCE", headerColor)
                Spacer(modifier = Modifier.height(6.dp))
                experience.forEach { entry ->
                Text(entry.jobTitle, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = headerColor)
                val sub = listOfNotNull(entry.company.ifBlank { null }, entry.dates.ifBlank { null })
                if (sub.isNotEmpty()) {
                    Text(sub.joinToString("  |  "), fontSize = 9.sp, color = gray, fontStyle = FontStyle.Italic)
                }
                Spacer(modifier = Modifier.height(4.dp))
                entry.bullets.forEach { bullet -> BulletPoint(bullet, ink, Color(0xFF999999)) }
                Spacer(modifier = Modifier.height(8.dp))
            }
            }
        } else if (bullets.isNotEmpty()) {
            SectionHeader("PROFESSIONAL EXPERIENCE", headerColor)
            Spacer(modifier = Modifier.height(6.dp))
            val role = targetRole.ifBlank { title }
            if (role.isNotBlank()) {
                Text(role, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = headerColor)
                Spacer(modifier = Modifier.height(4.dp))
            }
            bullets.forEach { bullet -> BulletPoint(bullet, ink, Color(0xFF999999)) }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Education
        if (education.isNotBlank()) {
            Column(modifier = Modifier.fillMaxWidth().clickable { onEditSection("education") }) {
                SectionHeader("EDUCATION", headerColor)
                Spacer(modifier = Modifier.height(6.dp))
                Text(education, fontSize = 10.sp, color = ink)
            }
        }
    }
}

// ── Modern Template Preview ────────────────────────────────────────────────

@Composable
private fun ModernPreview(
    name: String, title: String, location: String, education: String,
    skills: List<String>, targetRole: String,
    bullets: List<String>, keywords: List<String>,
    experience: List<ExperienceEntry> = emptyList(),
    professionalSummary: String = "",
    onEditSection: (String) -> Unit,
) {
    val accent = Color(0xFF1A6BDB)
    val ink = Color(0xFF333333)
    val gray = Color(0xFF7F8C8D)

    Column {
        // Blue header block (Contact)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(accent)
                .clickable { onEditSection("contact") }
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                val parts = listOfNotNull(title.ifBlank { null }, location.ifBlank { null })
                if (parts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(parts.joinToString("  |  "), fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                }
            }
        }

        Column(modifier = Modifier.padding(24.dp)) {
            // Professional Summary
            if (professionalSummary.isNotBlank()) {
                Column(modifier = Modifier.fillMaxWidth().clickable { onEditSection("summary") }) {
                    Text("Summary", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = accent)
                    Spacer(modifier = Modifier.height(2.dp))
                    HorizontalDivider(thickness = 1.dp, color = accent.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(professionalSummary, fontSize = 10.sp, color = ink, lineHeight = 15.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Skills
            if (skills.isNotEmpty() || keywords.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().clickable { onEditSection("skills") }) {
                    val allSkills = (skills + keywords).distinct()
                    Text("Skills", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = accent)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(allSkills.joinToString("  •  "), fontSize = 9.sp, color = ink)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Experience
            if (experience.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().clickable { onEditSection("experience") }) {
                    Text("Experience", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = accent)
                    Spacer(modifier = Modifier.height(2.dp))
                    HorizontalDivider(thickness = 1.dp, color = accent.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(6.dp))
                    experience.forEach { entry ->
                        Text(entry.jobTitle, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = ink)
                        val sub = listOfNotNull(entry.company.ifBlank { null }, entry.dates.ifBlank { null })
                        if (sub.isNotEmpty()) {
                            Text(sub.joinToString("  |  "), fontSize = 9.sp, color = gray, fontStyle = FontStyle.Italic)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        entry.bullets.forEach { bullet -> BulletPoint(bullet, ink, accent.copy(alpha = 0.5f)) }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            } else if (bullets.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().clickable { onEditSection("experience") }) {
                    Text("Experience", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = accent)
                    Spacer(modifier = Modifier.height(2.dp))
                    HorizontalDivider(thickness = 1.dp, color = accent.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(6.dp))
                    val role = targetRole.ifBlank { title }
                    if (role.isNotBlank()) {
                        Text(role, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = ink)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    bullets.forEach { bullet -> BulletPoint(bullet, ink, accent.copy(alpha = 0.5f)) }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Education
            if (education.isNotBlank()) {
                Column(modifier = Modifier.fillMaxWidth().clickable { onEditSection("education") }) {
                    Text("Education", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = accent)
                    Spacer(modifier = Modifier.height(2.dp))
                    HorizontalDivider(thickness = 1.dp, color = accent.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(education, fontSize = 10.sp, color = ink)
                }
            }
        }
    }
}

// ── Minimal Template Preview ───────────────────────────────────────────────

@Composable
private fun MinimalPreview(
    name: String, title: String, location: String, education: String,
    skills: List<String>, targetRole: String,
    bullets: List<String>, keywords: List<String>,
    experience: List<ExperienceEntry> = emptyList(),
    professionalSummary: String = "",
    onEditSection: (String) -> Unit,
) {
    val black = Color(0xFF1A1A1A)
    val darkGray = Color(0xFF444444)
    val midGray = Color(0xFF777777)

    Column(modifier = Modifier.padding(24.dp)) {
        // Name & Contact
        Column(modifier = Modifier.fillMaxWidth().clickable { onEditSection("contact") }) {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = black)

            val parts = listOfNotNull(title.ifBlank { null }, location.ifBlank { null })
            if (parts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(parts.joinToString("  |  "), fontSize = 9.sp, color = midGray)
            }

            // Thin divider
            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFCCCCCC))
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Professional Summary
        if (professionalSummary.isNotBlank()) {
            Column(modifier = Modifier.fillMaxWidth().clickable { onEditSection("summary") }) {
                Text("Summary", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(professionalSummary, fontSize = 9.sp, color = darkGray, lineHeight = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Skills
        if (skills.isNotEmpty() || keywords.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().clickable { onEditSection("skills") }) {
                val allSkills = (skills + keywords).distinct()
                Text("Skills", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(allSkills.joinToString(", "), fontSize = 9.sp, color = darkGray)
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Experience
        if (experience.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().clickable { onEditSection("experience") }) {
                Text("Experience", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = black)
                Spacer(modifier = Modifier.height(4.dp))
                experience.forEach { entry ->
                    Text(entry.jobTitle, fontSize = 10.sp, color = darkGray, fontStyle = FontStyle.Italic)
                    val sub = listOfNotNull(entry.company.ifBlank { null }, entry.dates.ifBlank { null })
                    if (sub.isNotEmpty()) {
                        Text(sub.joinToString("  |  "), fontSize = 8.sp, color = midGray)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    entry.bullets.forEach { bullet ->
                        Row(modifier = Modifier.padding(start = 8.dp, bottom = 3.dp)) {
                            Text("—  ", fontSize = 9.sp, color = midGray)
                            Text(bullet, fontSize = 9.sp, color = darkGray, lineHeight = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        } else if (bullets.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().clickable { onEditSection("experience") }) {
                Text("Experience", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = black)
                Spacer(modifier = Modifier.height(4.dp))
                val role = targetRole.ifBlank { title }
                if (role.isNotBlank()) {
                    Text(role, fontSize = 10.sp, color = darkGray, fontStyle = FontStyle.Italic)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                bullets.forEach { bullet ->
                    Row(modifier = Modifier.padding(start = 8.dp, bottom = 3.dp)) {
                        Text("—  ", fontSize = 9.sp, color = midGray)
                        Text(bullet, fontSize = 9.sp, color = darkGray, lineHeight = 13.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
        }

        // Education
        if (education.isNotBlank()) {
            Column(modifier = Modifier.fillMaxWidth().clickable { onEditSection("education") }) {
                Text("Education", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(education, fontSize = 9.sp, color = darkGray)
            }
        }
    }
}

// ── Shared components ──────────────────────────────────────────────────────

@Composable
private fun SectionHeader(text: String, color: Color) {
    Column {
        Text(
            text,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = color,
            letterSpacing = 1.sp,
        )
        Spacer(modifier = Modifier.height(3.dp))
        HorizontalDivider(thickness = 1.dp, color = color.copy(alpha = 0.4f))
    }
}

@Composable
private fun BulletPoint(text: String, textColor: Color, bulletColor: Color) {
    Row(modifier = Modifier.padding(start = 8.dp, bottom = 3.dp), verticalAlignment = Alignment.Top) {
        Text("•", fontSize = 9.sp, color = bulletColor, modifier = Modifier.padding(top = 1.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, fontSize = 9.sp, color = textColor, lineHeight = 14.sp)
    }
}
