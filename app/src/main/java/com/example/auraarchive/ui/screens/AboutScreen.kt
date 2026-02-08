package com.example.auraarchive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen() {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(16.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(colorScheme.primary, colorScheme.primaryContainer)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Aura Archive",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onPrimary
                )
                Text(
                    text = "AI-Powered Documentation",
                    style = MaterialTheme.typography.labelLarge,
                    color = colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Collaborators",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary
            )

            Text(
                text = "Nirvan Jain & Dhairya Pandya",
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurface
            )

            HorizontalDivider(color = colorScheme.outlineVariant)

            Text(
                text = "Tech Stack",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary
            )

            TechSection("Languages", "Kotlin, Python", colorScheme)
            TechSection("Mobile Frameworks", "Jetpack Compose, Navigation3, Hilt", colorScheme)
            TechSection("Backend & Infrastructure", "Ktor, QDrant, Cloudinary", colorScheme)
            TechSection("Artificial Intelligence", "Google AI SDK (Gemini)", colorScheme)

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "© 2026 Aura Archive",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.outline,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Composable
fun TechSection(title: String, description: String, colorScheme: ColorScheme) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.secondary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant
        )
    }
}