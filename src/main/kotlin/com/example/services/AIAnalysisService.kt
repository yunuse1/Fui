package com.example.services

import com.example.models.*
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * AI-based image analysis service
 * Analyzes vehicles, crowds, air quality and traffic using image processing techniques
 */
object AIAnalysisService {

    // ==================== MAIN ANALYSIS FUNCTION ====================

    /**
     * Analyzes image with all AI modules
     */
    fun analyzeImage(
        img: BufferedImage,
        enableVehicle: Boolean = true,
        enableCrowd: Boolean = true,
        enableAirQuality: Boolean = true,
        enableTraffic: Boolean = true
    ): SmartAnalysisComponents {
        val basicAnalysis = ImageService.analyzeImage(img)

        return SmartAnalysisComponents(
            basicAnalysis = basicAnalysis,
            vehicleDetection = if (enableVehicle) detectVehicles(img) else null,
            crowdAnalysis = if (enableCrowd) analyzeCrowd(img) else null,
            airQualityEstimate = if (enableAirQuality) estimateAirQuality(img) else null,
            trafficAnalysis = if (enableTraffic) analyzeTraffic(img) else null
        )
    }

    // ==================== VEHICLE DETECTION ====================

    /**
     * Detects vehicles in the image
     * Uses edge detection and color analysis to identify vehicle-like regions
     */
    fun detectVehicles(img: BufferedImage): VehicleDetectionResult {
        val w = img.width
        val h = img.height

        // Divide image into regions for analysis
        val regions = detectObjectRegions(img)

        // Araç benzeri bölgeleri filtrele (boyut ve renk bazlı)
        val vehicleLikeRegions = regions.filter { region ->
            val aspectRatio = region.width.toDouble() / region.height.toDouble()
            // Araçlar genellikle yatay dikdörtgen şeklinde
            aspectRatio in 0.5..4.0 &&
                    region.width > w * 0.02 && // minimum genişlik
                    region.height > h * 0.02 && // minimum yükseklik
                    region.width < w * 0.5 // maksimum genişlik (çok büyük olmasın)
        }

        // Araç tiplerini renk ve boyut bazlı tahmin et
        var busCount = 0
        var carCount = 0
        var truckCount = 0
        var motorcycleCount = 0
        var bicycleCount = 0

        vehicleLikeRegions.forEach { region ->
            val avgColor = getRegionAverageColor(img, region)
            val aspectRatio = region.width.toDouble() / region.height.toDouble()
            val relativeSize = (region.width * region.height).toDouble() / (w * h)

            when {
                // Otobüs: Büyük, uzun, genellikle beyaz/sarı/yeşil
                relativeSize > 0.01 && aspectRatio > 2.0 -> busCount++

                // Kamyon: Büyük, kare'ye yakın
                relativeSize > 0.008 && aspectRatio in 1.0..2.0 -> truckCount++

                // Motorsiklet/Bisiklet: Küçük, dar
                relativeSize < 0.003 && aspectRatio < 1.5 -> {
                    if (relativeSize < 0.001) bicycleCount++ else motorcycleCount++
                }

                // Araba: Orta boy
                else -> carCount++
            }
        }

        val totalVehicles = busCount + carCount + truckCount + motorcycleCount + bicycleCount

        // Yoğunluk hesapla
        val vehicleDensity = when {
            totalVehicles == 0 -> "none"
            totalVehicles < 5 -> "low"
            totalVehicles < 15 -> "medium"
            totalVehicles < 30 -> "high"
            else -> "very_high"
        }

        // Confidence score: Based on number and quality of detected regions
        val confidence = (0.3 + (vehicleLikeRegions.size.coerceAtMost(20) * 0.035)).coerceAtMost(0.95)

        return VehicleDetectionResult(
            totalVehicles = totalVehicles,
            busCount = busCount,
            carCount = carCount,
            truckCount = truckCount,
            motorcycleCount = motorcycleCount,
            bicycleCount = bicycleCount,
            vehicleDensity = vehicleDensity,
            confidence = confidence
        )
    }

    // ==================== CROWD ANALYSIS ====================

