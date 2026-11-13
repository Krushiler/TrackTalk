package top.krushiler.tracktalk.platform.context

import android.content.Context
import androidx.activity.ComponentActivity
import top.krushiler.tracktalk.platform.avatar.installImagePicker
import top.krushiler.tracktalk.platform.permission.installLocationPermissions
import top.krushiler.tracktalk.platform.permission.installNotificationPermissions

lateinit var androidAppContext: Context
    private set

fun hasAndroidAppContext(): Boolean = ::androidAppContext.isInitialized

lateinit var androidActivity: ComponentActivity
    private set

fun installAndroidAppContext(activity: ComponentActivity) {
    androidAppContext = activity.applicationContext
    androidActivity = activity
    installLocationPermissions(activity)
    installNotificationPermissions(activity)
    installImagePicker(activity)
}
