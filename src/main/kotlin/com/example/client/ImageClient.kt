package com.example.client

import com.example.models.*
import com.example.services.AIAnalysisService
import com.example.services.ImageService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("ImageRoutes")

fun Route.imageRoutes() {
    route("/image") {
        post("/analyze") {
            try {
                val request = call.receive<ImageAnalysisRequest>()
                logger.info("Analyzing image: ${request.filename}")

                val bufferedImage = ImageService.decodeBase64Image(request.imageBase64)

                if (bufferedImage == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Invalid image format or corrupted data")
                    )
                    return@post
                }

                // Boyut kontrolü
                if (!ImageService.validateImageSize(bufferedImage)) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Image too large. Maximum dimensions: 4096x4096")
                    )
                    return@post
                }

                val result = ImageService.analyzeImage(bufferedImage)
                logger.info("Image analyzed: ${result.width}x${result.height}")

                call.respond(
                    ImageAnalysisResponse(
                        success = true,
                        analysis = result
                    )
                )
            } catch (e: Exception) {
                logger.error("Error analyzing image: ${e.message}", e)
                call.respond(
                    HttpStatusCode.BadRequest,
                    ImageAnalysisResponse(
                        success = false,
                        error = e.message ?: "Unknown error"
                    )
                )
            }
        }

        // Basit görüntü bilgisi endpoint'i
        post("/info") {
            try {
                val request = call.receive<ImageAnalysisRequest>()
                val bufferedImage = ImageService.decodeBase64Image(request.imageBase64)

                if (bufferedImage == null) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse("Invalid image format")
                    )
                    return@post
                }

                call.respond(
                    mapOf(
                        "filename" to request.filename,
                        "width" to bufferedImage.width,
                        "height" to bufferedImage.height,
                        "pixelCount" to ImageService.getPixelCount(bufferedImage)
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(e.message ?: "Unknown error")
                )
            }
        }
    }

    // Eski endpoint'i koruyalım (backward compatibility)
    post("/analyze-image") {
        try {
            val request = call.receive<ImageAnalysisRequest>()
            val bufferedImage = ImageService.decodeBase64Image(request.imageBase64)

            if (bufferedImage == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to "Invalid image format")
                )
                return@post
            }

            val result = ImageService.analyzeImage(bufferedImage)
            call.respond(mapOf("success" to true, "analysis" to result))
        } catch (e: Exception) {
            call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to (e.message ?: "Unknown error"))
            )
        }
    }

    // ==================== AI/SMART ANALİZ ENDPOINTLERİ ====================

    // Ana AI analiz endpoint'i - tüm özellikleri içerir
    post("/smart-analyze") {
        val startTime = System.currentTimeMillis()
        try {
            val request = call.receive<SmartAnalysisRequest>()
            logger.info("Smart analyzing image: ${request.filename}")

            val bufferedImage = ImageService.decodeBase64Image(request.imageBase64)

            if (bufferedImage == null) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    SmartAnalysisResponse(
                        success = false,
                        filename = request.filename,
                        error = "Invalid image format or corrupted data"
                    )
                )
                return@post
            }

            // Boyut kontrolü
            if (!ImageService.validateImageSize(bufferedImage)) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    SmartAnalysisResponse(
                        success = false,
                        filename = request.filename,
                        error = "Image too large. Maximum dimensions: 4096x4096"
                    )
                )
                return@post
            }

            // AI analizi yap
            val analysisResult = AIAnalysisService.analyzeImage(
                img = bufferedImage,
                enableVehicle = request.enableVehicleDetection,
                enableCrowd = request.enableCrowdAnalysis,
                enableAirQuality = request.enableAirQuality,
                enableTraffic = request.enableTrafficAnalysis
            )

            val processingTime = System.currentTimeMillis() - startTime
            logger.info("Smart analysis completed in ${processingTime}ms for ${request.filename}")

            call.respond(
                SmartAnalysisResponse(
                    success = true,
                    filename = request.filename,
                    basicAnalysis = analysisResult.basicAnalysis,
                    vehicleDetection = analysisResult.vehicleDetection,
                    crowdAnalysis = analysisResult.crowdAnalysis,
                    airQualityEstimate = analysisResult.airQualityEstimate,
                    trafficAnalysis = analysisResult.trafficAnalysis,
                    processingTimeMs = processingTime
                )
            )
        } catch (e: Exception) {
            logger.error("Error in smart analysis: ${e.message}", e)
            call.respond(
                HttpStatusCode.BadRequest,
                SmartAnalysisResponse(
                    success = false,
                    filename = "unknown",
                    error = e.message ?: "Unknown error",
                    processingTimeMs = System.currentTimeMillis() - startTime
                )
            )
        }
    }

    // Sadece araç tespiti
    post("/detect-vehicles") {
        try {
            val request = call.receive<ImageAnalysisRequest>()
            logger.info("Detecting vehicles in: ${request.filename}")

            val bufferedImage = ImageService.decodeBase64Image(request.imageBase64)
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Invalid image format")
                )

            val result = AIAnalysisService.detectVehicles(bufferedImage)
            call.respond(mapOf("success" to true, "vehicleDetection" to result))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Error"))
        }
    }

    // Sadece kalabalık analizi
    post("/analyze-crowd") {
        try {
            val request = call.receive<ImageAnalysisRequest>()
            logger.info("Analyzing crowd in: ${request.filename}")

            val bufferedImage = ImageService.decodeBase64Image(request.imageBase64)
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Invalid image format")
                )

            val result = AIAnalysisService.analyzeCrowd(bufferedImage)
            call.respond(mapOf("success" to true, "crowdAnalysis" to result))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Error"))
        }
    }

    // Sadece hava kalitesi tahmini
    post("/estimate-air-quality") {
        try {
            val request = call.receive<ImageAnalysisRequest>()
            logger.info("Estimating air quality from: ${request.filename}")

            val bufferedImage = ImageService.decodeBase64Image(request.imageBase64)
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Invalid image format")
                )

            val result = AIAnalysisService.estimateAirQuality(bufferedImage)
            call.respond(mapOf("success" to true, "airQuality" to result))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Error"))
        }
    }

    // Sadece trafik analizi
    post("/analyze-traffic") {
        try {
            val request = call.receive<ImageAnalysisRequest>()
            logger.info("Analyzing traffic in: ${request.filename}")

            val bufferedImage = ImageService.decodeBase64Image(request.imageBase64)
                ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Invalid image format")
                )

            val result = AIAnalysisService.analyzeTraffic(bufferedImage)
            call.respond(mapOf("success" to true, "trafficAnalysis" to result))
        } catch (e: Exception) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Error"))
        }
    }
}

