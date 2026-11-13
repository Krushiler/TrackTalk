package top.krushiler.tracktalk

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform