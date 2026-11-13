package top.krushiler.tracktalk

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import top.krushiler.tracktalk.platform.context.installAndroidAppContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        installAndroidAppContext(this)
        setContent {
            App(baseUrl = BuildConfig.BASE_URL, appBuild = BuildConfig.VERSION_CODE)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
