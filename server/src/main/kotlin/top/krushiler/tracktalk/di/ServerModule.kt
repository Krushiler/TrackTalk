package top.krushiler.tracktalk.di

import org.koin.core.module.Module
import top.krushiler.tracktalk.config.AppConfig
import top.krushiler.tracktalk.feature.auth.authModule
import top.krushiler.tracktalk.feature.chat.chatModule
import top.krushiler.tracktalk.feature.friendship.friendshipModule
import top.krushiler.tracktalk.feature.location.locationModule
import top.krushiler.tracktalk.feature.push.pushModule
import top.krushiler.tracktalk.feature.realtime.realtimeModule
import top.krushiler.tracktalk.feature.user.userModule

internal fun serverModules(config: AppConfig): List<Module> = listOf(
    databaseModule(config),
    userModule(),
    authModule(),
    friendshipModule(),
    locationModule(),
    chatModule(),
    realtimeModule(),
    pushModule(),
)
