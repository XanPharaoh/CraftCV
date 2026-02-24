package com.craftcv.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.craftcv.app.data.models.HistoryItem
import com.craftcv.app.ui.theme.CraftColors
import com.craftcv.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
) {
    val history by viewModel.history.collectAsState()

    LaunchedEffect(Unit) { viewModel.fetchHistory() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CraftColors.Surface,
                    titleContentColor = CraftColors.InkPrimary,
                ),
            )
        },
        containerColor = CraftColors.Background,
    ) { padding ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No tailoring history yet.\nResults will appear here after you tailor a resume.",
                    color = CraftColors.InkSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(history, key = { it.sessionId }) { item ->
                    HistoryCard(item)
                }
            }
        }
    }
}

@Composable
private fun HistoryCard(item: HistoryItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CraftColors.Surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title / role hint
            Text(
                text = item.jobTitleHint.ifBlank { "Untitled Role" },
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = CraftColors.InkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(4.dp))

            // Resume snippet
            if (item.resumeSnippet.isNotBlank()) {
                Text(
                    text = item.resumeSnippet,
                    fontSize = 13.sp,
                    color = CraftColors.InkSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
            }

            // Date
            Text(
                text = formatDate(item.createdAt),
                fontSize = 12.sp,
                color = CraftColors.InkTertiary,
            )
        }
    }
}

/** Simple date formatter — strips the time portion if present. */
private fun formatDate(raw: String): String {
    if (raw.isBlank()) return ""
    // Backend sends ISO format e.g. "2025-01-15T14:30:00"
    return raw.substringBefore("T").ifBlank { raw }
}
