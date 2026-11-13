import org.gradle.api.tasks.JavaExec

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktor)
}

group = "top.krushiler.tracktalk"
version = "1.0.0"
application {
    mainClass = "top.krushiler.tracktalk.ApplicationKt"
}

tasks.named<JavaExec>("run") {
    description = "Start the local server on port 8080 and return. The process keeps running after Gradle finishes."
    environment("HOST", "0.0.0.0")
    environment("PORT", "8080")
    workingDir = layout.projectDirectory.asFile
    detachServer(role = "local")
}

tasks.register<StopServer>("stop") {
    group = "application"
    description = "Stop the server started by run."
    pidFile.set(layout.buildDirectory.file("server-local.pid"))
}

tasks.register<JavaExec>("runBackend") {
    group = "application"
    description = "Start the server on port 8000 and return. The process keeps running after Gradle finishes."
    dependsOn(tasks.named("classes"))
    classpath = sourceSets.named("main").get().runtimeClasspath
    mainClass.set("top.krushiler.tracktalk.ApplicationKt")
    environment("HOST", "0.0.0.0")
    environment("PORT", "8000")
    workingDir = layout.projectDirectory.asFile
    detachServer(role = "backend")
}

tasks.register<StopServer>("stopBackend") {
    group = "application"
    description = "Stop the server started by runBackend."
    pidFile.set(layout.buildDirectory.file("server-backend.pid"))
}

dependencies {
    api(project(":core"))
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serializationKotlinxJson)
    implementation(libs.ktor.serverWebsockets)
    implementation(libs.ktor.serverAuth)
    implementation(libs.ktor.serverAuthJwt)
    implementation(libs.ktor.serverStatusPages)
    implementation(libs.ktor.serverCors)
    implementation(libs.bcrypt)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    implementation(libs.postgresql)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    testImplementation(libs.h2)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.ktor.clientWebsockets)
    testImplementation(libs.kotlin.testJunit)
}

// JavaExec waits for the process, so :server:run would stay "Gradle build running" for the
// whole server lifetime. Launch it outside the build and let the task finish.
private fun JavaExec.detachServer(role: String) {
    outputs.upToDateWhen { false }
    actions.clear()
    actions.add(StartServerAction(role))
}