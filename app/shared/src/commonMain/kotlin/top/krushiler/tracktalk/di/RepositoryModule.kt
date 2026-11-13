package top.krushiler.tracktalk.di

import org.koin.dsl.module
import top.krushiler.tracktalk.data.repository.AuthRepository
import top.krushiler.tracktalk.data.repository.ChatRepository
import top.krushiler.tracktalk.data.repository.FriendsRepository
import top.krushiler.tracktalk.data.repository.LocationRepository

fun repositoryModule() = module {
    single { AuthRepository(get(), get()) }
    single { FriendsRepository(get()) }
    single { ChatRepository(get()) }
    single { LocationRepository(get()) }
}
