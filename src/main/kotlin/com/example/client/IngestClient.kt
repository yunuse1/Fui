package com.example.client

import com.example.models.BatchIngestResponse
import com.example.models.IngestPayload
import com.example.models.IngestResponse
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("IngestRoutes")

fun Route.ingestRoutes() {
    route("/ingest") {
        post {
            val payload = call.receive<IngestPayload>()
            logger.info("Received ingest from device: ${payload.deviceId}, timestamp: ${payload.timestamp}")

            // TODO: Burada veri işleme/kaydetme mantığı eklenebilir

            call.respond(
                IngestResponse(
                    received = true,
                    deviceId = payload.deviceId
                )
            )
        }

        // Batch ingest endpoint (gelecekte kullanılabilir)
        post("/batch") {
            val payloads = call.receive<List<IngestPayload>>()
            logger.info("Received batch ingest: ${payloads.size} items")

            call.respond(
                BatchIngestResponse(
                    received = true,
                    count = payloads.size
                )
            )
        }
    }
}
