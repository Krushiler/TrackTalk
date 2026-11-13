package top.krushiler.tracktalk.feature.auth

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

internal fun authModule() = module {
    singleOf(::TokenService)
    singleOf(::AuthService)
}
