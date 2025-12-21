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
import io.ktor.server.response.*
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

        // Kök dizine test-analyze endpointi (shortcut)
        get("/test-analyze") {
            // Aynı işlemleri imageRoutes içindekiyle yap
            val startTime = System.currentTimeMillis()
            try {
                val testImage = java.awt.image.BufferedImage(100, 100, java.awt.image.BufferedImage.TYPE_INT_RGB)
                val graphics = testImage.createGraphics()
                graphics.color = java.awt.Color(135, 206, 235)
                graphics.fillRect(0, 0, 100, 33)
                graphics.color = java.awt.Color(34, 139, 34)
                graphics.fillRect(0, 33, 100, 67)
                graphics.color = java.awt.Color(139, 69, 19)
                graphics.fillRect(45, 50, 10, 30)
                graphics.color = java.awt.Color(0, 128, 0)
                graphics.fillOval(35, 35, 30, 30)
                graphics.dispose()
                val result = com.example.services.AIAnalysisService.analyzeImage(testImage)
                val elapsed = System.currentTimeMillis() - startTime
                call.respond(mapOf("success" to true, "analysis" to result, "elapsed_ms" to elapsed))
            } catch (e: Exception) {
                call.respond(io.ktor.http.HttpStatusCode.InternalServerError, mapOf("success" to false, "error" to (e.message ?: "Error")))
            }
        }
    }
}
