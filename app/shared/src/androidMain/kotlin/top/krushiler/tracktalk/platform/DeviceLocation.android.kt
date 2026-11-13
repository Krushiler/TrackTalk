package top.krushiler.tracktalk.platform

import top.krushiler.tracktalk.platform.location.AndroidDeviceLocation

actual fun createDeviceLocation(): DeviceLocation = AndroidDeviceLocation()
