package com.resumetailor.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resumetailor.app.data.models.TailorResponse
import com.resumetailor.app.ui.components.ProGateCard
import com.resumetailor.app.ui.components.CraftAccentButton
import com.resumetailor.app.ui.components.CraftOutlineButton
import com.resumetailor.app.ui.theme.CraftColors
import com.resumetailor.app.ui.theme.InterFamily

@Composable
fun InsightsTab(
    tailorData: TailorResponse,
    isPro: Boolean,
    onUpgrade: () -> Unit,
    onReanalyze: () -> Unit,
    isReanalyzing: Boolean = false,
    reanalyzeError: String? = null,
    isAdReady: Boolean = false,
    isAdLoading: Boolean = false,
    onWatchAdToReanalyze: () -> Unit = {},
) {
    val matchScore = tailorData.atsScore
    val roleStrength = when {
        matchScore >= 75 -> "Strong match"
        matchScore >= 55 -> "Good match"
        matchScore >= 35 -> "Partial match"
        else             -> "Weak match"
    }
    val roleColor = when {
        matchScore >= 75 -> CraftColors.Success
        matchScore >= 55 -> CraftColors.Warning
        else             -> CraftColors.Error
    }
    val roleSoftColor = when {
        matchScore >= 75 -> CraftColors.SuccessSoft
        matchScore >= 55 -> CraftColors.WarningSoft
        else             -> CraftColors.ErrorSoft
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

        Text("Role insights", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = CraftColors.InkPrimary)
        Text("Based on what this role typically requires and how your profile compares.", fontFamily = InterFamily, fontSize = 13.sp, color = CraftColors.InkSecondary)

        // ── Match likelihood ──
        Surface(shape = RoundedCornerShape(12.dp), color = roleSoftColor, border = BorderStroke(1.dp, roleColor.copy(alpha = 0.3f))) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(when { matchScore >= 75 -> "🎯"; matchScore >= 55 -> "📈"; else -> "⚠️" }, fontSize = 20.sp)
                    Column {
                        Text(roleStrength, fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = roleColor)
                        Text("Based on keyword alignment and profile fit", fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkSecondary)
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Profile match", fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkSecondary)
                        Text("$matchScore%", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = roleColor)
                    }
                    // Uses androidx.compose.ui.draw.clip — NOT a custom extension
                    LinearProgressIndicator(
                        progress   = { matchScore / 100f },
                        modifier   = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color      = roleColor,
                        trackColor = roleColor.copy(alpha = 0.15f),
                    )
                }
            }
        }

        // ── What this role typically requires (Pro only) ──
        if (isPro) {
            Surface(shape = RoundedCornerShape(10.dp), color = CraftColors.Surface, border = BorderStroke(1.dp, CraftColors.Border)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("What this role typically needs", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = CraftColors.InkPrimary)
                    val requirements = tailorData.roleRequirements.ifEmpty {
                        listOf("No requirements data — try re-tailoring.")
                    }
                    requirements.forEach { req ->
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.padding(top = 6.dp).size(5.dp).background(CraftColors.Border, RoundedCornerShape(3.dp)))
                            Text(req, fontFamily = InterFamily, fontSize = 13.sp, color = CraftColors.InkSecondary, lineHeight = 19.sp)
                        }
                    }
                }
            }

            // ── Related roles (Pro only) ──
            Surface(shape = RoundedCornerShape(10.dp), color = CraftColors.Surface, border = BorderStroke(1.dp, CraftColors.Border)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Roles you'd also qualify for", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = CraftColors.InkPrimary)
                    Text("Based on your skills and experience.", fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkTertiary)
                    val roles = tailorData.suggestedRoles.ifEmpty {
                        listOf("Re-tailor your resume to see role suggestions.")
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        roles.forEach { role ->
                            Surface(shape = RoundedCornerShape(8.dp), color = CraftColors.SurfaceVariant, border = BorderStroke(1.dp, CraftColors.Border)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(role, fontFamily = InterFamily, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = CraftColors.InkPrimary, modifier = Modifier.weight(1f))
                                    Text("→", color = CraftColors.InkTertiary, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (reanalyzeError != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CraftColors.ErrorSoft,
                    border = BorderStroke(1.dp, CraftColors.Error.copy(alpha = 0.3f)),
                ) {
                    Text(
                        reanalyzeError,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.Error,
                    )
                }
            }

            CraftAccentButton(
                text = if (isReanalyzing) "Analyzing…" else "🔄 Re-Analyze with AI",
                onClick = { if (!isReanalyzing) onReanalyze() },
                modifier = Modifier.fillMaxWidth(),
            )
            if (isReanalyzing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(color = CraftColors.Accent, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sending your edits to the AI for a full rescore…", fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkTertiary)
                }
            } else {
                Text(
                    text = "Scores update instantly as you edit. Use Re-Analyze for a full AI deep-dive after major changes.",
                    fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }
        } else {
            // ── Free user: show blurred teaser + gate ──
            ProGateCard(message = "Upgrade to Pro to unlock detailed role requirements, related roles, and AI Re-analysis", onUpgrade = onUpgrade)

            Spacer(modifier = Modifier.height(8.dp))

            // Watch ad to Re-Analyze (free users)
            if (reanalyzeError != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CraftColors.ErrorSoft,
                    border = BorderStroke(1.dp, CraftColors.Error.copy(alpha = 0.3f)),
                ) {
                    Text(
                        reanalyzeError,
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
                        Text("AI Re-Analyzing…", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = CraftColors.Accent)
                    }
                }
            } else {
                CraftOutlineButton(
                    text = if (isAdLoading) "Loading ad…" else if (isAdReady) "🎬 Watch Ad to Re-Analyze" else "Ad unavailable — try again later",
                    onClick = { if (isAdReady && !isAdLoading) onWatchAdToReanalyze() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Watch a short ad to use AI Re-Analysis for free.",
                    fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkTertiary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
