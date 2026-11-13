package top.krushiler.tracktalk.feature.friendship

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal fun friendshipModule() = module {
    singleOf(::FriendshipRepository)
    singleOf(::FriendshipService)
}
