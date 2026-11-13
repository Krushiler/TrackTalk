package top.krushiler.tracktalk.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.compose.koinInject
import top.krushiler.tracktalk.data.network.AccessTokenStore
import top.krushiler.tracktalk.data.network.AppUpdateState
import top.krushiler.tracktalk.presentation.screens.app.AppOutdatedScreen
import top.krushiler.tracktalk.presentation.screens.auth.AuthRoute
import top.krushiler.tracktalk.presentation.screens.chat.ChatRoute
import top.krushiler.tracktalk.presentation.screens.friends.AddFriendRoute
import top.krushiler.tracktalk.presentation.screens.home.HomeRoute
import top.krushiler.tracktalk.presentation.screens.location.LocationHistoryRoute

@Composable
fun AppNavHost() {
    val session = koinInject<AccessTokenStore>()
    var signedIn by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        session.load()
        signedIn = session.hasSession()
    }
    val restored = signedIn ?: return
    SessionNavHost(signedIn = restored)
}

@Composable
private fun SessionNavHost(signedIn: Boolean) {
    val backStackConfiguration = remember {
        SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(AppDestination.Auth::class)
                    subclass(AppDestination.Home::class)
                    subclass(AppDestination.AddFriend::class)
                    subclass(AppDestination.Chat::class)
                    subclass(AppDestination.LocationHistory::class)
                    subclass(AppDestination.AppOutdated::class)
                }
            }
        }
    }
    val backStack = rememberNavBackStack(
        backStackConfiguration,
        if (signedIn) AppDestination.Home else AppDestination.Auth,
    )
    var sessionApplied by remember { mutableStateOf(false) }
    SideEffect {
        if (sessionApplied) return@SideEffect
        if (!backStack.matchesSession(signedIn)) {
            backStack.clear()
            backStack.add(if (signedIn) AppDestination.Home else AppDestination.Auth)
        }
        sessionApplied = true
    }
    if (!sessionApplied) return

    val appUpdate = koinInject<AppUpdateState>()
    val outdated by appUpdate.outdated.collectAsState()
    LaunchedEffect(outdated) {
        if (outdated && backStack.lastOrNull() !is AppDestination.AppOutdated) {
            backStack.clear()
            backStack.add(AppDestination.AppOutdated)
        }
    }

    NavDisplay(
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        backStack = backStack,
        onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<AppDestination.Auth> {
                AuthRoute(
                    onAuthenticated = {
                        backStack[backStack.lastIndex] = AppDestination.Home
                    },
                )
            }
            entry<AppDestination.Home> {
                HomeRoute(
                    onLogout = {
                        backStack.clear()
                        backStack.add(AppDestination.Auth)
                    },
                    onOpenChat = { backStack.add(AppDestination.Chat(it)) },
                    onOpenHistory = { backStack.add(AppDestination.LocationHistory(it)) },
                    onAddFriend = { backStack.add(AppDestination.AddFriend) },
                )
            }
            entry<AppDestination.AddFriend> {
                AddFriendRoute(onBack = { backStack.removeLastOrNull() })
            }
            entry<AppDestination.Chat> { destination ->
                ChatRoute(person = destination.person, onBack = { backStack.removeLastOrNull() })
            }
            entry<AppDestination.LocationHistory> { destination ->
                LocationHistoryRoute(person = destination.person, onBack = { backStack.removeLastOrNull() })
            }
            entry<AppDestination.AppOutdated> {
                AppOutdatedScreen()
            }
        },
    )
}

private fun List<NavKey>.matchesSession(signedIn: Boolean): Boolean {
    if (lastOrNull() is AppDestination.AppOutdated) return true
    return if (signedIn) {
        any {
            it is AppDestination.Home ||
                it is AppDestination.AddFriend ||
                it is AppDestination.Chat ||
                it is AppDestination.LocationHistory
        }
    } else {
        isNotEmpty() && all { it is AppDestination.Auth }
    }
}
