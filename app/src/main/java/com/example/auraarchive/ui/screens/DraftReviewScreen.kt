package com.example.auraarchive.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.auraarchive.ui.viewModel.DraftViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftReviewScreen(
    postId: String,
    onBack: () -> Unit,
    viewModel: DraftViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(postId) {
        viewModel.loadDraft(postId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Draft", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                actions = {
                    if (uiState.post != null) {
                        IconButton(onClick = {
                            if (uiState.isEditing) {
                                // Now using values directly from ViewModel state
                                viewModel.saveDraftUpdates()
                            } else {
                                viewModel.toggleEditMode()
                            }
                        }) {
                            Icon(
                                if (uiState.isEditing) Icons.Default.Done else Icons.Default.Edit,
                                contentDescription = null,
                                tint = colorScheme.primary
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.post != null && !uiState.isEditing) {
                Surface(tonalElevation = 8.dp) {
                    Button(
                        onClick = { viewModel.publishDraft() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                        enabled = !uiState.isPublishing
                    ) {
                        if (uiState.isPublishing) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Send, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Publish to Feed", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading && uiState.post == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Show a mini progress bar if AI is still processing in the background
                if (uiState.post?.status == "PROCESSING") {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        "AI is still crafting the content...",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.secondary
                    )
                }

                Crossfade(targetState = uiState.isEditing, label = "EditToggle") { editing ->
                    if (editing) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = uiState.editedTitle,
                                onValueChange = { viewModel.onTextFieldChange(title = it) },
                                label = { Text("Headline") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = uiState.editedSummary,
                                onValueChange = { viewModel.onTextFieldChange(summary = it) },
                                label = { Text("Aura Summary") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )
                            OutlinedTextField(
                                value = uiState.editedContent,
                                onValueChange = { viewModel.onTextFieldChange(content = it) },
                                label = { Text("Markdown Body") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 10
                            )
                        }
                    } else {
                        Column {
                            Text(
                                text = uiState.editedTitle.ifBlank { "Generating Title..." },
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = uiState.editedSummary.ifBlank { "Summarizing aura..." },
                                style = MaterialTheme.typography.bodyLarge,
                                color = colorScheme.secondary
                            )
                            HorizontalDivider(Modifier.padding(vertical = 16.dp))
                            Text(
                                text = uiState.editedContent.ifBlank { "Drafting content..." },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.wasPublishedSuccessfully) {
        LaunchedEffect(Unit) { onBack() }
    }
}