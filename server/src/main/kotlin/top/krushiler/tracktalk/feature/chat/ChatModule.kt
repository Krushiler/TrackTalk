package top.krushiler.tracktalk.feature.chat

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal fun chatModule() = module {
    singleOf(::ChatRepository)
    singleOf(::ChatService)
}
