package com.nuvio.tv.ui.screens.search

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.nuvio.tv.ui.screens.home.HomeScreen
import com.nuvio.tv.ui.screens.home.HomeViewModel

/** Cinema's Movies and TV Shows roots use the same catalog/presentation pipeline as Home. */
@Composable
fun CinemaBrowseScreen(
    contentType: String,
    onNavigateToDetail: (String, String, String) -> Unit,
    cinemaMode: Boolean = true,
    viewModel: HomeViewModel = hiltViewModel()
) {
    HomeScreen(
        viewModel = viewModel,
        cinemaMode = cinemaMode,
        cinemaContentType = contentType,
        onNavigateToDetail = onNavigateToDetail
    )
}
