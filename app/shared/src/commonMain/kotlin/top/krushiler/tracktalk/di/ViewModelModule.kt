package top.krushiler.tracktalk.di

import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import top.krushiler.tracktalk.presentation.navigation.PersonRef
import top.krushiler.tracktalk.presentation.screens.auth.AuthViewModel
import top.krushiler.tracktalk.presentation.screens.chat.ChatViewModel
import top.krushiler.tracktalk.presentation.screens.friends.AddFriendViewModel
import top.krushiler.tracktalk.presentation.screens.friends.FriendsViewModel
import top.krushiler.tracktalk.presentation.screens.home.LocationSyncViewModel
import top.krushiler.tracktalk.presentation.screens.home.MapViewModel
import top.krushiler.tracktalk.presentation.screens.location.LocationHistoryViewModel
import top.krushiler.tracktalk.presentation.screens.profile.ProfileViewModel

fun viewModelModule(baseUrl: String) = module {
    viewModelOf(::AuthViewModel)
    viewModelOf(::MapViewModel)
    viewModelOf(::FriendsViewModel)
    viewModelOf(::AddFriendViewModel)
    viewModelOf(::ProfileViewModel)
    viewModel {
        LocationSyncViewModel(
            deviceLocation = get(),
            locationRepository = get(),
            accessToken = get(),
            baseUrl = baseUrl,
        )
    }
    viewModel { (person: PersonRef) -> ChatViewModel(person, get(), get()) }
    viewModel { (person: PersonRef) -> LocationHistoryViewModel(person, get(), get()) }
}
