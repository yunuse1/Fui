package com.example.models

import kotlinx.serialization.Serializable


@Serializable
data class IngestPayload(
    val deviceId: String,
    val timestamp: Long,
    val metrics: Map<String, Double> = emptyMap()
)

@Serializable
data class IngestResponse(
    val received: Boolean,
    val deviceId: String
)

// ============== Image Analysis Models ==============

@Serializable
data class ImageAnalysisRequest(
    val imageBase64: String,
    val filename: String = "unknown"
)

@Serializable
data class ImageAnalysisResult(
    val width: Int,
    val height: Int,
    val avgRed: Int,
    val avgGreen: Int,
    val avgBlue: Int,
    val avgBrightness: Double,
    val histogram: Map<String, Int>
)

@Serializable
data class ImageAnalysisResponse(
    val success: Boolean,
    val analysis: ImageAnalysisResult? = null,
    val error: String? = null
)


@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val version: String
)


@Serializable
data class ErrorResponse(
    val error: String
)

@Serializable
data class ServerInfoResponse(
    val message: String,
    val version: String
)


@Serializable
data class SmartAnalysisRequest(
    val imageBase64: String,
    val filename: String = "unknown",
    val enableVehicleDetection: Boolean = true,
    val enableCrowdAnalysis: Boolean = true,
    val enableAirQuality: Boolean = true,
    val enableTrafficAnalysis: Boolean = true
)

@Serializable
data class SmartAnalysisResponse(
    val success: Boolean,
    val filename: String,
    val basicAnalysis: ImageAnalysisResult? = null,
    val vehicleDetection: VehicleDetectionResult? = null,
    val crowdAnalysis: CrowdAnalysisResult? = null,
    val airQualityEstimate: AirQualityResult? = null,
    val trafficAnalysis: TrafficAnalysisResult? = null,
    val processingTimeMs: Long = 0,
    val error: String? = null
)

@Serializable
data class VehicleDetectionResult(
    val totalVehicles: Int,
    val busCount: Int,
    val carCount: Int,
    val truckCount: Int,
    val motorcycleCount: Int,
    val bicycleCount: Int,
    val vehicleDensity: String,
    val confidence: Double
)

@Serializable
data class CrowdAnalysisResult(
    val estimatedPeopleCount: Int,
    val densityLevel: String,
    val densityPercentage: Double,
    val crowdDistribution: String,
    val confidence: Double
)

@Serializable
data class AirQualityResult(
    val hazeLevel: Double,
    val visibilityScore: Double,
    val estimatedAQI: String,
    val aqiValue: Int,
    val pollutionIndicators: PollutionIndicators,
    val confidence: Double
)

@Serializable
data class PollutionIndicators(
    val smogDetected: Boolean,
    val dustDetected: Boolean,
    val fogDetected: Boolean,
    val clearSky: Boolean
)


@Serializable
data class TrafficAnalysisResult(
    val congestionLevel: String,
    val congestionPercentage: Double,
    val estimatedSpeed: String,
    val roadOccupancy: Double,
    val incidentDetected: Boolean,
    val confidence: Double
)

@Serializable
data class CityStatusSummary(
    val overallScore: Int,
    val trafficStatus: String,
    val airQualityStatus: String,
    val crowdStatus: String,
    val recommendations: List<String>,
    val timestamp: Long
)

