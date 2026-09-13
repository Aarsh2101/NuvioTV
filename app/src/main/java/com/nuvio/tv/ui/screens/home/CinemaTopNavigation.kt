package com.nuvio.tv.ui.screens.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
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
            CinemaNavButton(stringResource(R.string.nav_home), Screen.Home.route, selectedRoute, onNavigate, focusController)
            CinemaNavButton(stringResource(R.string.nav_movies), Screen.CinemaMovies.route, selectedRoute, onNavigate, focusController)
            CinemaNavButton(stringResource(R.string.nav_tv_shows), Screen.CinemaShows.route, selectedRoute, onNavigate, focusController)
            CinemaNavButton(stringResource(R.string.nav_search), Screen.Search.route, selectedRoute, onNavigate, focusController)
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
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val latestOnNavigate by rememberUpdatedState(onNavigate)
    val selected = selectedRoute == route

    fun navigateFromTopBar() {
        focusController?.beginTopBarNavigation(route)
        latestOnNavigate(route)
    }

    // Focus is intentionally a navigation gesture in Cinema. Debouncing here, rather than in
    // the route shell, cancels immediately when D-pad focus continues to the next tab.
    LaunchedEffect(focused, route, selectedRoute) {
        if (!focused) return@LaunchedEffect
        kotlinx.coroutines.delay(CINEMA_NAV_FOCUS_DEBOUNCE_MS)
        if (focused && selectedRoute != route) {
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
                if (focusController != null && route in CINEMA_RAIL_ROUTES) {
                    // The Cinema rail has a stable shared entry requester. Other shell screens
                    // own different first controls, so leave Down geometric rather than pointing
                    // at a requester that is not attached to their content.
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
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (focused) Color.Black else Color.White,
                style = androidx.tv.material3.MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (selected || focused) FontWeight.SemiBold else FontWeight.Medium
                ),
                maxLines = 1
            )
        }
    }
}
