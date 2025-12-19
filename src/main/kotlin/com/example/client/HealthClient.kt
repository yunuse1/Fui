package com.example.client

import com.example.config.AppConfig
import com.example.models.HealthResponse
import com.example.models.ServerInfoResponse
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.healthRoutes() {
    get("/") {
        call.respond(
            ServerInfoResponse(
                message = "${AppConfig.APP_NAME} is running",
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
}

