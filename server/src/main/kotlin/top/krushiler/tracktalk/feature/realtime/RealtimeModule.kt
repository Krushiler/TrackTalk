package top.krushiler.tracktalk.feature.realtime

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal fun realtimeModule() = module {
    singleOf(::RealtimeEventBus)
    singleOf(::RealtimeService)
    singleOf(::RealtimeHandler)
}
