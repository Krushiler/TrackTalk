package top.krushiler.tracktalk.feature.location

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal fun locationModule() = module {
    singleOf(::LocationRepository)
    singleOf(::LocationService)
}
