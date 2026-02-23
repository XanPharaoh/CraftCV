package com.resumetailor.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.BorderStroke
import com.resumetailor.app.data.models.UserProfile
import com.resumetailor.app.ui.components.*
import com.resumetailor.app.ui.theme.CraftColors
import com.resumetailor.app.ui.theme.InterFamily

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onProfileReady: (UserProfile) -> Unit,
    onSkipToDashboard: () -> Unit,
) {
    var fullName         by remember { mutableStateOf("") }
    var currentTitle     by remember { mutableStateOf("") }
    var location         by remember { mutableStateOf("") }
    var yearsExp         by remember { mutableStateOf("") }
    var education        by remember { mutableStateOf("") }
    var targetRole       by remember { mutableStateOf("") }
    var skillInput       by remember { mutableStateOf("") }
    var skills           by remember { mutableStateOf(listOf<String>()) }
    var uploadedFileName by remember { mutableStateOf("") }
    var uploadedUri      by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            uploadedUri      = it.toString()
            uploadedFileName = it.lastPathSegment ?: "resume.pdf"
        }
    }

    val isFormValid = fullName.isNotBlank() && targetRole.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CraftColors.Background)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Top bar ──
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✦", fontSize = 20.sp, color = CraftColors.Accent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("CraftCV", fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CraftColors.InkPrimary)
            }
            TextButton(onClick = onSkipToDashboard, modifier = Modifier.align(Alignment.CenterEnd)) {
                Text("Skip", fontFamily = InterFamily, fontSize = 14.sp, color = CraftColors.InkTertiary)
            }
        }

        // ── Hero ──
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text("Let's build your profile.", fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp, color = CraftColors.InkPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Takes 2 minutes. Makes every tailored resume better.", fontFamily = InterFamily, fontSize = 14.sp, color = CraftColors.InkSecondary)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── PDF Upload ──
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            CraftCard {
                if (uploadedFileName.isEmpty()) {
                    Row(
                        modifier              = Modifier.fillMaxWidth().clickable { filePicker.launch("application/pdf") },
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(
                            modifier         = Modifier.size(44.dp).background(CraftColors.AccentSoft, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Text("📄", fontSize = 20.sp) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Upload your resume", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = CraftColors.InkPrimary)
                            Text("PDF — we'll fill everything in for you", fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkTertiary)
                        }
                        Text("→", color = CraftColors.Accent, fontSize = 18.sp)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatusPill("Uploaded", CraftColors.SuccessSoft, CraftColors.Success, "✓")
                        Text(uploadedFileName, fontFamily = InterFamily, fontSize = 13.sp, color = CraftColors.InkSecondary, modifier = Modifier.weight(1f))
                        TextButton(onClick = { uploadedFileName = ""; uploadedUri = "" }) {
                            Text("Remove", fontSize = 12.sp, color = CraftColors.InkTertiary)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LabeledDivider("or fill in manually")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Section 01: About you ──
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            SectionHeader("SECTION 01", "About you", "Basic info that anchors every resume we generate.")
            Spacer(modifier = Modifier.height(16.dp))
            CraftCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CraftTextField(value = fullName, onValueChange = { fullName = it }, placeholder = "Jane Smith", label = "Full name", maxLines = 1)
                    CraftTextField(value = currentTitle, onValueChange = { currentTitle = it }, placeholder = "Senior Product Designer", label = "Current or most recent title", maxLines = 1)
                    CraftTextField(value = location, onValueChange = { location = it }, placeholder = "New York, NY", label = "Location", maxLines = 1)
                    CraftTextField(value = yearsExp, onValueChange = { yearsExp = it.filter { c -> c.isDigit() } }, placeholder = "5", label = "Years experience", maxLines = 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Section 02: Skills ──
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            SectionHeader("SECTION 02", "Your skills", "Add the ones you'd actually put on a resume.")
            Spacer(modifier = Modifier.height(16.dp))
            CraftCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    CraftTextField(value = skillInput, onValueChange = { skillInput = it }, placeholder = "e.g. Python, Figma, Project Management", label = "Type a skill and press Add")
                    CraftOutlineButton(
                        text     = "+ Add skill",
                        onClick  = {
                            if (skillInput.isNotBlank() && skills.size < 20) {
                                skills     = skills + skillInput.trim()
                                skillInput = ""
                            }
                        },
                        modifier = Modifier.wrapContentWidth(),
                    )
                    if (skills.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement   = Arrangement.spacedBy(8.dp),
                        ) {
                            skills.forEach { skill ->
                                CraftChip(text = skill, onRemove = { skills = skills - skill }, color = CraftColors.AccentSoft, textColor = CraftColors.Accent)
                            }
                        }
                    }
                    Text("Popular: Communication · SQL · Excel · Leadership · Agile", fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkTertiary)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Section 03: Education & Goal ──
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            SectionHeader("SECTION 03", "Education & goal", "Where you've studied and where you're headed.")
            Spacer(modifier = Modifier.height(16.dp))
            CraftCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CraftTextField(value = education, onValueChange = { education = it }, placeholder = "B.Sc. Computer Science — MIT, 2019", label = "Highest education", minLines = 1, maxLines = 2)
                    CraftTextField(value = targetRole, onValueChange = { targetRole = it }, placeholder = "Software Engineer, Product Manager...", label = "Target role  *", maxLines = 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ── CTA ──
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            CraftButton(
                text    = "Build my profile",
                onClick = {
                    onProfileReady(UserProfile(
                        fullName        = fullName,
                        currentTitle    = currentTitle,
                        location        = location,
                        yearsExperience = yearsExp,
                        skills          = skills,
                        education       = education,
                        targetRole      = targetRole,
                        resumeUriString = uploadedUri,
                    ))
                },
                enabled  = isFormValid || uploadedFileName.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text("You can always update this later.", fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkTertiary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
