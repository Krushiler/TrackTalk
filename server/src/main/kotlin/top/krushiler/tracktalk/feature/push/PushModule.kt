package top.krushiler.tracktalk.feature.push

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal fun pushModule() = module {
    singleOf(::PushService)
}
