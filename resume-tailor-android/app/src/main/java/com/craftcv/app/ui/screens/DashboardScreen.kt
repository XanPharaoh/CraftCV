package com.craftcv.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcv.app.data.models.UserProfile
import com.craftcv.app.ui.components.*
import com.craftcv.app.ui.theme.CraftColors
import com.craftcv.app.ui.theme.InterFamily
import com.craftcv.app.viewmodel.CoverLetterUiState
import com.craftcv.app.viewmodel.MainViewModel
import com.craftcv.app.viewmodel.UiState

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    profile: UserProfile?,
    isPro: Boolean,
    usesRemaining: Int,
    onResultsReady: () -> Unit,
    onPaywallRequired: () -> Unit,
    onEditProfile: () -> Unit,
    onHistory: () -> Unit = {},
) {
    var jobDescription by remember { mutableStateOf("") }
    var resumeFileName by remember { mutableStateOf("") }
    var resumeUri      by remember { mutableStateOf<Uri?>(null) }

    val clipboard  = LocalClipboardManager.current
    val context    = LocalContext.current
    val uiState    by viewModel.uiState.collectAsState()
    val quickCoverState by viewModel.quickCoverLetterState.collectAsState()

    // Restore saved inputs
    LaunchedEffect(Unit) {
        if (viewModel.lastJobDesc.isNotBlank() && jobDescription.isBlank()) {
            jobDescription = viewModel.lastJobDesc
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            resumeUri      = it
            resumeFileName = it.lastPathSegment ?: "resume"
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is UiState.Success -> onResultsReady()
            is UiState.Error -> {
                if (state.isUsageLimit) onPaywallRequired()
            }
            else -> {}
        }
    }

    val isLoading = uiState is UiState.Loading
    val isGeneratingLetter = quickCoverState is CoverLetterUiState.Loading

    // Quick cover letter dialog
    if (quickCoverState is CoverLetterUiState.Success || quickCoverState is CoverLetterUiState.Error) {
        AlertDialog(
            onDismissRequest = { viewModel.resetQuickCoverLetter() },
            title = {
                Text(
                    if (quickCoverState is CoverLetterUiState.Success) "Your Cover Letter" else "Error",
                    fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                )
            },
            text = {
                when (val state = quickCoverState) {
                    is CoverLetterUiState.Success -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (state.data.subjectLine.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = CraftColors.AccentSoft,
                                    border = BorderStroke(1.dp, CraftColors.AccentBorder),
                                ) {
                                    Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Subject:", fontFamily = InterFamily, fontSize = 11.sp, color = CraftColors.InkTertiary, fontWeight = FontWeight.Medium)
                                        Text(state.data.subjectLine, fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.Accent, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CraftColors.Surface,
                                border = BorderStroke(1.dp, CraftColors.Border),
                            ) {
                                Text(
                                    state.data.coverLetter,
                                    modifier = Modifier.padding(14.dp),
                                    fontFamily = InterFamily,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    color = CraftColors.InkPrimary,
                                )
                            }
                        }
                    }
                    is CoverLetterUiState.Error -> {
                        Text(state.message, fontFamily = InterFamily, fontSize = 13.sp, color = CraftColors.Error)
                    }
                    else -> {}
                }
            },
            confirmButton = {
                if (quickCoverState is CoverLetterUiState.Success) {
                    TextButton(onClick = {
                        val text = (quickCoverState as CoverLetterUiState.Success).data.coverLetter
                        clipboard.setText(AnnotatedString(text))
                    }) {
                        Text("Copy", fontFamily = InterFamily, color = CraftColors.Accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resetQuickCoverLetter() }) {
                    Text("Close", fontFamily = InterFamily, color = CraftColors.InkSecondary)
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CraftColors.Background)
            .verticalScroll(rememberScrollState()),
    ) {
        // ── Header ──
        Column(
            modifier = Modifier.fillMaxWidth().background(CraftColors.Surface).padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("✦", fontSize = 18.sp, color = CraftColors.Accent)
                Spacer(modifier = Modifier.width(6.dp))
                Text("CraftCV", fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = CraftColors.InkPrimary)
                Spacer(modifier = Modifier.weight(1f))
                if (isPro) {
                    TextButton(onClick = onHistory) {
                        Text("History", fontFamily = InterFamily, fontSize = 13.sp, color = CraftColors.InkSecondary)
                    }
                }
                if (isPro) {
                    StatusPill("Pro", CraftColors.ProGoldSoft, CraftColors.ProGold, "★")
                } else {
                    StatusPill(
                        text      = "$usesRemaining left",
                        color     = if (usesRemaining > 0) CraftColors.SurfaceVariant else CraftColors.ErrorSoft,
                        textColor = if (usesRemaining > 0) CraftColors.InkSecondary else CraftColors.Error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Profile snapshot
            if (profile != null && profile.fullName.isNotBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    color    = CraftColors.SurfaceVariant,
                    border   = BorderStroke(1.dp, CraftColors.Border),
                ) {
                    Row(
                        modifier              = Modifier.padding(14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier         = Modifier.size(40.dp).background(CraftColors.AccentSoft, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(profile.fullName.first().uppercase(), fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = CraftColors.Accent)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(profile.fullName, fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = CraftColors.InkPrimary)
                            Text(profile.currentTitle.ifBlank { profile.targetRole }, fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkSecondary)
                        }
                        TextButton(onClick = onEditProfile) {
                            Text("Edit", fontFamily = InterFamily, fontSize = 13.sp, color = CraftColors.InkTertiary)
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onEditProfile),
                    shape    = RoundedCornerShape(10.dp),
                    color    = CraftColors.AccentSoft,
                    border   = BorderStroke(1.dp, CraftColors.AccentBorder),
                ) {
                    Row(
                        modifier              = Modifier.padding(14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("💡", fontSize = 16.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Add your profile for better results", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CraftColors.Accent)
                            Text("Takes 2 min — skip anytime", fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkSecondary)
                        }
                        Text("→", color = CraftColors.Accent, fontSize = 16.sp)
                    }
                }
            }
        }

        HorizontalDivider(color = CraftColors.Border)

        Column(
            modifier              = Modifier.padding(horizontal = 24.dp),
            verticalArrangement   = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Tailor your resume.", fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = CraftColors.InkPrimary)
                Text("Paste a job description and we'll do the rest.", fontFamily = InterFamily, fontSize = 14.sp, color = CraftColors.InkSecondary)
            }

            // ── Resume source ──
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("RESUME SOURCE", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.sp, color = CraftColors.InkTertiary)

                if (profile != null && profile.fullName.isNotBlank()) {
                    SourceOptionCard(
                        icon     = "👤",
                        title    = "Use my profile",
                        subtitle = "${profile.fullName} · ${profile.skills.take(3).joinToString(", ")}",
                        selected = resumeUri == null && resumeFileName.isEmpty(),
                        onClick  = { resumeUri = null; resumeFileName = "" },
                    )
                }

                SourceOptionCard(
                    icon     = "📄",
                    title    = if (resumeFileName.isEmpty()) "Upload a PDF or DOCX" else resumeFileName,
                    subtitle = if (resumeFileName.isEmpty()) "Override profile with a specific resume" else "Tap to change file",
                    selected = resumeFileName.isNotEmpty(),
                    onClick  = {
                        filePicker.launch(
                            arrayOf(
                                "application/pdf",
                                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                "text/plain",
                            )
                        )
                    },
                )
            }

            // ── Job description ──
            CraftTextField(
                value         = jobDescription,
                onValueChange = { jobDescription = it },
                placeholder   = "Paste the full job description here...",
                label         = "JOB DESCRIPTION",
                minLines      = 6,
                maxLines      = 14,
            )

            // ── Tailor button ──
            val canSubmit = jobDescription.length > 50 && (profile != null || resumeFileName.isNotEmpty())
            var validationMsg by remember { mutableStateOf("") }

            CraftButton(
                text      = "Tailor my resume",
                onClick   = {
                    if (jobDescription.length <= 50) {
                        validationMsg = "Job description is too short. Paste the full listing."
                        return@CraftButton
                    }
                    validationMsg = ""
                    val useFile = resumeUri != null
                    val profileText = if (!useFile) {
                        profile?.let {
                            buildString {
                                append("Name: ${it.fullName}\n")
                                append("Title: ${it.currentTitle}\n")
                                if (it.location.isNotBlank()) append("Location: ${it.location}\n")
                                append("Experience: ${it.yearsExperience} years\n")
                                append("Skills: ${it.skills.joinToString(", ")}\n")
                                append("Education: ${it.education}\n")
                                append("Target Role: ${it.targetRole}")
                            }
                        } ?: ""
                    } else ""
                    viewModel.tailorResume(profileText, jobDescription, if (useFile) resumeUri else null)
                },
                enabled   = canSubmit,
                isLoading = isLoading,
                modifier  = Modifier.fillMaxWidth(),
                icon      = "✦",
            )

            // Quick cover letter button
            CraftOutlineButton(
                text = if (isGeneratingLetter) "Writing cover letter…" else "Write Cover Letter",
                onClick = {
                    if (jobDescription.length <= 50) {
                        validationMsg = "Job description is too short. Paste the full listing."
                        return@CraftOutlineButton
                    }
                    validationMsg = ""
                    val useFile = resumeUri != null
                    val profileText = if (!useFile) {
                        profile?.let {
                            buildString {
                                append("Name: ${it.fullName}\n")
                                append("Title: ${it.currentTitle}\n")
                                if (it.location.isNotBlank()) append("Location: ${it.location}\n")
                                append("Experience: ${it.yearsExperience} years\n")
                                append("Skills: ${it.skills.joinToString(", ")}\n")
                                append("Education: ${it.education}\n")
                                append("Target Role: ${it.targetRole}")
                            }
                        } ?: ""
                    } else ""
                    viewModel.generateQuickCoverLetter(profileText, jobDescription)
                },
                enabled = canSubmit && !isLoading && !isGeneratingLetter,
                modifier = Modifier.fillMaxWidth(),
            )

            if (validationMsg.isNotBlank()) {
                Text(validationMsg, fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.Error, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            if (!canSubmit && jobDescription.isNotBlank()) {
                Text("Add a resume source above to continue.", fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkTertiary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }

            if (uiState is UiState.Error && !(uiState as UiState.Error).isUsageLimit) {
                val msg = (uiState as UiState.Error).message
                Surface(shape = RoundedCornerShape(8.dp), color = CraftColors.ErrorSoft, border = BorderStroke(1.dp, CraftColors.Error.copy(alpha = 0.3f))) {
                    Text(msg, modifier = Modifier.padding(12.dp), fontSize = 13.sp, color = CraftColors.Error, fontFamily = InterFamily)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SourceOptionCard(icon: String, title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape    = RoundedCornerShape(10.dp),
        color    = if (selected) CraftColors.AccentSoft else CraftColors.Surface,
        border   = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) CraftColors.Accent else CraftColors.Border),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier         = Modifier.size(38.dp).background(if (selected) CraftColors.Accent else CraftColors.SurfaceVariant, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(icon, fontSize = 18.sp) }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = if (selected) CraftColors.Accent else CraftColors.InkPrimary)
                Text(subtitle, fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (selected) Text("✓", color = CraftColors.Accent, fontWeight = FontWeight.Bold)
        }
    }
}
