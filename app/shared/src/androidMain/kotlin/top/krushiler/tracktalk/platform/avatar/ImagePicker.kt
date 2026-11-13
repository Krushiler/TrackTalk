package top.krushiler.tracktalk.platform.avatar

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import top.krushiler.tracktalk.platform.AvatarPick

lateinit var imagePicker: ImagePicker
    private set

fun installImagePicker(activity: ComponentActivity) {
    imagePicker = ImagePicker(activity)
}

class ImagePicker(private val activity: ComponentActivity) {
    private var continuation: ((AvatarPick) -> Unit)? = null

    private val picker = activity.registerForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val pick = uri?.let(::read)?.let { (bytes, type) -> AvatarPick.Image(bytes, type) } ?: AvatarPick.Cancelled
        val resume = continuation
        continuation = null
        resume?.invoke(pick)
    }

    suspend fun pick(): AvatarPick = suspendCancellableCoroutine { cont ->
        continuation = { pick ->
            if (cont.isActive) cont.resume(pick)
        }
        cont.invokeOnCancellation { continuation = null }
        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun read(uri: Uri): Pair<ByteArray, String>? {
        val type = activity.contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = activity.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        return bytes to type
    }
}
