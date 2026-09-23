package com.nuvio.tv.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.ui.navigation.Screen
import com.nuvio.tv.ui.theme.NuvioTheme

private const val CINEMA_NAV_FOCUS_DEBOUNCE_MS = 190L
private const val CINEMA_NAV_FOCUS_RESTORE_ATTEMPTS = 30
private val CINEMA_RAIL_ROUTES = setOf(
    Screen.Home.route,
    Screen.CinemaMovies.route,
    Screen.CinemaShows.route
)

/** Shared only by the Cinema shell and its content rails. */
@Stable
internal class CinemaFocusController {
    val contentFocusRequester = FocusRequester()
    private val navFocusRequesters = mutableMapOf<String, FocusRequester>()

    var selectedRoute by mutableStateOf(Screen.Home.route)
    var activeCinemaCategory by mutableStateOf(Screen.Home.route)
    var pendingNavigationRoute by mutableStateOf<String?>(null)
        private set
    var navigationGeneration by mutableIntStateOf(0)
        private set
    var focusedNavRoute by mutableStateOf<String?>(null)
        private set

    fun requester(route: String): FocusRequester =
        navFocusRequesters.getOrPut(route) { FocusRequester() }

    fun beginTopBarNavigation(route: String) {
        pendingNavigationRoute = route
        navigationGeneration++
    }

    fun onNavFocusChanged(route: String, hasFocus: Boolean) {
        if (hasFocus) {
            focusedNavRoute = route
            // A horizontal move is an explicit user choice; do not let the previous
            // destination's restore loop pull focus back to the old tab.
            if (pendingNavigationRoute != null && pendingNavigationRoute != route) {
                pendingNavigationRoute = null
            }
        } else if (focusedNavRoute == route) {
            focusedNavRoute = null
        }
    }

    fun cancelPendingNavigation() {
        pendingNavigationRoute = null
    }
}

internal val LocalCinemaFocusController = staticCompositionLocalOf<CinemaFocusController?> { null }

/**
 * The Cinema shell navigation deliberately does not request focus on composition. This keeps
 * initial focus in the content rail. Once a user focuses a tab, the tab owns focus while its
 * root destination changes underneath it.
 */
@Composable
internal fun CinemaTopNavigation(
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusController = LocalCinemaFocusController.current
    val pendingRoute = focusController?.pendingNavigationRoute
    val navigationGeneration = focusController?.navigationGeneration ?: 0

    // The shell survives root destination replacement. Re-requesting the selected tab for a
    // short window covers both the NavHost transition and a destination's initial rail focus.
    // It is cancelled as soon as the user moves to another tab or presses Down intentionally.
    LaunchedEffect(selectedRoute, pendingRoute, navigationGeneration) {
        val controller = focusController ?: return@LaunchedEffect
        if (pendingRoute != selectedRoute) return@LaunchedEffect
        val targetRequester = controller.requester(selectedRoute)
        repeat(CINEMA_NAV_FOCUS_RESTORE_ATTEMPTS) {
            if (controller.pendingNavigationRoute != selectedRoute) return@LaunchedEffect
            if (controller.focusedNavRoute != null && controller.focusedNavRoute != selectedRoute) {
                return@LaunchedEffect
            }
            if (controller.focusedNavRoute != selectedRoute) {
                runCatching { targetRequester.requestFocus() }
            }
            withFrameNanos { }
            kotlinx.coroutines.delay(40L)
        }
        // Keep the restore guard alive long enough for a newly composed HomeScreen to issue its
        // normal initial focus request, but never fight a subsequent D-pad gesture.
        if (controller.pendingNavigationRoute == selectedRoute &&
            controller.focusedNavRoute == selectedRoute
        ) {
            kotlinx.coroutines.delay(900L)
            if (controller.focusedNavRoute == selectedRoute) {
                controller.cancelPendingNavigation()
            }
        }
    }
    SideEffect {
        focusController?.selectedRoute = selectedRoute
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xF20A0B0F), Color(0xB30A0B0F), Color.Transparent)
                )
            )
            .padding(horizontal = 32.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CinemaNavButton(
                label = stringResource(R.string.nav_home),
                route = Screen.Home.route,
                selectedRoute = selectedRoute,
                onNavigate = onNavigate,
                focusController = focusController
            )
            CinemaNavButton(
                label = stringResource(R.string.nav_movies),
                route = Screen.CinemaMovies.route,
                selectedRoute = selectedRoute,
                onNavigate = onNavigate,
                focusController = focusController
            )
            CinemaNavButton(
                label = stringResource(R.string.nav_tv_shows),
                route = Screen.CinemaShows.route,
                selectedRoute = selectedRoute,
                onNavigate = onNavigate,
                focusController = focusController
            )
            CinemaNavButton(
                label = stringResource(R.string.nav_search),
                route = Screen.Search.route,
                selectedRoute = selectedRoute,
                onNavigate = onNavigate,
                focusController = focusController,
                leadingIcon = Icons.Default.Search
            )
            CinemaNavButton(
                label = stringResource(R.string.nav_my_list),
                route = Screen.Library.route,
                selectedRoute = selectedRoute,
                onNavigate = onNavigate,
                focusController = focusController,
                modifier = Modifier.focusProperties {
                    focusController?.requester(Screen.Settings.route)?.let { right = it }
                }
            )
            Box(modifier = Modifier.weight(1f))
            CinemaNavButton(
                label = stringResource(R.string.nav_settings),
                route = Screen.Settings.route,
                selectedRoute = selectedRoute,
                onNavigate = onNavigate,
                focusController = focusController,
                leadingIcon = Icons.Default.Settings,
                modifier = Modifier.focusProperties {
                    focusController?.requester(Screen.Library.route)?.let { left = it }
                }
            )
        }
    }
}

