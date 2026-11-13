import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.googleServices)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

val keystoreProperties = Properties()
val keystorePropertiesFile = file("key.properties")
check(keystorePropertiesFile.isFile) { "Missing $keystorePropertiesFile" }
keystorePropertiesFile.inputStream().use(keystoreProperties::load)

fun keystoreValue(name: String): String =
    keystoreProperties.getProperty(name) ?: error("key.properties is missing $name")

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) {
        file.inputStream().use(::load)
    }
}

val localBaseUrl: String =
    localProperties.getProperty("tracktalk.baseUrl")?.takeIf { it.isNotBlank() }
        ?: "http://10.0.2.2:8080"

fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

dependencies {
    implementation(project(":app:shared"))

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
    runtimeOnly(libs.maplibre.compose.runtime.opengl)
}

android {
    namespace = "top.krushiler.tracktalk"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    signingConfigs {
        create("release") {
            keyAlias = keystoreValue("keyAlias")
            keyPassword = keystoreValue("keyPassword")
            storeFile = file(keystoreValue("storeFile"))
            storePassword = keystoreValue("storePassword")
        }
    }
    defaultConfig {
        applicationId = "ru.powerofpoint.gms_app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 5
        versionName = "1.0.2"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    flavorDimensions += "backend"
    productFlavors {
        create("local") {
            dimension = "backend"
            versionNameSuffix = "-local"
            // Emulator reaches the host at 10.0.2.2. Override with tracktalk.baseUrl in local.properties.
            buildConfigField("String", "BASE_URL", localBaseUrl.asBuildConfigString())
        }
        create("laptop") {
            dimension = "backend"
            versionNameSuffix = "-laptop"
            buildConfigField("String", "BASE_URL", "http://192.168.0.102:8080".asBuildConfigString())
        }
        create("backend") {
            dimension = "backend"
            buildConfigField("String", "BASE_URL", "https://api.gms.krushiler.top".asBuildConfigString())
        }
    }
    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}
