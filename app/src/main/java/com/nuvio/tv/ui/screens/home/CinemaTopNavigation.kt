package com.nuvio.tv.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import androidx.tv.material3.MaterialTheme

@Composable
internal fun CinemaTopNavigation(
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xD9141418), Color(0x00141418))
                )
            )
            .padding(horizontal = 42.dp, vertical = 22.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CinemaNavButton("Home", "home", onNavigate)
        CinemaNavButton("Movies", "library", onNavigate)
        CinemaNavButton("TV Shows", "library", onNavigate)
        CinemaNavButton("Search", "search", onNavigate)
        CinemaNavButton("My List", "library", onNavigate)
        CinemaNavButton("Settings", "settings", onNavigate)
    }
}

@Composable
private fun CinemaNavButton(
    label: String,
    route: String,
    onNavigate: (String) -> Unit
) {
    Button(
        onClick = { onNavigate(route) },
        shape = ButtonDefaults.shape(shape = RoundedCornerShape(20.dp)),
        colors = ButtonDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black
        )
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
