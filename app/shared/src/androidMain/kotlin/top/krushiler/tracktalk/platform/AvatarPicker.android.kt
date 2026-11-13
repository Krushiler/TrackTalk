package top.krushiler.tracktalk.platform

import top.krushiler.tracktalk.platform.avatar.AndroidAvatarPicker

actual fun createAvatarPicker(): AvatarPicker = AndroidAvatarPicker()