    /**
     * Analyzes crowd/human density in the image
     */
    fun analyzeCrowd(img: BufferedImage): CrowdAnalysisResult {
        val w = img.width
        val h = img.height

        // Cilt tonu benzeri pikselleri tespit et
        var skinTonePixels = 0
        var totalPixels = 0

        // Alt yarıyı analiz et (insanlar genellikle alt kısımda)
        val startY = h / 3
        for (y in startY until h) {
            for (x in 0 until w) {
                val rgb = img.getRGB(x, y)
                val c = Color(rgb, true)
                if (isSkinTone(c)) {
                    skinTonePixels++
                }
                totalPixels++
            }
        }

        val skinRatio = skinTonePixels.toDouble() / totalPixels

        // Hareket/kenar yoğunluğunu hesapla
        val edgeDensity = calculateEdgeDensity(img)

        // Tahmini insan sayısı (kabaca)
        val estimatedPeople = when {
            skinRatio < 0.001 -> 0
            skinRatio < 0.005 -> (skinRatio * 1000).toInt().coerceIn(1, 5)
            skinRatio < 0.02 -> (skinRatio * 500).toInt().coerceIn(5, 20)
            skinRatio < 0.05 -> (skinRatio * 400).toInt().coerceIn(20, 50)
            else -> (skinRatio * 300).toInt().coerceIn(50, 200)
        }

        // Yoğunluk seviyesi
        val densityLevel = when {
            estimatedPeople == 0 -> "empty"
            estimatedPeople < 5 -> "low"
            estimatedPeople < 20 -> "medium"
            estimatedPeople < 50 -> "high"
            else -> "overcrowded"
        }

        // Dağılım analizi (kenar varyansına göre)
        val distribution = when {
            edgeDensity < 0.1 -> "sparse"
            edgeDensity > 0.3 -> "clustered"
            else -> "uniform"
        }

        val densityPercentage = (skinRatio * 100 * 10).coerceIn(0.0, 100.0)
        val confidence = (0.4 + skinRatio * 5).coerceIn(0.3, 0.85)

        return CrowdAnalysisResult(
            estimatedPeopleCount = estimatedPeople,
            densityLevel = densityLevel,
            densityPercentage = densityPercentage,
            crowdDistribution = distribution,
            confidence = confidence
        )
    }

    // ==================== AIR QUALITY ESTIMATION ====================

    /**
     * Estimates air quality from the image
     * Analyzes visibility, fog, smoke and other factors
     */
    fun estimateAirQuality(img: BufferedImage): AirQualityResult {
        val w = img.width
        val h = img.height

        // Üst 1/3'ü gökyüzü olarak analiz et
        val skyRegionHeight = h / 3
        var skyBrightness = 0.0
        var skyBlueRatio = 0.0
        var grayPixels = 0
        var hazyPixels = 0
        var skyPixelCount = 0

        for (y in 0 until skyRegionHeight) {
            for (x in 0 until w) {
                val rgb = img.getRGB(x, y)
                val c = Color(rgb, true)
                val r = c.red
                val g = c.green
                val b = c.blue

                val brightness = (0.299 * r + 0.587 * g + 0.114 * b)
                skyBrightness += brightness

                // Mavi gökyüzü kontrolü
                if (b > r && b > g && b > 100) {
                    skyBlueRatio++
                }

                // Gri/sisli piksel kontrolü
                val maxDiff = maxOf(abs(r - g), abs(g - b), abs(r - b))
                if (maxDiff < 30 && brightness > 150) {
                    grayPixels++
                    if (brightness > 180) hazyPixels++
                }

                skyPixelCount++
            }
        }

        val avgSkyBrightness = skyBrightness / skyPixelCount
        val blueRatio = skyBlueRatio / skyPixelCount
        val grayRatio = grayPixels.toDouble() / skyPixelCount
        val hazeRatio = hazyPixels.toDouble() / skyPixelCount

        // Kontrast analizi (görünürlük için)
        val contrast = calculateImageContrast(img)

        // Haze seviyesi
        val hazeLevel = (grayRatio * 0.5 + hazeRatio * 0.3 + (1 - contrast) * 0.2).coerceIn(0.0, 1.0)

        // Görünürlük skoru
        val visibilityScore = (contrast * 0.6 + blueRatio * 0.2 + (1 - hazeLevel) * 0.2).coerceIn(0.0, 1.0)

        // Kirlilik göstergeleri
        val smogDetected = hazeLevel > 0.4 && avgSkyBrightness < 200
        val dustDetected = grayRatio > 0.3 && avgSkyBrightness > 150
        val fogDetected = hazeLevel > 0.5 && avgSkyBrightness > 200
        val clearSky = blueRatio > 0.3 && hazeLevel < 0.2

        // AQI tahmini
        val (aqiCategory, aqiValue) = when {
            clearSky && visibilityScore > 0.8 -> "good" to 25
            visibilityScore > 0.6 && hazeLevel < 0.3 -> "moderate" to 75
            visibilityScore > 0.4 && hazeLevel < 0.5 -> "unhealthy_sensitive" to 125
            visibilityScore > 0.2 && hazeLevel < 0.7 -> "unhealthy" to 175
            hazeLevel < 0.85 -> "very_unhealthy" to 250
            else -> "hazardous" to 350
        }

        val confidence = (0.5 + blueRatio * 0.3 + (1 - hazeLevel) * 0.2).coerceIn(0.4, 0.9)

        return AirQualityResult(
            hazeLevel = hazeLevel,
            visibilityScore = visibilityScore,
            estimatedAQI = aqiCategory,
            aqiValue = aqiValue,
            pollutionIndicators = PollutionIndicators(
                smogDetected = smogDetected,
                dustDetected = dustDetected,
                fogDetected = fogDetected,
                clearSky = clearSky
            ),
            confidence = confidence
        )
    }

