package top.krushiler.tracktalk.platform.url

import android.content.Intent
import android.net.Uri
import top.krushiler.tracktalk.platform.context.androidAppContext

fun openAndroidExternalUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    androidAppContext.startActivity(intent)
}
