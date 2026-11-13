package top.krushiler.tracktalk.data.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppUpdateState {
    private val outdatedFlag = MutableStateFlow(false)
    val outdated: StateFlow<Boolean> = outdatedFlag.asStateFlow()

    fun markOutdated() {
        outdatedFlag.value = true
    }
}