@Composable
private fun CinemaNavButton(
    label: String,
    route: String,
    selectedRoute: String,
    onNavigate: (String) -> Unit,
    focusController: CinemaFocusController?,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    var focused by remember { mutableStateOf(false) }
    val latestOnNavigate by rememberUpdatedState(onNavigate)
    val isRailRoute = route in CINEMA_RAIL_ROUTES
    val activeCategory = focusController?.activeCinemaCategory ?: Screen.Home.route
    val selected = if (isRailRoute && selectedRoute in CINEMA_RAIL_ROUTES) {
        activeCategory == route
    } else {
        selectedRoute == route
    }

    fun navigateFromTopBar() {
        if (isRailRoute) {
            focusController?.activeCinemaCategory = route
        }
        focusController?.beginTopBarNavigation(route)
        latestOnNavigate(route)
    }

    // Focus is intentionally a navigation gesture in Cinema. Debouncing here, rather than in
    // the route shell, cancels immediately when D-pad focus continues to the next tab.
    LaunchedEffect(focused, route, selectedRoute, focusController?.activeCinemaCategory) {
        if (!focused) return@LaunchedEffect
        kotlinx.coroutines.delay(CINEMA_NAV_FOCUS_DEBOUNCE_MS)
        val currentActive = if (isRailRoute && selectedRoute in CINEMA_RAIL_ROUTES) {
            focusController?.activeCinemaCategory ?: selectedRoute
        } else {
            selectedRoute
        }
        if (focused && currentActive != route) {
            navigateFromTopBar()
        }
    }

    val containerColor by animateColorAsState(
        targetValue = when {
            focused -> Color.White.copy(alpha = 0.92f)
            selected -> Color.White.copy(alpha = 0.14f)
            else -> Color.Transparent
        },
        animationSpec = tween(160),
        label = "cinemaNavButton"
    )

    val navFocusRequester = focusController?.requester(route)
    Card(
        onClick = { navigateFromTopBar() },
        modifier = modifier
            .height(40.dp)
            .then(if (navFocusRequester != null) Modifier.focusRequester(navFocusRequester) else Modifier)
            .then(
                if (focusController != null && (route in CINEMA_RAIL_ROUTES || route == Screen.Search.route)) {
                    // The Cinema rail and Search have stable shared entry requesters.
                    Modifier.focusProperties {
                        down = focusController.contentFocusRequester
                    }
                } else Modifier
            )
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionDown) {
                    focusController?.cancelPendingNavigation()
                }
                false
            }
            .onFocusChanged {
                focused = it.hasFocus
                focusController?.onNavFocusChanged(route, it.hasFocus)
            },
        colors = CardDefaults.colors(
            containerColor = containerColor,
            focusedContainerColor = containerColor
        ),
        border = CardDefaults.border(
            border = Border.None,
            focusedBorder = Border.None,
            pressedBorder = Border.None
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(21.dp))
    ) {
        val textColor = when {
            focused -> Color.Black
            selected -> Color.White
            else -> Color.White.copy(alpha = 0.65f)
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = if (leadingIcon != null) 14.dp else 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier
                            .size(16.dp)
                            .offset(y = (-1).dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = label,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(y = (-1).dp),
                    style = androidx.tv.material3.MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (selected || focused) FontWeight.SemiBold else FontWeight.Medium,
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
}
