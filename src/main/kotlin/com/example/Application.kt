package com.example

import com.example.config.AppConfig
import com.example.plugins.configurePlugins
import com.example.client.healthRoutes
import com.example.client.imageRoutes
import com.example.client.ingestRoutes
import com.example.client.cameraRoutes
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

        // Kamera polling endpoint'leri
        cameraRoutes()

        // Kök dizine test-analyze endpointi (shortcut)
        get("/test-analyze") {
            // Görüntü URL'sinden analiz yap
            val startTime = System.currentTimeMillis()
            try {
                val cameraUrl = "https://images.wsdot.wa.gov/nw/005vc13410.jpg"
                val image = javax.imageio.ImageIO.read(java.net.URL(cameraUrl))
                val result = com.example.services.AIAnalysisService.analyzeImage(image)
                val elapsed = System.currentTimeMillis() - startTime
                call.respond(mapOf("success" to true, "analysis" to result, "elapsed_ms" to elapsed, "camera_url" to cameraUrl))
            } catch (e: Exception) {
                call.respond(io.ktor.http.HttpStatusCode.InternalServerError, mapOf("success" to false, "error" to (e.message ?: "Error")))
            }
        }
    }
}
