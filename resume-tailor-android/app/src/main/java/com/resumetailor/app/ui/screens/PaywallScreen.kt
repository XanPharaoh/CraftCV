package com.resumetailor.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.resumetailor.app.ui.components.CraftButton
import com.resumetailor.app.ui.components.CraftOutlineButton
import com.resumetailor.app.ui.theme.CraftColors
import com.resumetailor.app.ui.theme.InterFamily

private data class PlanOption(val label: String, val price: String, val period: String, val badge: String = "")

private val features = listOf(
    Triple("✦", "Unlimited tailoring", "No monthly cap — tailor as many resumes as you need."),
    Triple("📝", "Cover letters", "Generate complete, job-specific cover letters with subject line."),
    Triple("🎯", "Full insights", "Detailed role fit analysis, related roles, and AI Re-analysis."),
    Triple("🔍", "All missing keywords", "See every keyword the ATS is looking for."),
    Triple("✏️", "Bullet rewriter", "Rewrite any bullet in a different tone on demand."),
    Triple("📄", "DOCX export", "Download your tailored resume as a Word document."),
    Triple("🚫", "Ad-free experience", "No banner ads — a clean, distraction-free workflow."),
)

@Composable
fun PaywallScreen(
    onUpgrade: (planIndex: Int) -> Unit,
    onBack: () -> Unit,
    isPurchasePending: Boolean = false,
    billingError: String? = null,
    onDismissBillingError: () -> Unit = {},
) {
    var selectedPlan by remember { mutableIntStateOf(1) } // 0 = monthly, 1 = yearly

    val plans = listOf(
        PlanOption("Monthly", "$2.99", "/month"),
        PlanOption("Yearly", "$1.79", "/month", "Best value — save 40%"),
    )

    Column(
        modifier = Modifier.fillMaxSize().background(CraftColors.Background).verticalScroll(rememberScrollState()),
    ) {
        // ── Top bar ──
        Row(
            modifier              = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                Text("← Back", fontFamily = InterFamily, fontSize = 14.sp, color = CraftColors.InkSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("✦", fontSize = 16.sp, color = CraftColors.Accent)
                Text("CraftCV", fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CraftColors.InkPrimary)
            }
        }

        // ── Hero ──
        Column(modifier = Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier         = Modifier.size(72.dp).background(CraftColors.ProGoldSoft, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) { Text("★", fontSize = 32.sp, color = CraftColors.ProGold) }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Upgrade to Pro", fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = CraftColors.InkPrimary, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Everything you need to land the job — unlimited, no caps, no compromises.", fontFamily = InterFamily, fontSize = 14.sp, color = CraftColors.InkSecondary, textAlign = TextAlign.Center, lineHeight = 21.sp)
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Features ──
        Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            features.forEach { (icon, title, desc) ->
                Surface(shape = RoundedCornerShape(10.dp), color = CraftColors.Surface, border = BorderStroke(1.dp, CraftColors.Border)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            modifier         = Modifier.size(38.dp).background(CraftColors.ProGoldSoft, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Text(icon, fontSize = 18.sp) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = CraftColors.InkPrimary)
                            Text(desc, fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkSecondary, lineHeight = 17.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ── Plan selector ──
        Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("CHOOSE A PLAN", fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.sp, color = CraftColors.InkTertiary)
            plans.forEachIndexed { i, plan ->
                val isSelected = selectedPlan == i
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(10.dp),
                    color    = if (isSelected) CraftColors.InkPrimary else CraftColors.Surface,
                    border   = BorderStroke(if (isSelected) 0.dp else 1.dp, CraftColors.Border),
                    onClick  = { selectedPlan = i },
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        RadioButton(
                            selected = isSelected,
                            onClick  = { selectedPlan = i },
                            colors   = RadioButtonDefaults.colors(
                                selectedColor   = if (isSelected) Color.White else CraftColors.Accent,
                                unselectedColor = CraftColors.Border,
                            ),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(plan.label, fontFamily = InterFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = if (isSelected) Color.White else CraftColors.InkPrimary)
                            if (plan.badge.isNotEmpty()) {
                                Text(plan.badge, fontFamily = InterFamily, fontSize = 11.sp, color = if (isSelected) CraftColors.ProGold else CraftColors.InkSecondary)
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(plan.price, fontFamily = InterFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = if (isSelected) Color.White else CraftColors.InkPrimary)
                            Text(plan.period, fontFamily = InterFamily, fontSize = 11.sp, color = if (isSelected) Color.White.copy(alpha = 0.6f) else CraftColors.InkTertiary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── CTA ──
        Column(modifier = Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (billingError != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFEEEE),
                    border = BorderStroke(1.dp, Color(0xFFCC3333).copy(alpha = 0.3f)),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(billingError, fontFamily = InterFamily, fontSize = 12.sp, color = Color(0xFFCC3333), modifier = Modifier.weight(1f))
                        TextButton(onClick = onDismissBillingError, contentPadding = PaddingValues(0.dp)) {
                            Text("Dismiss", fontFamily = InterFamily, fontSize = 12.sp, color = Color(0xFFCC3333))
                        }
                    }
                }
            }

            CraftButton(
                text = if (isPurchasePending) "Opening Google Play…" else "Start Pro — ${plans[selectedPlan].price}${plans[selectedPlan].period}",
                onClick = { if (!isPurchasePending) onUpgrade(selectedPlan) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (isPurchasePending) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(color = CraftColors.Accent, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connecting to Google Play…", fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkTertiary)
                }
            }
            CraftOutlineButton(text = "Maybe later", onClick = onBack, modifier = Modifier.fillMaxWidth())
            Text("Cancel anytime. Billed by Google Play. No hidden fees.", fontFamily = InterFamily, fontSize = 12.sp, color = CraftColors.InkTertiary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}
