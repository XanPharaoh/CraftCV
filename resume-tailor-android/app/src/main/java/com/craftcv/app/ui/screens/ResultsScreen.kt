package com.craftcv.app.ui.screens

import android.app.Activity
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcv.app.data.models.TailorResponse
import com.craftcv.app.data.models.ExperienceEntry
import com.craftcv.app.data.models.UserProfile
import com.craftcv.app.ads.AdManager
import com.craftcv.app.ui.components.*
import com.craftcv.app.ui.theme.CraftColors
import com.craftcv.app.ui.theme.InterFamily
import com.craftcv.app.viewmodel.CoverLetterUiState
import com.craftcv.app.viewmodel.MainViewModel
import com.craftcv.app.viewmodel.RewriteUiState

@Composable
fun ResultsScreen(
    tailorData: TailorResponse,
    viewModel: MainViewModel,
    resumeText: String,
    jobDescription: String,
    isPro: Boolean,
    onUpgradeClick: () -> Unit,
    onBack: () -> Unit,
    activity: Activity? = null,
) {
    val clipboard         = LocalClipboardManager.current
    var selectedTab       by remember { mutableIntStateOf(0) }
    var copiedBulletIndex by remember { mutableIntStateOf(-1) }
    var editingSection    by remember { mutableStateOf<String?>(null) }
    val coverLetterState  by viewModel.coverLetterState.collectAsState()
    val rewriteState      by viewModel.rewriteState.collectAsState()
    val userProfile       by viewModel.userProfile.collectAsState()
    val draftResume       by viewModel.draftResume.collectAsState()
    val isReanalyzing     by viewModel.isReanalyzing.collectAsState()
    val reanalyzeError    by viewModel.reanalyzeError.collectAsState()
    val isAdReady         by AdManager.isAdReady.collectAsState()
    val isAdLoading       by AdManager.isAdLoading.collectAsState()
    val tabs              = listOf("Preview", "Bullets", "Keywords", "Letter", "Insights")
    var selectedTemplate  by remember { mutableStateOf("professional") }
    
    // The active source of truth (draft overrides original API response)
    val activeData = draftResume ?: tailorData

    // Individual bullet selection
    val selectedBullets = remember { mutableStateMapOf<Int, Boolean>() }
    LaunchedEffect(tailorData.tailoredBullets) {
        tailorData.tailoredBullets.indices.forEach { i ->
            if (!selectedBullets.containsKey(i)) selectedBullets[i] = true
        }
    }
    val selectedBulletCount = selectedBullets.count { it.value }

    // Edited bullet text tracking (manual edits override AI-generated text)
    val editedBullets = remember { mutableStateMapOf<Int, String>() }
    var editingBulletIndex by remember { mutableIntStateOf(-1) }
    var editingText by remember { mutableStateOf("") }
    var aiSuggestionForBullet by remember { mutableIntStateOf(-1) }

    // Effective bullets = user-edited or original
    val effectiveBullets = tailorData.tailoredBullets.mapIndexed { i, original -> editedBullets[i] ?: original }
    val selectedBulletTexts = effectiveBullets.filterIndexed { i, _ -> selectedBullets[i] == true }

    // Individual keyword selection
    val selectedKeywords = remember { mutableStateMapOf<Int, Boolean>() }
    LaunchedEffect(activeData.missingKeywords) {
        selectedKeywords.clear()
        activeData.missingKeywords.indices.forEach { i ->
             selectedKeywords[i] = true
        }
    }
    val selectedKeywordTexts = activeData.missingKeywords.filterIndexed { i, _ -> selectedKeywords[i] == true }

    // Track if cover letter needs refresh
    var coverLetterStale by remember { mutableStateOf(false) }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 3 && isPro && coverLetterState is CoverLetterUiState.Idle) {
            viewModel.generateCoverLetter(resumeText, jobDescription)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(CraftColors.Background)) {

        // ── Top bar ──
        Column(modifier = Modifier.fillMaxWidth().background(CraftColors.Surface).padding(horizontal = 24.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                    Text("← Back", fontFamily = InterFamily, fontSize = 14.sp, color = CraftColors.InkSecondary)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("✦", fontSize = 16.sp, color = CraftColors.Accent)
                Spacer(modifier = Modifier.width(4.dp))
                Text("CraftCV", fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CraftColors.InkPrimary)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ATS Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScoreGauge(score = activeData.atsScore, modifier = Modifier.size(80.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        when {
                            activeData.atsScore >= 75 -> "Strong match"
                            activeData.atsScore >= 55 -> "Good match"
                            activeData.atsScore >= 35 -> "Partial match"
                            else -> "Needs work"
                        },
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = CraftColors.InkPrimary,
                    )
                    if (activeData.atsReason.isNotBlank()) {
                        Text(
                            activeData.atsReason,
                            fontFamily = InterFamily,
                            fontSize = 12.sp,
                            color = CraftColors.InkSecondary,
                            lineHeight = 16.sp,
                            maxLines = 2,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatusPill(
                            text = "$selectedBulletCount bullets",
                            color = CraftColors.AccentSoft,
                            textColor = CraftColors.Accent,
                        )
                        StatusPill(
                            text = "${selectedKeywordTexts.size} keywords",
                            color = CraftColors.SuccessSoft,
                            textColor = CraftColors.Success,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab row
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor   = CraftColors.Surface,
                contentColor     = CraftColors.Accent,
                edgePadding      = 0.dp,
                divider = {},
            ) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected               = selectedTab == i,
                        onClick                = { selectedTab = i },
                        selectedContentColor   = CraftColors.Accent,
                        unselectedContentColor = CraftColors.InkSecondary,
                        text = {
                            Text(
                                text       = title,
                                fontFamily = InterFamily,
                                fontWeight = if (selectedTab == i) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize   = 13.sp,
                                color      = if (selectedTab == i) CraftColors.Accent else CraftColors.InkSecondary,
                            )
                        },
                    )
                }
            }
        }

        HorizontalDivider(color = CraftColors.Border)

        // ── Tab content ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (selectedTab) {

                // ── PREVIEW ──
                0 -> {
                    Text("Resume preview", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = CraftColors.InkPrimary)
                    Text(
                        "This is how your tailored resume will look. Edit bullets and keywords in the other tabs — changes appear here in real time.",
                        fontFamily = InterFamily, fontSize = 13.sp, color = CraftColors.InkSecondary, lineHeight = 19.sp,
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Template picker
                    Text("TEMPLATE", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.sp, color = CraftColors.InkTertiary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf(
                            Triple("Professional", "professional", "📋"),
                            Triple("Modern", "modern", "✨"),
                            Triple("Minimal", "minimal", "📝"),
                        ).forEach { (label, key, icon) ->
                            val sel = selectedTemplate == key
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (sel) CraftColors.AccentSoft else CraftColors.Surface,
                                border = BorderStroke(1.5.dp, if (sel) CraftColors.Accent else CraftColors.Border),
                                modifier = Modifier.weight(1f).clickable { selectedTemplate = key },
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(icon, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        label,
                                        fontFamily = InterFamily,
                                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                        color = if (sel) CraftColors.Accent else CraftColors.InkSecondary,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // The actual live preview
                    ResumePreviewCard(
                        profile = userProfile,
                        selectedBullets = selectedBulletTexts,
                        selectedKeywords = selectedKeywordTexts,
                        template = selectedTemplate,
                        experience = activeData.experience,
                        professionalSummary = activeData.professionalSummary,
                        contactInfo = activeData.contactInfo,
                        educationEntries = activeData.education,
                        activeSkills = activeData.skills,
                        onEditSection = { editingSection = it }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Improvement suggestions
                    ImprovementSuggestions(
                        atsScore = activeData.atsScore,
                        bulletCount = selectedBulletCount,
                        totalBullets = tailorData.tailoredBullets.size,
                        missingKeywordCount = activeData.missingKeywords.size - selectedKeywordTexts.size,
                        hasProfile = (activeData.contactInfo.fullName.isNotBlank()) || (userProfile?.fullName?.isNotBlank() == true),
                        hasSkills = activeData.skills.isNotEmpty() || (userProfile?.skills?.isNotEmpty() == true),
                        hasEducation = activeData.education.isNotEmpty() || (userProfile?.education?.isNotBlank() == true),
                        hasContactInfo = activeData.contactInfo.fullName.isNotBlank() || activeData.contactInfo.email.isNotBlank(),
                        resumeSkills = activeData.skills,
                        missingKeywords = activeData.missingKeywords,
                        onGoToBullets = { selectedTab = 1 },
                        onGoToKeywords = { selectedTab = 2 },
                        onEditSection = { editingSection = it },
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Re-Analyze button + local-scoring note
                    if (reanalyzeError != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CraftColors.ErrorSoft,
                            border = BorderStroke(1.dp, CraftColors.Error.copy(alpha = 0.3f)),
                        ) {
                            Text(
                                "Re-analyze failed: $reanalyzeError",
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.Error,
                            )
                        }
                    }

                    if (isReanalyzing) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = CraftColors.AccentSoft,
                            border = BorderStroke(1.dp, CraftColors.AccentBorder),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator(color = CraftColors.Accent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("AI Re-Analyzing…", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = CraftColors.Accent)
                                    Text("Your edits are being sent for a full AI rescore. This takes a few seconds.", fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkSecondary)
                                }
                            }
                        }
                    } else {
                        if (isPro) {
                            CraftOutlineButton(
                                text = "🔄 Re-Analyze with AI",
                                onClick = { viewModel.reanalyzeDraft(jobDescription); viewModel.clearReanalyzeError() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            CraftOutlineButton(
                                text = if (isAdLoading) "Loading ad…" else if (isAdReady) "🎬 Watch Ad to Re-Analyze" else "🔄 Tap to Load Ad",
                                onClick = {
                                    if (isAdReady && !isAdLoading) {
                                        activity?.let { act ->
                                            AdManager.showRewardedAd(act,
                                                onRewarded = {
                                                    viewModel.reanalyzeDraft(jobDescription)
                                                    viewModel.clearReanalyzeError()
                                                },
                                            )
                                        }
                                    } else if (!isAdLoading) {
                                        AdManager.retryLoad()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        Text(
                            if (isPro) "Score updates instantly as you edit. Re-Analyze sends your changes to the AI for a full deep-dive rescore."
                            else if (isAdReady) "Watch a short ad to use AI Re-Analysis for free."
                            else if (isAdLoading) "Loading ad, please wait…"
                            else "Tap the button to retry loading an ad.",
                            fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkTertiary, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    if (isPro) {
                        CraftAccentButton(
                            text = "⬇ Download Resume ($selectedBulletCount bullets)",
                            onClick = { viewModel.downloadDocx(tailorData.copy(tailoredBullets = selectedBulletTexts), selectedTemplate) },
                            modifier = Modifier.fillMaxWidth(),
                            icon = "",
                        )
                    } else {
                        ProGateCard(message = "Download your tailored resume as a polished DOCX file", onUpgrade = onUpgradeClick)
                    }

                    // Copy all
                    CraftOutlineButton(
                        text = "Copy all selected bullets",
                        onClick = { clipboard.setText(AnnotatedString(selectedBulletTexts.joinToString("\n\n") { "• $it" })) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // ── BULLETS ──
                1 -> {
                    // ATS keyword coverage banner
                    val allBulletText = effectiveBullets.joinToString(" ").lowercase()
                    val coveredKeywords = tailorData.missingKeywords.filter { allBulletText.contains(it.lowercase()) }
                    val totalTargetKeywords = tailorData.missingKeywords.size

                    if (totalTargetKeywords > 0) {
                        val coverageGood = coveredKeywords.size >= totalTargetKeywords / 2
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (coverageGood) CraftColors.SuccessSoft else CraftColors.WarningSoft,
                            border = BorderStroke(1.dp, if (coverageGood) CraftColors.Success.copy(alpha = 0.3f) else CraftColors.Warning.copy(alpha = 0.3f)),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text("🎯", fontSize = 18.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "ATS Keyword Coverage",
                                        fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                                        color = CraftColors.InkPrimary,
                                    )
                                    Text(
                                        "${coveredKeywords.size} of $totalTargetKeywords target keywords addressed in your bullets",
                                        fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkSecondary,
                                    )
                                }
                            }
                        }
                    }

                    // Selection banner
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CraftColors.AccentSoft,
                        border = BorderStroke(1.dp, CraftColors.AccentBorder),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "$selectedBulletCount of ${tailorData.tailoredBullets.size} selected",
                                fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                                color = CraftColors.Accent, modifier = Modifier.weight(1f),
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CraftColors.Surface,
                                modifier = Modifier.clickable {
                                    val allSelected = selectedBullets.all { it.value }
                                    tailorData.tailoredBullets.indices.forEach { i -> selectedBullets[i] = !allSelected }
                                    coverLetterStale = true
                                },
                            ) {
                                Text(
                                    if (selectedBullets.all { it.value }) "Deselect all" else "Select all",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    fontFamily = InterFamily, fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium, color = CraftColors.InkSecondary,
                                )
                            }
                        }
                    }

                    Text(
                        "Tap Edit to customize any bullet. Keyword matches update in real time so you can see the ATS impact.",
                        fontFamily = InterFamily, fontSize = 13.sp, color = CraftColors.InkSecondary,
                    )

                    // Grouped experience display (if available), else flat
                    if (tailorData.experience.isNotEmpty()) {
                        var globalIdx = 0
                        tailorData.experience.forEach { entry ->
                            // Role header
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CraftColors.SurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        entry.jobTitle,
                                        fontFamily = InterFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = CraftColors.InkPrimary,
                                    )
                                    val meta = listOfNotNull(
                                        entry.company.ifBlank { null },
                                        entry.dates.ifBlank { null },
                                    )
                                    if (meta.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            meta.joinToString("  ·  "),
                                            fontFamily = InterFamily,
                                            fontSize = 12.sp,
                                            color = CraftColors.InkTertiary,
                                        )
                                    }
                                }
                            }

                            entry.bullets.forEachIndexed { _, _ ->
                                val index = globalIdx
                                val bullet = effectiveBullets.getOrElse(index) { "" }
                                if (bullet.isNotBlank()) {
                                    BulletCard(
                                        index = index,
                                        bullet = bullet,
                                        isSelected = selectedBullets[index] == true,
                                        isEditing = editingBulletIndex == index,
                                        editingText = editingText,
                                        copiedIndex = copiedBulletIndex,
                                        missingKeywords = tailorData.missingKeywords,
                                        isPro = isPro,
                                        rewriteState = rewriteState,
                                        aiSuggestionForBullet = aiSuggestionForBullet,
                                        editedBullets = editedBullets,
                                        onSelectionChange = { selectedBullets[index] = it; coverLetterStale = true },
                                        onCopy = { clipboard.setText(AnnotatedString(bullet)); copiedBulletIndex = index },
                                        onStartEdit = { editingBulletIndex = index; editingText = bullet; aiSuggestionForBullet = -1; viewModel.resetRewriteState() },
                                        onEditTextChange = { editingText = it },
                                        onSave = {
                                            if (editingText.isNotBlank()) { editedBullets[index] = editingText.trim(); coverLetterStale = true }
                                            editingBulletIndex = -1; aiSuggestionForBullet = -1; viewModel.resetRewriteState()
                                        },
                                        onCancel = { editingBulletIndex = -1; aiSuggestionForBullet = -1; viewModel.resetRewriteState() },
                                        onSuggest = { aiSuggestionForBullet = index; viewModel.rewriteBullet(editingText.ifBlank { bullet }, jobDescription) },
                                        onApplySuggestion = { text -> editingText = text; aiSuggestionForBullet = -1; viewModel.resetRewriteState() },
                                        onDismissSuggestion = { aiSuggestionForBullet = -1; viewModel.resetRewriteState() },
                                        onReset = { editedBullets.remove(index); coverLetterStale = true },
                                    )
                                }
                                globalIdx++
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    } else {
                        // Flat display (backward compat)
                        effectiveBullets.forEachIndexed { index, bullet ->
                            BulletCard(
                                index = index,
                                bullet = bullet,
                                isSelected = selectedBullets[index] == true,
                                isEditing = editingBulletIndex == index,
                                editingText = editingText,
                                copiedIndex = copiedBulletIndex,
                                missingKeywords = tailorData.missingKeywords,
                                isPro = isPro,
                                rewriteState = rewriteState,
                                aiSuggestionForBullet = aiSuggestionForBullet,
                                editedBullets = editedBullets,
                                onSelectionChange = { selectedBullets[index] = it; coverLetterStale = true },
                                onCopy = { clipboard.setText(AnnotatedString(bullet)); copiedBulletIndex = index },
                                onStartEdit = { editingBulletIndex = index; editingText = bullet; aiSuggestionForBullet = -1; viewModel.resetRewriteState() },
                                onEditTextChange = { editingText = it },
                                onSave = {
                                    if (editingText.isNotBlank()) { editedBullets[index] = editingText.trim(); coverLetterStale = true }
                                    editingBulletIndex = -1; aiSuggestionForBullet = -1; viewModel.resetRewriteState()
                                },
                                onCancel = { editingBulletIndex = -1; aiSuggestionForBullet = -1; viewModel.resetRewriteState() },
                                onSuggest = { aiSuggestionForBullet = index; viewModel.rewriteBullet(editingText.ifBlank { bullet }, jobDescription) },
                                onApplySuggestion = { text -> editingText = text; aiSuggestionForBullet = -1; viewModel.resetRewriteState() },
                                onDismissSuggestion = { aiSuggestionForBullet = -1; viewModel.resetRewriteState() },
                                onReset = { editedBullets.remove(index); coverLetterStale = true },
                            )
                        }
                    }

                    if (selectedBulletTexts.isNotEmpty()) {
                        CraftOutlineButton(
                            text = "Copy ${selectedBulletTexts.size} selected bullets",
                            onClick = { clipboard.setText(AnnotatedString(selectedBulletTexts.joinToString("\n\n") { "• $it" })) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // ── KEYWORDS ──
                2 -> {
                    Text("Missing keywords", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = CraftColors.InkPrimary)
                    Text("Tap to select keywords to weave into your resume.", fontFamily = InterFamily, fontSize = 13.sp, color = CraftColors.InkSecondary)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tailorData.missingKeywords.forEachIndexed { index, keyword ->
                            val isSelected = selectedKeywords[index] == true
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) CraftColors.AccentSoft else CraftColors.Surface,
                                border = BorderStroke(1.dp, if (isSelected) CraftColors.Accent else CraftColors.Border),
                                modifier = Modifier.clickable { selectedKeywords[index] = !isSelected },
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { selectedKeywords[index] = it },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = CraftColors.Accent,
                                            uncheckedColor = CraftColors.Border,
                                            checkmarkColor = Color.White,
                                        ),
                                        modifier = Modifier.size(22.dp),
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(keyword, fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, color = if (isSelected) CraftColors.Accent else CraftColors.InkPrimary, modifier = Modifier.weight(1f))
                                    if (!isSelected) {
                                        Surface(shape = RoundedCornerShape(4.dp), color = CraftColors.ErrorSoft) {
                                            Text("Missing", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontFamily = InterFamily, fontSize = 11.sp, color = CraftColors.Error)
                                        }
                                    } else {
                                        Surface(shape = RoundedCornerShape(4.dp), color = CraftColors.SuccessSoft) {
                                            Text("Will add", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), fontFamily = InterFamily, fontSize = 11.sp, color = CraftColors.Success)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (selectedKeywordTexts.isNotEmpty()) {
                        CraftOutlineButton(
                            text = "Copy ${selectedKeywordTexts.size} selected keywords",
                            onClick = { clipboard.setText(AnnotatedString(selectedKeywordTexts.joinToString(", "))) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // ── COVER LETTER ──
                3 -> {
                    if (!isPro) {
                        Text("Cover letter", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = CraftColors.InkPrimary)
                        Text("Generate a tailored cover letter that matches the job description and highlights your strengths.", fontFamily = InterFamily, fontSize = 13.sp, color = CraftColors.InkSecondary, lineHeight = 19.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        ProGateCard(message = "Upgrade to Pro to generate complete, job-specific cover letters with email subject line", onUpgrade = onUpgradeClick)
                    } else {
                    // Stale banner — when user changed bullets
                    if (coverLetterStale && coverLetterState is CoverLetterUiState.Success) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CraftColors.WarningSoft,
                            border = BorderStroke(1.dp, CraftColors.Warning.copy(alpha = 0.3f)),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text("⚠️", fontSize = 16.sp)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Bullets changed", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CraftColors.InkPrimary)
                                    Text("Regenerate to reflect your updated selections.", fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkSecondary)
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = CraftColors.InkPrimary,
                                    modifier = Modifier.clickable {
                                        viewModel.resetCoverLetter()
                                        // Build an updated resume text that includes selected bullets
                                        val updatedResume = buildString {
                                            append(resumeText)
                                            append("\n\nSelected bullets:\n")
                                            selectedBulletTexts.forEach { append("• $it\n") }
                                        }
                                        viewModel.generateCoverLetter(updatedResume, jobDescription)
                                        coverLetterStale = false
                                    },
                                ) {
                                    Text("Refresh", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    when (val state = coverLetterState) {
                        is CoverLetterUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    CircularProgressIndicator(color = CraftColors.Accent, strokeWidth = 2.dp)
                                    Text("Writing your cover letter...", fontFamily = InterFamily, fontSize = 13.sp, color = CraftColors.InkTertiary)
                                }
                            }
                        }
                        is CoverLetterUiState.Success -> {
                            Text("Cover letter", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = CraftColors.InkPrimary)
                            if (state.data.subjectLine.isNotBlank()) {
                                Surface(shape = RoundedCornerShape(8.dp), color = CraftColors.AccentSoft, border = BorderStroke(1.dp, CraftColors.AccentBorder)) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text("Subject:", fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkTertiary, fontWeight = FontWeight.Medium)
                                        Text(state.data.subjectLine, fontFamily = InterFamily, fontSize = 13.sp, color = CraftColors.Accent, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                            Surface(shape = RoundedCornerShape(10.dp), color = CraftColors.Surface, border = BorderStroke(1.dp, CraftColors.Border)) {
                                Text(state.data.coverLetter, modifier = Modifier.padding(18.dp), fontFamily = InterFamily, fontSize = 14.sp, lineHeight = 22.sp, color = CraftColors.InkPrimary)
                            }
                            CraftOutlineButton(text = "Copy cover letter", onClick = { clipboard.setText(AnnotatedString(state.data.coverLetter)) }, modifier = Modifier.fillMaxWidth())
                        }
                        is CoverLetterUiState.Error -> {
                            Surface(shape = RoundedCornerShape(8.dp), color = CraftColors.ErrorSoft, border = BorderStroke(1.dp, CraftColors.Error.copy(alpha = 0.3f))) {
                                Text("Error: ${state.message}", modifier = Modifier.padding(14.dp), fontSize = 13.sp, color = CraftColors.Error, fontFamily = InterFamily)
                            }
                        }
                        else -> {}
                    }
                    } // end isPro else
                }

                // ── INSIGHTS ──
                4 -> InsightsTab(
                    tailorData = activeData,
                    isPro = isPro,
                    onUpgrade = onUpgradeClick,
                    onReanalyze = { viewModel.reanalyzeDraft(jobDescription); viewModel.clearReanalyzeError() },
                    isReanalyzing = isReanalyzing,
                    reanalyzeError = reanalyzeError,
                    isAdReady = isAdReady,
                    isAdLoading = isAdLoading,
                    onWatchAdToReanalyze = {
                        activity?.let { act ->
                            AdManager.showRewardedAd(act,
                                onRewarded = {
                                    viewModel.reanalyzeDraft(jobDescription)
                                    viewModel.clearReanalyzeError()
                                },
                            )
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (editingSection != null) {
        ResumeEditorBottomSheet(
            section = editingSection!!,
            draftData = activeData,
            onDismiss = { editingSection = null },
            onSave = { updated ->
                viewModel.updateDraftResume(updated)
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BulletCard — reusable editable bullet card with ATS keyword tracking
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BulletCard(
    index: Int,
    bullet: String,
    isSelected: Boolean,
    isEditing: Boolean,
    editingText: String,
    copiedIndex: Int,
    missingKeywords: List<String>,
    isPro: Boolean,
    rewriteState: RewriteUiState,
    aiSuggestionForBullet: Int,
    editedBullets: Map<Int, String>,
    onSelectionChange: (Boolean) -> Unit,
    onCopy: () -> Unit,
    onStartEdit: () -> Unit,
    onEditTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onSuggest: () -> Unit,
    onApplySuggestion: (String) -> Unit,
    onDismissSuggestion: () -> Unit,
    onReset: () -> Unit,
) {
    val bulletKeywords = missingKeywords.filter { bullet.lowercase().contains(it.lowercase()) }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) CraftColors.Surface else CraftColors.SurfaceVariant,
        border = BorderStroke(1.dp, if (isEditing) CraftColors.Accent else if (isSelected) CraftColors.AccentBorder else CraftColors.Border),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectionChange,
                    colors = CheckboxDefaults.colors(checkedColor = CraftColors.Accent, uncheckedColor = CraftColors.Border, checkmarkColor = Color.White),
                    modifier = Modifier.size(22.dp),
                )
                Spacer(modifier = Modifier.width(10.dp))

                if (isEditing) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = editingText,
                            onValueChange = onEditTextChange,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFamily, fontSize = 14.sp, lineHeight = 21.sp, color = CraftColors.InkPrimary),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CraftColors.Accent, unfocusedBorderColor = CraftColors.Border, cursorColor = CraftColors.Accent),
                            shape = RoundedCornerShape(8.dp),
                            minLines = 2,
                        )

                        val editKeywords = missingKeywords.filter { editingText.lowercase().contains(it.lowercase()) }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (editKeywords.isNotEmpty()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                editKeywords.take(6).forEach { kw ->
                                    Surface(shape = RoundedCornerShape(4.dp), color = CraftColors.SuccessSoft) {
                                        Text(kw, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontFamily = InterFamily, fontSize = 10.sp, color = CraftColors.Success)
                                    }
                                }
                                if (editKeywords.size > 6) Text("+${editKeywords.size - 6}", fontFamily = InterFamily, fontSize = 10.sp, color = CraftColors.InkTertiary, modifier = Modifier.padding(start = 2.dp, top = 2.dp))
                            }
                            Text("${editKeywords.size} target keyword${if (editKeywords.size != 1) "s" else ""} — improves ATS match", fontFamily = InterFamily, fontSize = 11.sp, color = CraftColors.Success, modifier = Modifier.padding(top = 2.dp))
                        } else {
                            Text("No target keywords detected — try weaving in terms from the Keywords tab", fontFamily = InterFamily, fontSize = 11.sp, color = CraftColors.InkTertiary)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = RoundedCornerShape(6.dp), color = CraftColors.Accent, modifier = Modifier.clickable(onClick = onSave)) {
                                Text("Save", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontFamily = InterFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                            Surface(shape = RoundedCornerShape(6.dp), color = CraftColors.SurfaceVariant, modifier = Modifier.clickable(onClick = onCancel)) {
                                Text("Cancel", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontFamily = InterFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = CraftColors.InkSecondary)
                            }
                            if (isPro) {
                                Surface(shape = RoundedCornerShape(6.dp), color = CraftColors.AccentSoft, modifier = Modifier.clickable(onClick = onSuggest)) {
                                    Text("✨ Suggest", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), fontFamily = InterFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = CraftColors.Accent)
                                }
                            }
                        }

                        if (aiSuggestionForBullet == index) {
                            when (val state = rewriteState) {
                                is RewriteUiState.Loading -> {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CircularProgressIndicator(color = CraftColors.Accent, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                        Text("Getting suggestion…", fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkTertiary)
                                    }
                                }
                                is RewriteUiState.Success -> {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Surface(shape = RoundedCornerShape(8.dp), color = CraftColors.AccentSoft, border = BorderStroke(1.dp, CraftColors.AccentBorder)) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("AI Suggestion:", fontFamily = InterFamily, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = CraftColors.Accent)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(state.rewritten, fontFamily = InterFamily, fontSize = 13.sp, color = CraftColors.InkPrimary, lineHeight = 19.sp)
                                            val sugKeywords = missingKeywords.filter { state.rewritten.lowercase().contains(it.lowercase()) }
                                            if (sugKeywords.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text("${sugKeywords.size} target keyword${if (sugKeywords.size != 1) "s" else ""} in suggestion", fontFamily = InterFamily, fontSize = 10.sp, color = CraftColors.Success)
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Surface(shape = RoundedCornerShape(6.dp), color = CraftColors.Accent, modifier = Modifier.clickable { onApplySuggestion(state.rewritten) }) {
                                                    Text("Apply", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontFamily = InterFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                                }
                                                Surface(shape = RoundedCornerShape(6.dp), color = CraftColors.SurfaceVariant, modifier = Modifier.clickable(onClick = onDismissSuggestion)) {
                                                    Text("Dismiss", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontFamily = InterFamily, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = CraftColors.InkSecondary)
                                                }
                                            }
                                        }
                                    }
                                }
                                is RewriteUiState.Error -> {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Suggestion failed: ${state.message}", fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.Error)
                                }
                                else -> {}
                            }
                        }
                    }
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(bullet, fontFamily = InterFamily, fontSize = 14.sp, lineHeight = 21.sp, color = if (isSelected) CraftColors.InkPrimary else CraftColors.InkTertiary)
                        if (bulletKeywords.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                bulletKeywords.take(5).forEach { kw ->
                                    Surface(shape = RoundedCornerShape(4.dp), color = CraftColors.SuccessSoft) {
                                        Text(kw, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontFamily = InterFamily, fontSize = 10.sp, color = CraftColors.Success)
                                    }
                                }
                                if (bulletKeywords.size > 5) Text("+${bulletKeywords.size - 5}", fontFamily = InterFamily, fontSize = 10.sp, color = CraftColors.InkTertiary, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                    }
                }
            }

            if (!isEditing) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 32.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (copiedIndex == index) CraftColors.SuccessSoft else CraftColors.SurfaceVariant,
                        modifier = Modifier.clickable(onClick = onCopy),
                    ) {
                        Text(if (copiedIndex == index) "✓ Copied" else "Copy", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontFamily = InterFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (copiedIndex == index) CraftColors.Success else CraftColors.InkSecondary)
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = CraftColors.AccentSoft, modifier = Modifier.clickable(onClick = onStartEdit)) {
                        Text("Edit", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontFamily = InterFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = CraftColors.Accent)
                    }
                    if (editedBullets.containsKey(index)) {
                        Surface(shape = RoundedCornerShape(6.dp), color = CraftColors.SurfaceVariant, modifier = Modifier.clickable(onClick = onReset)) {
                            Text("Reset", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), fontFamily = InterFamily, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = CraftColors.InkTertiary)
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Improvement Suggestions — smart, context-aware tips
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ImprovementSuggestions(
    atsScore: Int,
    bulletCount: Int,
    totalBullets: Int,
    missingKeywordCount: Int,
    hasProfile: Boolean,
    hasSkills: Boolean,
    hasEducation: Boolean,
    hasContactInfo: Boolean = true,
    resumeSkills: List<String> = emptyList(),
    missingKeywords: List<String> = emptyList(),
    onGoToBullets: () -> Unit,
    onGoToKeywords: () -> Unit,
    onEditSection: (String) -> Unit = {},
) {
    // Build suggestions dynamically
    data class Suggestion(val icon: String, val title: String, val desc: String, val action: String, val onClick: () -> Unit, val priority: Int)

    val suggestions = buildList {
        // Contact info — highest priority when missing
        if (!hasContactInfo) {
            add(Suggestion(
                "📇", "Add contact information",
                "Your resume is missing contact details. Add your name, email, phone, and location so employers can reach you.",
                "Edit contact info", { onEditSection("contact") }, 1,
            ))
        }
        if (missingKeywordCount > 0) {
            add(Suggestion(
                "🔑", "Add missing keywords",
                "$missingKeywordCount keyword(s) from the job description aren't selected yet. Adding them can significantly boost your ATS score.",
                "Go to keywords", onGoToKeywords, 2,
            ))
        }
        if (atsScore < 60) {
            add(Suggestion(
                "🎯", "Strengthen ATS alignment",
                "Your ATS score is below 60. Focus on matching the exact phrases used in the job description.",
                "Review keywords", onGoToKeywords, 2,
            ))
        }
        if (bulletCount < totalBullets) {
            add(Suggestion(
                "📝", "Include more bullets",
                "You've deselected ${totalBullets - bulletCount} bullet(s). Including all gives ATS scanners more material to match.",
                "Review bullets", onGoToBullets, 3,
            ))
        }
        // Skill gap analysis — suggest adding job keywords as skills
        if (missingKeywords.isNotEmpty() && resumeSkills.isNotEmpty()) {
            val skillsLower = resumeSkills.map { it.lowercase() }
            val gapKeywords = missingKeywords.filter { kw -> skillsLower.none { it.contains(kw.lowercase()) } }
            if (gapKeywords.isNotEmpty()) {
                val preview = gapKeywords.take(3).joinToString(", ")
                add(Suggestion(
                    "🔧", "Add skills from job description",
                    "Consider adding: $preview. These keywords from the job posting could boost your ATS score.",
                    "Edit skills", { onEditSection("skills") }, 2,
                ))
            }
        } else if (missingKeywords.isNotEmpty() && resumeSkills.isEmpty()) {
            add(Suggestion(
                "🔧", "Add skills to match the job",
                "You have no skills listed. Add relevant skills from the job description to improve your ATS score.",
                "Edit skills", { onEditSection("skills") }, 2,
            ))
        }
        if (!hasProfile) {
            add(Suggestion(
                "👤", "Complete your profile",
                "Adding your name, title, and location makes the resume and DOCX export look much more professional.",
                "Edit contact info", { onEditSection("contact") }, 4,
            ))
        }
        if (!hasSkills) {
            add(Suggestion(
                "⚡", "Add your skills",
                "Skills aren't showing on your resume preview. Add them to strengthen your ATS match.",
                "Edit skills", { onEditSection("skills") }, 4,
            ))
        }
        if (!hasEducation) {
            add(Suggestion(
                "🎓", "Add your education",
                "Education is missing from your resume. Employers often look for this section.",
                "Add education", { onEditSection("education") }, 4,
            ))
        }
        if (atsScore >= 75 && bulletCount == totalBullets && missingKeywordCount == 0) {
            add(Suggestion(
                "🏆", "Looking great!",
                "Your resume is well-optimized for this role. Download the DOCX and apply with confidence.",
                "", {}, 10,
            ))
        }
    }.sortedBy { it.priority }

    if (suggestions.isNotEmpty()) {
        Text(
            "SUGGESTIONS",
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            color = CraftColors.InkTertiary,
        )
        Spacer(modifier = Modifier.height(4.dp))

        suggestions.forEach { suggestion ->
            val isSuccess = suggestion.icon == "🏆"
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSuccess) CraftColors.SuccessSoft else CraftColors.Surface,
                border = BorderStroke(1.dp, if (isSuccess) CraftColors.Success.copy(alpha = 0.3f) else CraftColors.Border),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(suggestion.icon, fontSize = 20.sp, modifier = Modifier.padding(top = 2.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(suggestion.title, fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = CraftColors.InkPrimary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(suggestion.desc, fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkSecondary, lineHeight = 17.sp)
                        if (suggestion.action.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = CraftColors.AccentSoft,
                                modifier = Modifier.clickable(onClick = suggestion.onClick),
                            ) {
                                Text(
                                    suggestion.action,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    fontFamily = InterFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = CraftColors.Accent,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
