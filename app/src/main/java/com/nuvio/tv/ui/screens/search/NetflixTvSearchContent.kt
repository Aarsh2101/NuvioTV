package com.nuvio.tv.ui.screens.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.MetaPreview

private val KEYBOARD_KEYS = listOf(
    "A", "B", "C", "D", "E", "F",
    "G", "H", "I", "J", "K", "L",
    "M", "N", "O", "P", "Q", "R",
    "S", "T", "U", "V", "W", "X",
    "Y", "Z", "1", "2", "3", "4",
    "5", "6", "7", "8", "9", "0"
)

private const val KEYBOARD_COLUMNS = 6

@Composable
fun NetflixTvSearchContent(
    uiState: SearchUiState,
    onQueryChanged: (String) -> Unit,
    onNavigateToDetail: (String, String, String) -> Unit,
    onItemFocus: ((String, String) -> Unit)? = null,
    onEnsureDiscoverLoaded: () -> Unit = {},
    cinemaTopNavFocusRequester: FocusRequester? = null,
    contentFocusRequester: FocusRequester? = null,
    onNavigateBackToHome: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onEnsureDiscoverLoaded()
    }

    val query = uiState.query
    val trimmedQuery = query.trim()
    val isQueryActive = trimmedQuery.length >= MIN_SEARCH_QUERY_LENGTH

    // Collect deduplicated search results from catalog rows
    val searchResults = remember(uiState.catalogRows) {
        val seenIds = mutableSetOf<String>()
        val list = mutableListOf<MetaPreview>()
        for (row in uiState.catalogRows) {
            for (item in row.items) {
                if (seenIds.add(item.id)) {
                    list.add(item)
                }
            }
        }
        list
    }

    val displayItems: List<MetaPreview> = if (isQueryActive) {
        searchResults
    } else {
        uiState.discoverResults
    }

    var lastFocusedKeyIndex by remember { mutableIntStateOf(0) }
    val keyFocusRequesters = remember(contentFocusRequester) {
        List(KEYBOARD_KEYS.size) { i ->
            if (i == 0 && contentFocusRequester != null) contentFocusRequester else FocusRequester()
        }
    }
    val resultsFirstItemRequester = remember { FocusRequester() }
    val spaceFocusRequester = remember { FocusRequester() }
    val deleteFocusRequester = remember { FocusRequester() }
    val gridState = rememberLazyGridState()

    // Back handler: if query has text, back clears it; if empty, back returns to Home
    BackHandler(enabled = true) {
        if (query.isNotEmpty()) {
            onQueryChanged("")
        } else {
            onNavigateBackToHome()
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0E12))
            .padding(top = 44.dp)
    ) {
        // --- LEFT COLUMN: Query Display & On-Screen Keyboard ---
        Column(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .padding(start = 44.dp, end = 20.dp, bottom = 24.dp)
                .focusGroup()
        ) {
            // Search Input Display Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .background(Color(0xFF1E1F26), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF333540), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Color(0xFF8E90A0),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (query.isEmpty()) stringResource(R.string.nav_search) else query,
                            color = if (query.isEmpty()) Color(0xFF6B6D7C) else Color.White,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = if (query.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = { onQueryChanged("") },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = Color(0xFFAAAAAA),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 6x6 Character Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (row in 0 until 6) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (col in 0 until KEYBOARD_COLUMNS) {
                            val keyIndex = row * KEYBOARD_COLUMNS + col
                            val char = KEYBOARD_KEYS[keyIndex]
                            val isTopRow = row == 0
                            val isBottomRow = row == 5
                            val isRightCol = col == KEYBOARD_COLUMNS - 1
                            val keyRequester = keyFocusRequesters[keyIndex]

                            NetflixSearchKey(
                                text = char,
                                focusRequester = keyRequester,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .focusProperties {
                                        if (isTopRow && cinemaTopNavFocusRequester != null) {
                                            up = cinemaTopNavFocusRequester
                                        }
                                        if (isBottomRow) {
                                            down = if (col < 3) spaceFocusRequester else deleteFocusRequester
                                        }
                                        if (isRightCol && displayItems.isNotEmpty()) {
                                            right = resultsFirstItemRequester
                                        }
                                    },
                                onFocused = { lastFocusedKeyIndex = keyIndex },
                                onClick = { onQueryChanged(query + char) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action Row: SPACE, DELETE (Netflix TV 2-key layout)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    NetflixSearchKey(
                        text = "SPACE",
                        focusRequester = spaceFocusRequester,
                        modifier = Modifier
                            .weight(3.5f)
                            .height(40.dp)
                            .focusProperties {
                                up = keyFocusRequesters[31]
                                right = deleteFocusRequester
                            },
                        onFocused = { /* keep last key */ },
                        onClick = { onQueryChanged("$query ") }
                    )
                    NetflixSearchKey(
                        text = "DELETE",
                        focusRequester = deleteFocusRequester,
                        modifier = Modifier
                            .weight(2.5f)
                            .height(40.dp)
                            .focusProperties {
                                up = keyFocusRequesters[34]
                                left = spaceFocusRequester
                                if (displayItems.isNotEmpty()) {
                                    right = resultsFirstItemRequester
                                }
                            },
                        onFocused = { /* keep last key */ },
                        onClick = {
                            if (query.isNotEmpty()) {
                                onQueryChanged(query.dropLast(1))
                            }
                        }
                    )
                }
            }
        }

        // --- RIGHT COLUMN: Popular Recommendations / Search Results Poster Grid ---
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 44.dp, bottom = 16.dp)
                .focusGroup()
        ) {
            // Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when {
                        !isQueryActive -> "Explore titles related to:"
                        uiState.isSearching -> "Searching for \"$trimmedQuery\"..."
                        searchResults.isEmpty() -> "No matches found"
                        else -> "Titles related to \"$trimmedQuery\""
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE5E5E5),
                        fontSize = 21.sp,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )

                if (uiState.isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                }
            }

            // Results State
            if (isQueryActive && searchResults.isEmpty() && !uiState.isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.TopStart
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "We couldn't find any matches for \"$trimmedQuery\".",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color(0xFF9E9EA8),
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "Suggestions:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF707280),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            text = "• Check the spelling of your keywords\n• Try different keywords or actors\n• Try more general terms",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFF606270),
                                lineHeight = 22.sp
                            )
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = gridState,
                    contentPadding = PaddingValues(bottom = 64.dp, top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { clip = false }
                ) {
                    itemsIndexed(
                        items = displayItems,
                        key = { _, item -> item.id }
                    ) { index, item ->
                        val isFirstColumn = index % 4 == 0
                        val isTopRow = index < 4
                        val isFirstItem = index == 0

                        NetflixPosterCard(
                            item = item,
                            modifier = Modifier
                                .then(
                                    if (isFirstItem) Modifier.focusRequester(resultsFirstItemRequester)
                                    else Modifier
                                )
                                .focusProperties {
                                    if (isFirstColumn) {
                                        left = keyFocusRequesters.getOrElse(lastFocusedKeyIndex) {
                                            keyFocusRequesters[0]
                                        }
                                    }
                                    if (isTopRow && cinemaTopNavFocusRequester != null) {
                                        up = cinemaTopNavFocusRequester
                                    }
                                },
                            onItemFocus = {
                                onItemFocus?.invoke(item.id, item.apiType)
                            },
                            onClick = {
                                onNavigateToDetail(item.id, item.apiType, item.sourceAddonBaseUrl.orEmpty())
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NetflixSearchKey(
    text: String,
    focusRequester: FocusRequester,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = tween(120),
        label = "keyScale"
    )

    val containerColor by animateColorAsState(
        targetValue = if (isFocused) Color.White else Color(0xFF1B1C22),
        animationSpec = tween(120),
        label = "keyBg"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .scale(scale)
            .focusRequester(focusRequester)
            .onFocusChanged {
                isFocused = it.hasFocus
                if (it.hasFocus) onFocused()
            },
        colors = CardDefaults.colors(
            containerColor = containerColor,
            focusedContainerColor = Color.White
        ),
        border = CardDefaults.border(
            border = Border.None,
            focusedBorder = Border.None,
            pressedBorder = Border.None
        ),
        shape = CardDefaults.shape(RoundedCornerShape(6.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = if (isFocused) Color.Black else Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.offset(y = (-1).dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isFocused) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = if (text.length > 2) 11.sp else 16.sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.Both
                    )
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun NetflixPosterCard(
    item: MetaPreview,
    modifier: Modifier = Modifier,
    onItemFocus: () -> Unit = {},
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.08f else 1.0f,
        animationSpec = tween(140),
        label = "posterScale"
    )

    val imageRequest = remember(item.poster) {
        ImageRequest.Builder(context)
            .data(item.poster)
            .crossfade(true)
            .build()
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .zIndex(if (isFocused) 10f else 0f)
            .fillMaxWidth()
            .aspectRatio(2f / 3f)
            .scale(scale)
            .onFocusChanged {
                isFocused = it.hasFocus
                if (it.hasFocus) onItemFocus()
            },
        colors = CardDefaults.colors(
            containerColor = Color(0xFF181920),
            focusedContainerColor = Color(0xFF181920)
        ),
        border = CardDefaults.border(
            border = Border(
                border = BorderStroke(
                    width = 1.dp,
                    color = Color(0xFF262832)
                ),
                shape = RoundedCornerShape(8.dp)
            ),
            focusedBorder = Border(
                border = BorderStroke(
                    width = 2.5.dp,
                    color = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            )
        ),
        shape = CardDefaults.shape(RoundedCornerShape(8.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!item.poster.isNullOrBlank()) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = item.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF222430))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.name,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
