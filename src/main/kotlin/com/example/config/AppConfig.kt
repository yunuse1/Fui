package com.example.config

object AppConfig {
    const val APP_NAME = "fui-server"
    const val APP_VERSION = "0.1.0"

    object Server {
        const val HOST = "0.0.0.0"
        const val PORT = 8080
    }

    object Headers {
        const val ENGINE_HEADER = "X-Engine"
        const val ENGINE_VALUE = "Ktor"
    }
}

