package com.example.client

import com.example.config.AppConfig
import com.example.models.DbStatusResponse
import com.example.models.HealthResponse
import com.example.models.ServerInfoResponse
import com.example.services.DatabaseService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.healthRoutes() {
    get("/") {
        call.respond(
            ServerInfoResponse(
                message = "${AppConfig.APP_NAME} is running (Local-first, Privacy-focused)",
                version = AppConfig.APP_VERSION
            )
        )
    }

    get("/health") {
        call.respond(
            HealthResponse(
                status = "ok",
                service = AppConfig.APP_NAME,
                version = AppConfig.APP_VERSION
            )
        )
    }

    get("/db-status") {
        call.respond(DbStatusResponse(
            databaseConnected = DatabaseService.isReady(),
            status = if (DatabaseService.isReady()) "connected" else "disconnected",
            databaseUrl = "${DatabaseService.getDbType()} - ${DatabaseService.getDbPath()}"
        ))
    }

    // Tüm local verileri sil (GDPR - right to be forgotten)
    delete("/clear-data") {
        val success = DatabaseService.clearAllData()
        call.respond(mapOf(
            "success" to success,
            "message" to if (success) "All local data cleared" else "Failed to clear data"
        ))
    }
}

