package top.krushiler.tracktalk.config

import java.io.File

data class AppConfig(
    val host: String = "0.0.0.0",
    val port: Int = 8080,
    val jwtSecret: String,
    val jwtExpireMinutes: Long = 60L * 24L * 7L,
    val jwtRefreshExpireMinutes: Long = 60L * 24L * 30L,
    val minAppBuild: Int = 1,
    val bcryptCost: Int = 12,
    val databaseUrl: String? = null,
) {
    companion object {
        fun fromEnvironment(): AppConfig {
            val dotenv = loadDotEnv()
            val jwtSecret = env(dotenv, "JWT_SECRET")
                ?: error("JWT_SECRET is missing. Set it in the environment or server/.env (see server/.env.example).")
            return AppConfig(
                host = env(dotenv, "HOST") ?: "0.0.0.0",
                port = env(dotenv, "PORT")?.toIntOrNull() ?: 8080,
                jwtSecret = jwtSecret,
                jwtExpireMinutes = env(dotenv, "JWT_EXPIRE_MINUTES")?.toLongOrNull()
                    ?: (60L * 24L * 7L),
                jwtRefreshExpireMinutes = env(dotenv, "JWT_REFRESH_EXPIRE_MINUTES")?.toLongOrNull()
                    ?: (60L * 24L * 30L),
                minAppBuild = env(dotenv, "MIN_APP_BUILD")?.toIntOrNull() ?: 1,
                bcryptCost = env(dotenv, "BCRYPT_COST")?.toIntOrNull()?.coerceIn(4, 31) ?: 12,
                databaseUrl = env(dotenv, "DATABASE_URL"),
            )
        }
    }
}

internal fun loadDotEnv(): Map<String, String> {
    val file = sequenceOf(File(".env"), File("server/.env")).firstOrNull { it.isFile } ?: return emptyMap()
    return parseDotEnv(file.readText())
}

internal fun parseDotEnv(text: String): Map<String, String> {
    val values = linkedMapOf<String, String>()
    text.lineSequence().forEach { rawLine ->
        val line = rawLine.trim()
        if (line.isEmpty() || line.startsWith("#")) return@forEach
        val body = line.removePrefix("export ").trim()
        val separator = body.indexOf('=')
        if (separator <= 0) return@forEach
        val key = body.substring(0, separator).trim()
        var value = body.substring(separator + 1).trim()
        if (value.length >= 2 &&
            ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))
        ) {
            value = value.substring(1, value.length - 1)
        }
        values[key] = value
    }
    return values
}

private fun env(dotenv: Map<String, String>, name: String): String? =
    System.getenv(name)?.takeIf { it.isNotBlank() } ?: dotenv[name]?.takeIf { it.isNotBlank() }
