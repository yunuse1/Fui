package com.example

import com.example.config.AppConfig
import com.example.plugins.configurePlugins
import com.example.client.healthRoutes
import com.example.client.imageRoutes
import com.example.client.ingestRoutes
import com.example.services.DatabaseService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Application")

fun main() {
    logger.info("Starting ${AppConfig.APP_NAME} v${AppConfig.APP_VERSION}")

    // Veritabanını başlat
    try {
        DatabaseService.init()
        logger.info("Database initialized")
    } catch (e: Exception) {
        logger.warn("Database initialization failed, running without database: ${e.message}")
    }

    // Graceful shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread {
        logger.info("Shutting down...")
        DatabaseService.close()
    })

    embeddedServer(
        Netty,
        port = AppConfig.Server.PORT,
        host = AppConfig.Server.HOST,
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    // Plugin'leri yükle
    configurePlugins()

    // Route'ları yapılandır
    configureRouting()

    logger.info("Application configured successfully")
}

fun Application.configureRouting() {
    routing {
        // Health ve genel endpoint'ler
        healthRoutes()

        // Veri alım endpoint'leri
        ingestRoutes()

        // Görüntü analiz endpoint'leri
        imageRoutes()
    }
}

