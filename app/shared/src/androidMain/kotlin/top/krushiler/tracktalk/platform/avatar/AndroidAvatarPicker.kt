package top.krushiler.tracktalk.platform.avatar

import top.krushiler.tracktalk.platform.AvatarPick
import top.krushiler.tracktalk.platform.AvatarPicker

class AndroidAvatarPicker : AvatarPicker {
    override suspend fun pick(): AvatarPick = imagePicker.pick()
}
