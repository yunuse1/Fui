package com.example.plugins

import com.example.config.AppConfig
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import kotlinx.serialization.json.Json

fun Application.configurePlugins() {
    configureSerialization()
    configureHeaders()
    configureCompression()
}

private fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
}

private fun Application.configureHeaders() {
    install(DefaultHeaders) {
        header(AppConfig.Headers.ENGINE_HEADER, AppConfig.Headers.ENGINE_VALUE)
    }
}

private fun Application.configureCompression() {
    install(Compression) {
        gzip {
            priority = 1.0
        }
        deflate {
            priority = 10.0
            minimumSize(1024)
        }
    }
}

