package glasseswhisperer.pipeline

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform