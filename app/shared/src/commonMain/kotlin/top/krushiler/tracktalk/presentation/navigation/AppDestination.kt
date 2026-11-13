package top.krushiler.tracktalk.presentation.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppDestination : NavKey {
    @Serializable
    data object Auth : AppDestination

    @Serializable
    data object Home : AppDestination

    @Serializable
    data object AddFriend : AppDestination

    @Serializable
    data class Chat(val person: PersonRef) : AppDestination

    @Serializable
    data class LocationHistory(val person: PersonRef) : AppDestination

    @Serializable
    data object AppOutdated : AppDestination
}
