package top.krushiler.tracktalk.presentation

sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Ready<T>(val value: T) : LoadState<T>
    data class Failed(val message: String) : LoadState<Nothing>
}

fun Throwable.userMessage(): String = message?.takeIf { it.isNotBlank() } ?: "Request failed"
