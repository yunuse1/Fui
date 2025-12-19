package com.example.models
data class ObjectRegion(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

data class SmartAnalysisComponents(
    val basicAnalysis: ImageAnalysisResult,
    val vehicleDetection: VehicleDetectionResult?,
    val crowdAnalysis: CrowdAnalysisResult?,
    val airQualityEstimate: AirQualityResult?,
    val trafficAnalysis: TrafficAnalysisResult?
)