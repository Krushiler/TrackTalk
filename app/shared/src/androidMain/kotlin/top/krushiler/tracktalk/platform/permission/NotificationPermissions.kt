package top.krushiler.tracktalk.platform.permission

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private const val PostNotifications = "android.permission.POST_NOTIFICATIONS"

lateinit var notificationPermissions: NotificationPermissions
    private set

fun installNotificationPermissions(activity: ComponentActivity) {
    notificationPermissions = NotificationPermissions(activity)
}

class NotificationPermissions(private val activity: ComponentActivity) {
    private var continuation: ((Boolean) -> Unit)? = null
    private var inFlight = false

    private val permission = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val resume = continuation
        continuation = null
        resume?.invoke(granted)
    }

    suspend fun request() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (activity.checkSelfPermission(PostNotifications) == PackageManager.PERMISSION_GRANTED) return
        if (inFlight) return
        inFlight = true
        try {
            suspendCancellableCoroutine { cont ->
                continuation = { granted ->
                    if (cont.isActive) cont.resume(granted)
                }
                cont.invokeOnCancellation { continuation = null }
                permission.launch(PostNotifications)
            }
        } finally {
            inFlight = false
        }
    }
}
