package com.example.auraarchive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.auraarchive.ui.components.AuraCard
import com.example.auraarchive.ui.viewModel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onCardClick: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsState()
    val fridayLabels = listOf("Last Friday", "Friday, Jan 30", "Friday, Jan 23")

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = {
            viewModel.loadFeed()
            viewModel.fetchInspiration()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                HeroHeader(
                    colorScheme = colorScheme,
                    quote = uiState.quote,
                    author = uiState.author
                )
            }

            if (uiState.posts.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyState(colorScheme)
                }
            } else {
                val displayPosts = uiState.posts.take(3)
                itemsIndexed(displayPosts) { index, post ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        SectionHeader(fridayLabels.getOrElse(index) { "Previous Friday" })
                        Spacer(modifier = Modifier.height(12.dp))
                        AuraCard(
                            post = post,
                            onClick = { onCardClick(post.id) }
                        )
                    }
                }

                if (uiState.posts.size > 3) {
                    item {
                        SectionHeader("Earlier Archives")
                    }
                    itemsIndexed(uiState.posts.drop(3)) { _, post ->
                        AuraCard(
                            post = post,
                            onClick = { onCardClick(post.id) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeroHeader(
    colorScheme: ColorScheme,
    quote: String = "Loading inspiration...",
    author: String = ""
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(colorScheme.primary, colorScheme.primaryContainer)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
        ) {
            Text(
                text = "Your Archive",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "\"$quote\"",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = colorScheme.onPrimary.copy(alpha = 0.9f)
            )

            if (author.isNotEmpty()) {
                Text(
                    text = "— $author",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onPrimary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = colorScheme.outlineVariant
        )
    }
}

@Composable
fun EmptyState(colorScheme: ColorScheme) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No Archives Found",
            style = MaterialTheme.typography.bodyLarge,
            color = colorScheme.onSurfaceVariant
        )
    }
}