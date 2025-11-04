package com.twinmind.voicerecorder.presentation.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.twinmind.voicerecorder.domain.model.SessionSummary
import com.twinmind.voicerecorder.domain.model.SummaryStatus
import com.twinmind.voicerecorder.presentation.viewmodel.SummaryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    sessionId: String,
    onNavigateBack: () -> Unit,
    viewModel: SummaryViewModel = hiltViewModel()
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(sessionId) {
        viewModel.loadSummary(sessionId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Session Summary") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    LoadingState()
                }
                errorMessage != null -> {
                    ErrorState(
                        errorMessage = errorMessage!!,
                        onRetry = { viewModel.generateSummary(sessionId) }
                    )
                }
                summary != null -> {
                    SummaryContent(
                        summary = summary!!,
                        onRegenerateClick = { viewModel.generateSummary(sessionId) }
                    )
                }
                else -> {
                    NoSummaryState(
                        onGenerateClick = { viewModel.generateSummary(sessionId) }
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "🧠 Generating summary...",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "This may take a few moments",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ErrorState(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "❌",
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Summary Generation Failed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
fun NoSummaryState(
    onGenerateClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "📝",
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "No Summary Yet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Generate an AI-powered summary of your recording session.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onGenerateClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🧠 Generate Summary")
                }
            }
        }
    }
}

@Composable
fun SummaryContent(
    summary: SessionSummary,
    onRegenerateClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status indicator
        item {
            SummaryStatusCard(summary = summary)
        }

        // Title Section
        item {
            SummarySection(
                title = "📋 TITLE",
                content = summary.title,
                isTitle = true
            )
        }

        // Summary Section
        item {
            SummarySection(
                title = "📝 SUMMARY", 
                content = summary.summary
            )
        }

        // Action Items Section
        item {
            SummarySection(
                title = "✅ ACTION ITEMS",
                content = summary.actionItems
            )
        }

        // Key Points Section
        item {
            SummarySection(
                title = "🔑 KEY POINTS",
                content = summary.keyPoints
            )
        }

        // Regenerate button
        item {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRegenerateClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Regenerate Summary")
            }
        }
    }
}

@Composable
fun SummaryStatusCard(summary: SessionSummary) {
    Card(
        colors = when (summary.status) {
            SummaryStatus.GENERATING -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            SummaryStatus.COMPLETED -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            SummaryStatus.FAILED -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (icon, text, color) = when (summary.status) {
                SummaryStatus.GENERATING -> Triple("🔄", "Generating...", MaterialTheme.colorScheme.onPrimaryContainer)
                SummaryStatus.COMPLETED -> Triple("✅", "Summary Complete", MaterialTheme.colorScheme.onSecondaryContainer)
                SummaryStatus.FAILED -> Triple("❌", "Generation Failed", MaterialTheme.colorScheme.onErrorContainer)
            }
            
            Text(
                text = icon,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleSmall,
                color = color
            )
            
            if (summary.status == SummaryStatus.GENERATING) {
                Spacer(modifier = Modifier.width(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = color
                )
            }
        }
    }
}

@Composable
fun SummarySection(
    title: String,
    content: String,
    isTitle: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content.ifBlank { "Not generated yet..." },
                style = if (isTitle) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                fontWeight = if (isTitle) FontWeight.Medium else FontWeight.Normal,
                color = if (content.isBlank()) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}