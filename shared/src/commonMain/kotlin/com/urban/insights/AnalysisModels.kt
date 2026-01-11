package com.urban.insights

/**
 * Represents the result of a traffic and crowd analysis
 */
data class AnalysisResult(
    val vehicleCount: Int,
    val estimatedPeople: Int,
    val trafficLevel: TrafficLevel,
    val crowdLevel: CrowdLevel,
    val sceneType: SceneType,
    val confidence: Double,
    val timestamp: Long
)

/**
 * Traffic density levels
 */
enum class TrafficLevel(val emoji: String, val description: String) {
    EMPTY("⚪", "No vehicles detected"),
    LOW("🟢", "Light traffic"),
    MEDIUM("🟡", "Moderate traffic"),
    HIGH("🟠", "Heavy traffic"),
    VERY_HIGH("🔴", "Severe congestion"),
    INDOOR("🏛️", "Indoor scene - no traffic analysis");

    override fun toString(): String = "$description $emoji"
}

/**
 * Crowd density levels
 */
enum class CrowdLevel(val emoji: String, val description: String) {
    EMPTY("⚪", "No people detected"),
    VERY_LOW("🟢", "Very few people"),
    LOW("🟢", "Light crowd"),
    MEDIUM("🟡", "Moderate crowd"),
    HIGH("🟠", "Dense crowd"),
    VERY_HIGH("🔴", "Very dense crowd");

    override fun toString(): String = "$description $emoji"
}

/**
 * Types of scenes that can be detected
 */
enum class SceneType(val emoji: String, val description: String) {
    TRAFFIC("🚗", "Traffic/Road Scene"),
    NATURE("🌳", "Nature/Park"),
    INDOOR_HISTORIC("🏛️", "Indoor (Historic Building)"),
    INDOOR("🏠", "Indoor"),
    OUTDOOR("🏙️", "Outdoor (No Traffic)"),
    UNKNOWN("❓", "Unknown");

    override fun toString(): String = "$emoji $description"
}

