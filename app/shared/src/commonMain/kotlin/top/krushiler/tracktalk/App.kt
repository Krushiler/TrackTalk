package top.krushiler.tracktalk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration
import top.krushiler.tracktalk.di.appModules
import top.krushiler.tracktalk.presentation.navigation.AppNavHost
import top.krushiler.tracktalk.presentation.theme.AppTheme
import top.krushiler.tracktalk.presentation.theme.LocalDownloadUrl

@Composable
@Preview
fun App(baseUrl: String = "", appBuild: Int = 1, downloadUrl: String = "") {
    val viewModelStoreOwner = rememberViewModelStoreOwner()

    CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
        key(baseUrl, appBuild) {
            KoinApplication(configuration = koinConfiguration { modules(appModules(baseUrl, appBuild)) }) {
                AppTheme {
                    CompositionLocalProvider(LocalDownloadUrl provides downloadUrl) {
                        AppNavHost()
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberViewModelStoreOwner(): ViewModelStoreOwner {
    val owner = remember {
        object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
    }
    DisposableEffect(owner) {
        onDispose { owner.viewModelStore.clear() }
    }
    return owner
}