    // ==================== TRAFFIC ANALYSIS ====================

    /**
     * Analyzes traffic conditions in the image
     */
    fun analyzeTraffic(img: BufferedImage): TrafficAnalysisResult {
        // First detect vehicles
        val vehicleResult = detectVehicles(img)

        val w = img.width
        val h = img.height

        // Yol bölgesini analiz et (genellikle alt 2/3)
        val roadRegionStart = h / 3
        val roadRegion = img.getSubimage(0, roadRegionStart, w, h - roadRegionStart)

        // Yol yüzeyi renk analizi (asfalt genellikle gri)
        var roadPixels = 0
        var occupiedPixels = 0

        for (y in 0 until roadRegion.height) {
            for (x in 0 until roadRegion.width) {
                val rgb = roadRegion.getRGB(x, y)
                val c = Color(rgb, true)
                val brightness = (0.299 * c.red + 0.587 * c.green + 0.114 * c.blue)
                val maxDiff = maxOf(abs(c.red - c.green), abs(c.green - c.blue), abs(c.red - c.blue))

                // Gri tonları (yol yüzeyi)
                if (maxDiff < 40) {
                    roadPixels++
                    // Dark gray = empty road, light gray/colored = vehicle
                    if (brightness > 80 || maxDiff > 20) {
                        occupiedPixels++
                    }
                }
            }
        }

        // Yol doluluk oranı
        val roadOccupancy = if (roadPixels > 0) {
            (occupiedPixels.toDouble() / roadPixels).coerceIn(0.0, 1.0)
        } else {
            vehicleResult.totalVehicles.toDouble() / 30.0 // alternatif hesaplama
        }.coerceIn(0.0, 1.0)

        // Tıkanıklık seviyesi
        val congestionLevel = when {
            vehicleResult.totalVehicles == 0 && roadOccupancy < 0.1 -> "free_flow"
            vehicleResult.totalVehicles < 5 || roadOccupancy < 0.2 -> "light"
            vehicleResult.totalVehicles < 15 || roadOccupancy < 0.4 -> "moderate"
            vehicleResult.totalVehicles < 25 || roadOccupancy < 0.7 -> "heavy"
            else -> "standstill"
        }

        val congestionPercentage = when (congestionLevel) {
            "free_flow" -> roadOccupancy * 100 * 0.2
            "light" -> 10 + roadOccupancy * 100 * 0.3
            "moderate" -> 30 + roadOccupancy * 100 * 0.3
            "heavy" -> 60 + roadOccupancy * 100 * 0.3
            else -> 90 + roadOccupancy * 10
        }.coerceIn(0.0, 100.0)

        val estimatedSpeed = when (congestionLevel) {
            "free_flow" -> "fast"
            "light" -> "normal"
            "moderate" -> "slow"
            "heavy" -> "very_slow"
            else -> "stopped"
        }

        // Olay tespiti (ani yoğunluk değişimi veya anormal patern)
        val incidentDetected = vehicleResult.vehicleDensity == "very_high" && roadOccupancy > 0.8

        val confidence = (vehicleResult.confidence * 0.6 + 0.4).coerceAtMost(0.9)

        return TrafficAnalysisResult(
            congestionLevel = congestionLevel,
            congestionPercentage = congestionPercentage,
            estimatedSpeed = estimatedSpeed,
            roadOccupancy = roadOccupancy,
            incidentDetected = incidentDetected,
            confidence = confidence
        )
    }

    // ==================== HELPER FUNCTIONS ====================

    /**
     * Detects object regions in the image (simple edge-based segmentation)
     */
    private fun detectObjectRegions(img: BufferedImage): List<ObjectRegion> {
        val w = img.width
        val h = img.height
        val regions = mutableListOf<ObjectRegion>()

        // Basitleştirilmiş grid-based bölge tespiti
        val gridSize = 20
        val gridW = w / gridSize
        val gridH = h / gridSize

        for (gy in 0 until gridH) {
            for (gx in 0 until gridW) {
                val startX = gx * gridSize
                val startY = gy * gridSize
                val endX = minOf(startX + gridSize, w)
                val endY = minOf(startY + gridSize, h)

                // Grid bölgesinin kenar yoğunluğunu hesapla
                var edgeCount = 0
                var avgBrightness = 0.0
                var pixelCount = 0

                for (y in startY until endY - 1) {
                    for (x in startX until endX - 1) {
                        val c1 = Color(img.getRGB(x, y))
                        val c2 = Color(img.getRGB(x + 1, y))
                        val c3 = Color(img.getRGB(x, y + 1))

                        val diff1 = colorDifference(c1, c2)
                        val diff2 = colorDifference(c1, c3)

                        if (diff1 > 30 || diff2 > 30) edgeCount++

                        avgBrightness += (0.299 * c1.red + 0.587 * c1.green + 0.114 * c1.blue)
                        pixelCount++
                    }
                }

                avgBrightness /= pixelCount
                val edgeDensity = edgeCount.toDouble() / pixelCount

                // Yüksek kenar yoğunluğu olan bölgeler nesne olabilir
                if (edgeDensity > 0.15 && avgBrightness > 30) {
                    regions.add(ObjectRegion(startX, startY, gridSize, gridSize))
                }
            }
        }

        // Bitişik bölgeleri birleştir
        return mergeAdjacentRegions(regions, gridSize)
    }

