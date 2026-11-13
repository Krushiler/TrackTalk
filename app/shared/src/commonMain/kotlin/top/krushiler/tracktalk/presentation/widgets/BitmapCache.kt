package top.krushiler.tracktalk.presentation.widgets

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.decodeToImageBitmap
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object BitmapCache {
    private val images = LinkedHashMap<String, ImageBitmap>()
    private val order = ArrayDeque<String>()
    private val lock = Mutex()

    suspend fun load(http: HttpClient, url: String): ImageBitmap? {
        lock.withLock { touch(url) }?.let { return it }
        val bytes = runCatching { http.get(url).bodyAsBytes() }.getOrNull() ?: return null
        val bitmap = runCatching { bytes.decodeToImageBitmap() }.getOrNull() ?: return null
        return lock.withLock {
            touch(url) ?: bitmap.also { store(url, it) }
        }
    }

    private fun store(url: String, bitmap: ImageBitmap) {
        val existed = images.put(url, bitmap) != null
        if (existed) order.remove(url)
        order.addLast(url)
        while (order.size > 80) images.remove(order.removeFirst())
    }

    private fun touch(url: String): ImageBitmap? {
        val bitmap = images[url] ?: return null
        order.remove(url)
        order.addLast(url)
        return bitmap
    }
}
