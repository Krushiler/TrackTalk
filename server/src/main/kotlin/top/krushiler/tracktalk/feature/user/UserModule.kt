package top.krushiler.tracktalk.feature.user

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal fun userModule() = module {
    singleOf(::UserRepository)
    singleOf(::InMemoryAvatarStorage) bind AvatarStorage::class
    singleOf(::Profiles)
    singleOf(::UserService)
}
