package com.nuvio.tv.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import com.nuvio.tv.ui.theme.NuvioTheme

private val CinemaNavigationShape = RoundedCornerShape(26.dp)

@Composable
internal fun CinemaTopNavigation(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xE60B0C10), Color(0xB30B0C10), Color.Transparent)
                )
            )
            .padding(horizontal = 38.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(CinemaNavigationShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.White.copy(alpha = 0.14f), Color.Black.copy(alpha = 0.34f))
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.25f), CinemaNavigationShape)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CinemaNavButton("Home", "home", onNavigate, selected = true)
            CinemaNavButton("Movies", "library", onNavigate)
            CinemaNavButton("TV Shows", "library", onNavigate)
            CinemaNavButton("Search", "search", onNavigate)
            CinemaNavButton("My List", "library", onNavigate)
            Box(modifier = Modifier.weight(1f))
            CinemaNavButton("Settings", "settings", onNavigate)
        }
    }
}

@Composable
private fun CinemaNavButton(
    label: String,
    route: String,
    onNavigate: (String) -> Unit,
    selected: Boolean = false
) {
    var focused by remember { mutableStateOf(false) }
    val containerColor by animateColorAsState(
        targetValue = when {
            focused -> Color.White.copy(alpha = 0.92f)
            selected -> NuvioTheme.colors.Secondary.copy(alpha = 0.35f)
            else -> Color.Transparent
        },
        animationSpec = tween(160),
        label = "cinemaNavButton"
    )
    val contentColor = if (focused) Color.Black else Color.White

    Card(
        onClick = { onNavigate(route) },
        modifier = Modifier
            .height(42.dp)
            .onFocusChanged { focused = it.hasFocus },
        colors = CardDefaults.colors(
            containerColor = containerColor,
            focusedContainerColor = containerColor
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(21.dp))
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = contentColor,
                style = androidx.tv.material3.MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (selected || focused) FontWeight.SemiBold else FontWeight.Medium
                ),
                maxLines = 1
            )
        }
    }
}
