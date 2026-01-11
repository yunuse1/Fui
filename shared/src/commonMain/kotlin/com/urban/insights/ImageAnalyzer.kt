package com.urban.insights

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Platform-agnostic image analysis algorithms for traffic and crowd detection.
 * This class contains the core logic shared between Android and Web platforms.
 */
object ImageAnalyzer {

    /**
     * Determines traffic level based on vehicle count
     */
    fun determineTrafficLevel(vehicleCount: Int, isTrafficScene: Boolean): TrafficLevel {
        if (!isTrafficScene) return TrafficLevel.INDOOR

        return when {
            vehicleCount >= 12 -> TrafficLevel.VERY_HIGH
            vehicleCount >= 6 -> TrafficLevel.HIGH
            vehicleCount >= 3 -> TrafficLevel.MEDIUM
            vehicleCount >= 1 -> TrafficLevel.LOW
            else -> TrafficLevel.EMPTY
        }
    }

    /**
     * Determines crowd level based on estimated people count
     */
    fun determineCrowdLevel(peopleCount: Int): CrowdLevel {
        return when {
            peopleCount >= 30 -> CrowdLevel.VERY_HIGH
            peopleCount >= 15 -> CrowdLevel.HIGH
            peopleCount >= 8 -> CrowdLevel.MEDIUM
            peopleCount >= 3 -> CrowdLevel.LOW
            peopleCount >= 1 -> CrowdLevel.VERY_LOW
            else -> CrowdLevel.EMPTY
        }
    }

    /**
     * Classifies scene type based on analysis parameters
     */
    fun classifyScene(
        skyRatio: Double,
        roadRatio: Double,
        greenRatio: Double,
        brownRatio: Double,
        avgBrightness: Int,
        colorVariety: Double
    ): SceneType {
        val isOutdoor = skyRatio > 0.15 || (avgBrightness > 100 && roadRatio > 0.1)
        val isIndoor = !isOutdoor && (brownRatio > 0.2 || colorVariety < 3)

        val isTrafficScene = (skyRatio > 0.1 || avgBrightness > 120) &&
                            roadRatio > 0.08 &&
                            brownRatio < 0.25 &&
                            greenRatio < 0.4

        return when {
            isTrafficScene -> SceneType.TRAFFIC
            greenRatio > 0.3 -> SceneType.NATURE
            isIndoor && brownRatio > 0.2 -> SceneType.INDOOR_HISTORIC
            isIndoor -> SceneType.INDOOR
            isOutdoor -> SceneType.OUTDOOR
            else -> SceneType.UNKNOWN
        }
    }

    /**
     * Calculates scene confidence based on analysis parameters
     */
    fun calculateConfidence(
        sceneType: SceneType,
        skyRatio: Double,
        roadRatio: Double
    ): Double {
        return when {
            sceneType == SceneType.TRAFFIC && skyRatio > 0.2 && roadRatio > 0.15 -> 0.9
            sceneType == SceneType.TRAFFIC && roadRatio > 0.1 -> 0.7
            sceneType == SceneType.TRAFFIC -> 0.5
            else -> 0.3
        }
    }

    /**
     * Calculates gradient magnitude for edge detection
     */
    fun calculateGradient(left: Int, right: Int, top: Int, bottom: Int): Double {
        val dx = right - left
        val dy = bottom - top
        return sqrt((dx * dx + dy * dy).toDouble())
    }

    /**
     * Checks if RGB values represent a skin tone
     */
    fun isSkinTone(r: Int, g: Int, b: Int): Boolean {
        return r > 95 && g > 40 && b > 20 &&
               r > g && r > b &&
               abs(r - g) > 15 &&
               r - g < 100 && r - b < 100
    }

    /**
     * Checks if a pixel might belong to a vehicle based on color analysis
     */
    fun isVehiclePixel(
        r: Int, g: Int, b: Int,
        avgRoadBrightness: Int
    ): Boolean {
        val brightness = (r + g + b) / 3
        val saturation = maxOf(r, g, b) - minOf(r, g, b)
        val diffFromRoad = abs(brightness - avgRoadBrightness)

        return when {
            diffFromRoad < 25 -> false
            // White vehicle
            brightness > 200 && saturation < 30 && diffFromRoad > 40 -> true
            // Dark vehicle
            brightness < 45 && saturation < 20 && diffFromRoad > 30 -> true
            // Red vehicle
            r > 140 && r > g + 50 && r > b + 50 -> true
            // Blue vehicle
            b > 120 && b > r + 40 && b > g + 20 -> true
            // Silver/gray vehicle
            brightness in 150..200 && saturation < 25 && diffFromRoad > 35 -> true
            // Yellow/orange vehicle
            r > 180 && g > 120 && b < 100 && saturation > 60 -> true
            else -> false
        }
    }

    /**
     * Checks if a pixel represents sky
     */
    fun isSkyPixel(r: Int, g: Int, b: Int): Boolean {
        // Blue sky
        if (b > 150 && b > r + 20 && b > g - 30 && g > 100) return true
        // Cloudy/white sky
        if (r > 180 && g > 180 && b > 180 && abs(r - g) < 30 && abs(g - b) < 30) return true
        return false
    }

    /**
     * Checks if a pixel represents road surface
     */
    fun isRoadPixel(r: Int, g: Int, b: Int): Boolean {
        val brightness = (r + g + b) / 3
        val saturation = maxOf(r, g, b) - minOf(r, g, b)
        return brightness in 40..120 && saturation < 40
    }

    /**
     * Validates blob dimensions for vehicle detection
     */
    fun isValidVehicleBlob(
        blobSize: Int,
        blobWidth: Int,
        blobHeight: Int,
        minSize: Int = 8,
        maxSize: Int = 80
    ): Boolean {
        if (blobSize !in minSize..maxSize) return false

        val aspectRatio = if (blobHeight > 0) blobWidth.toFloat() / blobHeight else 0f
        return aspectRatio in 0.3f..5f && blobWidth >= 2 && blobHeight >= 2
    }
}

