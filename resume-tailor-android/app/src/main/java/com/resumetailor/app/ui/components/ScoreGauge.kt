package com.resumetailor.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.resumetailor.app.ui.theme.CraftColors
import com.resumetailor.app.ui.theme.InterFamily

@Composable
fun ScoreGauge(
    score: Int,
    modifier: Modifier = Modifier
) {
    val scoreColor = when {
        score >= 75 -> CraftColors.Success
        score >= 50 -> CraftColors.Warning
        else -> CraftColors.Error
    }

    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(durationMillis = 1000),
        label = "score_animation"
    )

    val trackColor = CraftColors.Border

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(160.dp)
    ) {
        val isCompact = maxWidth < 120.dp
        val strokeDp = if (isCompact) 7.dp else 14.dp
        val innerPadding = if (isCompact) 5.dp else 12.dp
        val scoreFontSize = if (isCompact) 20.sp else 36.sp
        val labelFontSize = if (isCompact) 7.sp else 12.sp

        Canvas(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val strokeWidth = strokeDp.toPx()

            // Background track
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Score arc
            drawArc(
                color = scoreColor,
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                fontSize = scoreFontSize,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Bold,
                color = scoreColor
            )
            Text(
                text = if (isCompact) "ATS" else "ATS Score",
                fontSize = labelFontSize,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                color = CraftColors.InkSecondary
            )
        }
    }
}

