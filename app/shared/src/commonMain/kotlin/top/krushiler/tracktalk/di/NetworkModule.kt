package top.krushiler.tracktalk.di

import org.koin.dsl.module
import org.koin.dsl.onClose
import top.krushiler.tracktalk.data.network.AccessTokenStore
import top.krushiler.tracktalk.data.network.AppUpdateState
import top.krushiler.tracktalk.data.network.HttpRoutes
import top.krushiler.tracktalk.data.network.RealtimeClient
import top.krushiler.tracktalk.data.network.WebSocketRoutes
import top.krushiler.tracktalk.data.network.createHttpClient
import top.krushiler.tracktalk.data.storage.createTokenStorage

fun networkModule(baseUrl: String, appBuild: Int) = module {
    single { AccessTokenStore(createTokenStorage()) }
    single { AppUpdateState() }
    single { createHttpClient(baseUrl, appBuild, get(), get()) } onClose { it?.close() }
    single { HttpRoutes(get()) }
    single { WebSocketRoutes(get(), get()) }
    single { RealtimeClient(get(), get()) }
}
