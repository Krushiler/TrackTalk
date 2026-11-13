package top.krushiler.tracktalk.di

import org.koin.dsl.module
import top.krushiler.tracktalk.platform.createAvatarPicker
import top.krushiler.tracktalk.platform.createDeviceLocation

fun platformModule() = module {
    single { createDeviceLocation() }
    single { createAvatarPicker() }
}
