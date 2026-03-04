package com.craftcv.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcv.app.ui.theme.CraftColors
import com.craftcv.app.ui.theme.InterFamily
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val body: String,
)

private val pages = listOf(
    OnboardingPage(
        icon = Icons.Outlined.Description,
        title = "AI-Powered Resume Tailoring",
        body = "Paste your resume and a job description — our AI tailors your resume for maximum impact.",
    ),
    OnboardingPage(
        icon = Icons.Outlined.Analytics,
        title = "Beat the ATS",
        body = "Get an ATS compatibility score and detailed insights to optimize your resume for applicant tracking systems.",
    ),
    OnboardingPage(
        icon = Icons.Outlined.Mail,
        title = "Cover Letters in Seconds",
        body = "Generate tailored cover letters and download your polished resume as a DOCX file.",
    ),
    OnboardingPage(
        icon = Icons.Outlined.RocketLaunch,
        title = "Ready to Land Your Dream Job?",
        body = "Set up your profile and start tailoring your resume today.",
    ),
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CraftColors.Background),
    ) {
        // Skip button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            AnimatedVisibility(visible = !isLastPage) {
                TextButton(onClick = onComplete) {
                    Text(
                        "Skip",
                        fontFamily = InterFamily,
                        fontSize = 14.sp,
                        color = CraftColors.InkTertiary,
                    )
                }
            }
        }

        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            val item = pages[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Icon
                Surface(
                    shape = CircleShape,
                    color = CraftColors.AccentSoft,
                    modifier = Modifier.size(100.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = CraftColors.Accent,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))

                Text(
                    text = item.title,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = CraftColors.InkPrimary,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = item.body,
                    fontFamily = InterFamily,
                    fontSize = 15.sp,
                    color = CraftColors.InkSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                )
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 24.dp),
            ) {
                repeat(pages.size) { index ->
                    Box(
                        modifier = Modifier
                            .size(if (index == pagerState.currentPage) 10.dp else 8.dp)
                            .background(
                                color = if (index == pagerState.currentPage) CraftColors.Accent else CraftColors.Border,
                                shape = CircleShape,
                            ),
                    )
                }
            }

            // Next / Get Started button
            Button(
                onClick = {
                    if (isLastPage) {
                        onComplete()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CraftColors.Accent,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = if (isLastPage) "Get Started" else "Next",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}