    /**
     * Bitişik bölgeleri birleştirir
     */
    private fun mergeAdjacentRegions(regions: List<ObjectRegion>, gridSize: Int): List<ObjectRegion> {
        if (regions.isEmpty()) return emptyList()

        val merged = mutableListOf<ObjectRegion>()
        val used = BooleanArray(regions.size)

        for (i in regions.indices) {
            if (used[i]) continue

            var minX = regions[i].x
            var minY = regions[i].y
            var maxX = regions[i].x + regions[i].width
            var maxY = regions[i].y + regions[i].height

            for (j in i + 1 until regions.size) {
                if (used[j]) continue

                val r = regions[j]
                // Bitişik mi kontrol et
                if (abs(r.x - maxX) <= gridSize && abs(r.y - minY) <= gridSize * 2) {
                    minX = minOf(minX, r.x)
                    minY = minOf(minY, r.y)
                    maxX = maxOf(maxX, r.x + r.width)
                    maxY = maxOf(maxY, r.y + r.height)
                    used[j] = true
                }
            }

            merged.add(ObjectRegion(minX, minY, maxX - minX, maxY - minY))
            used[i] = true
        }

        return merged
    }

    /**
     * Bölgenin ortalama rengini hesaplar
     */
    private fun getRegionAverageColor(img: BufferedImage, region: ObjectRegion): Color {
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0

        val endX = minOf(region.x + region.width, img.width)
        val endY = minOf(region.y + region.height, img.height)

        for (y in region.y until endY) {
            for (x in region.x until endX) {
                val c = Color(img.getRGB(x, y))
                rSum += c.red
                gSum += c.green
                bSum += c.blue
                count++
            }
        }

        return if (count > 0) {
            Color((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt())
        } else {
            Color.BLACK
        }
    }

    /**
     * Cilt tonu kontrolü
     */
    private fun isSkinTone(c: Color): Boolean {
        val r = c.red
        val g = c.green
        val b = c.blue

        // Basit cilt tonu tespiti (çeşitli ten renkleri için)
        return r > 60 && g > 40 && b > 20 &&
                r > g && r > b &&
                abs(r - g) > 10 &&
                r - b > 15 && r - b < 170 &&
                r < 250 && g < 230 && b < 210
    }

    /**
     * Kenar yoğunluğunu hesaplar
     */
    private fun calculateEdgeDensity(img: BufferedImage): Double {
        val w = img.width
        val h = img.height
        var edgeCount = 0
        var totalPixels = 0

        for (y in 0 until h - 1 step 2) {
            for (x in 0 until w - 1 step 2) {
                val c1 = Color(img.getRGB(x, y))
                val c2 = Color(img.getRGB(x + 1, y))
                val diff = colorDifference(c1, c2)
                if (diff > 25) edgeCount++
                totalPixels++
            }
        }

        return edgeCount.toDouble() / totalPixels
    }

    /**
     * Calculates image contrast
     */
    private fun calculateImageContrast(img: BufferedImage): Double {
        val w = img.width
        val h = img.height
        val brightnesses = mutableListOf<Double>()

        for (y in 0 until h step 4) {
            for (x in 0 until w step 4) {
                val c = Color(img.getRGB(x, y))
                val brightness = 0.299 * c.red + 0.587 * c.green + 0.114 * c.blue
                brightnesses.add(brightness)
            }
        }

        if (brightnesses.isEmpty()) return 0.5

        val mean = brightnesses.average()
        val variance = brightnesses.map { (it - mean) * (it - mean) }.average()
        val stdDev = sqrt(variance)

        // Normalize: high standard deviation = high contrast
        return (stdDev / 128.0).coerceIn(0.0, 1.0)
    }

    /**
     * Calculates difference between two colors
     */
    private fun colorDifference(c1: Color, c2: Color): Int {
        return abs(c1.red - c2.red) + abs(c1.green - c2.green) + abs(c1.blue - c2.blue)
    }

    // ==================== DATA CLASSES ====================

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
}

