package com.example.models

import kotlinx.serialization.Serializable

// ============== Ingest Models ==============

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

// ============== Health Models ==============

@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val version: String
)

// ============== Generic Response Models ==============

@Serializable
data class ErrorResponse(
    val error: String
)

@Serializable
data class ServerInfoResponse(
    val message: String,
    val version: String
)

// ============== AI/Smart Analysis Models ==============

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

// Araç Tespiti Sonucu
@Serializable
data class VehicleDetectionResult(
    val totalVehicles: Int,
    val busCount: Int,
    val carCount: Int,
    val truckCount: Int,
    val motorcycleCount: Int,
    val bicycleCount: Int,
    val vehicleDensity: String,  // "low", "medium", "high", "very_high"
    val confidence: Double       // 0.0 - 1.0
)

// Kalabalık/Yoğunluk Analizi
@Serializable
data class CrowdAnalysisResult(
    val estimatedPeopleCount: Int,
    val densityLevel: String,        // "empty", "low", "medium", "high", "overcrowded"
    val densityPercentage: Double,   // 0-100
    val crowdDistribution: String,   // "uniform", "clustered", "sparse"
    val confidence: Double
)

// Hava Kalitesi Tahmini
@Serializable
data class AirQualityResult(
    val hazeLevel: Double,           // 0.0 - 1.0 (0=temiz, 1=çok sisli)
    val visibilityScore: Double,     // 0.0 - 1.0 (0=görünmez, 1=çok net)
    val estimatedAQI: String,        // "good", "moderate", "unhealthy_sensitive", "unhealthy", "very_unhealthy", "hazardous"
    val aqiValue: Int,               // 0-500 arası tahmini AQI değeri
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

// Trafik Analizi
@Serializable
data class TrafficAnalysisResult(
    val congestionLevel: String,     // "free_flow", "light", "moderate", "heavy", "standstill"
    val congestionPercentage: Double, // 0-100
    val estimatedSpeed: String,      // "fast", "normal", "slow", "very_slow", "stopped"
    val roadOccupancy: Double,       // 0.0 - 1.0
    val incidentDetected: Boolean,
    val confidence: Double
)

// Genel Şehir Durumu Özeti
@Serializable
data class CityStatusSummary(
    val overallScore: Int,           // 0-100 arası genel şehir durumu
    val trafficStatus: String,
    val airQualityStatus: String,
    val crowdStatus: String,
    val recommendations: List<String>,
    val timestamp: Long
)

// ============== Database & Test Response Models ==============

@Serializable
data class DbStatusResponse(
    val databaseConnected: Boolean,
    val status: String,
    val databaseUrl: String? = null
)

@Serializable
data class TestAnalyzeResponse(
    val success: Boolean,
    val message: String? = null,
    val savedToDatabase: Boolean = false,
    val databaseId: Int? = null,
    val databaseConnected: Boolean = false,
    val basicAnalysis: ImageAnalysisResult? = null,
    val vehicleDetection: VehicleDetectionResult? = null,
    val crowdAnalysis: CrowdAnalysisResult? = null,
    val airQuality: AirQualityResult? = null,
    val trafficAnalysis: TrafficAnalysisResult? = null,
    val processingTimeMs: Long = 0,
    val error: String? = null
)

@Serializable
data class RecordsResponse(
    val success: Boolean,
    val count: Int,
    val records: List<AnalysisRecordDto>
)

@Serializable
data class AnalysisRecordDto(
    val id: Int,
    val timestamp: String,
    val filename: String,
    val deviceId: String? = null,
    val location: String? = null,
    val totalVehicles: Int = 0,
    val busCount: Int = 0,
    val carCount: Int = 0,
    val estimatedPeople: Int = 0,
    val crowdDensityLevel: String? = null,
    val aqiValue: Int = 0,
    val aqiCategory: String? = null,
    val congestionLevel: String? = null,
    val congestionPercentage: Double = 0.0
)

@Serializable
data class StatisticsResponse(
    val success: Boolean,
    val periodHours: Int,
    val statistics: StatisticsDto
)

@Serializable
data class StatisticsDto(
    val totalRecords: Int = 0,
    val avgVehicles: Double = 0.0,
    val avgPeople: Double = 0.0,
    val avgAqi: Double = 0.0,
    val avgCongestion: Double = 0.0,
    val totalBuses: Int = 0,
    val totalCars: Int = 0,
    val incidentCount: Int = 0
)

