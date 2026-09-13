package com.nuvio.tv.ui.screens.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nuvio.tv.ui.screens.home.CinemaTopNavigation

@Composable
fun CinemaBrowseScreen(
    contentType: String,
    title: String,
    selectedRoute: String,
    onNavigateToRoute: (String) -> Unit,
    onNavigateToDetail: (String, String, String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        DiscoverScreen(
            showBuiltInHeader = true,
            headerTitle = title,
            initialContentType = contentType,
            lockContentType = true,
            forceDiscoverEnabled = true,
            // Cinema browse owns a compact, collapsible filter chrome. The regular Discover
            // route keeps its existing header and controls.
            cinemaBrowse = true,
            contentTopPadding = 96.dp,
            onNavigateToDetail = onNavigateToDetail
        )
        CinemaTopNavigation(
            selectedRoute = selectedRoute,
            onNavigate = onNavigateToRoute
        )
    }
}
