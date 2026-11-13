package top.krushiler.tracktalk.presentation.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import top.krushiler.tracktalk.data.network.RealtimeClient
import top.krushiler.tracktalk.presentation.navigation.PersonRef
import top.krushiler.tracktalk.presentation.screens.friends.FriendsRoute
import top.krushiler.tracktalk.presentation.screens.map.MapRoute
import top.krushiler.tracktalk.presentation.screens.profile.ProfileRoute
import top.krushiler.tracktalk.presentation.widgets.AppBottomNav
import top.krushiler.tracktalk.presentation.widgets.HomeTab

@Composable
fun HomeRoute(
    onLogout: () -> Unit,
    onOpenChat: (PersonRef) -> Unit,
    onOpenHistory: (PersonRef) -> Unit,
    onAddFriend: () -> Unit,
    locationSync: LocationSyncViewModel = koinViewModel(),
) {
    val realtime = koinInject<RealtimeClient>()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) { realtime.ensureStarted() }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) locationSync.onAppResumed()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val sync by locationSync.uiState.collectAsState()
    var tab by rememberSaveable { mutableStateOf(HomeTab.Map) }
    Box(Modifier.fillMaxSize()) {
        when (tab) {
            HomeTab.Map -> MapRoute(locationSync = locationSync, onOpenHistory = onOpenHistory)
            HomeTab.Friends -> FriendsRoute(
                onOpenChat = onOpenChat,
                onOpenHistory = onOpenHistory,
                onAddFriend = onAddFriend,
            )
            HomeTab.Profile -> ProfileRoute(sharing = sync, onLoggedOut = onLogout)
        }
        AppBottomNav(
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
