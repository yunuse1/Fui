package com.example.webapp.service

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.sqrt
// Import shared module for cross-platform analysis logic
import com.urban.insights.ImageAnalyzer
import com.urban.insights.TrafficLevel
import com.urban.insights.CrowdLevel
import com.urban.insights.SceneType

data class AnalysisResult(
    val trafficLevel: String,
    val crowdLevel: String,
    val weather: String,
    val airQuality: String,
    val timeEstimate: String,
    val estimatedVehicles: Int,
    val estimatedPeople: Int,
    val fullReport: String,
    val imageData: ByteArray? = null
) {
    val vehicleCount: Int get() = estimatedVehicles
    val timeOfDay: String get() = timeEstimate
    val brightness: String get() = when {
        timeEstimate.contains("Night") -> "Low"
        timeEstimate.contains("Sunny") -> "High"
        else -> "Normal"
    }
}

class CameraAnalysisService {

    private val liveCameraUrls = listOf(
        "https://trafik.ibb.gov.tr/kamera/cam001.jpg",
        "https://www.meteo.be/services/camera/IRM_Uccle1.jpg",
        "https://www.trafficcam.eu/shot.jpg",
        "https://picsum.photos/640/480?random=traffic"
    )

    fun analyzeFromUrl(url: String): AnalysisResult {
        val image = fetchImageFromUrl(url)
        return if (image != null) analyzeImage(image) else analyzeDemo()
    }

    fun analyzeFromLiveCamera(): AnalysisResult {
        for (url in liveCameraUrls) {
            try {
                val image = fetchImageFromUrl(url)
                if (image != null) return analyzeImage(image)
            } catch (e: Exception) { continue }
        }
        return analyzeDemo()
    }

    fun analyzeOtogarKavsagi(): AnalysisResult {
        val image = createDemoImage()
        return analyzeImage(image)
    }

    fun analyzeDemo(): AnalysisResult {
        val image = createDemoImage()
        return analyzeImage(image)
    }

    fun analyzeFromImageData(imageData: ByteArray): AnalysisResult {
        return try {
            val image = ImageIO.read(ByteArrayInputStream(imageData))
            if (image != null) analyzeImage(image, imageData) else analyzeDemo()
        } catch (e: Exception) {
            e.printStackTrace()
            analyzeDemo()
        }
    }

    private fun fetchImageFromUrl(urlString: String): BufferedImage? {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.setRequestProperty("Accept", "image/*")
            connection.instanceFollowRedirects = true

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val contentType = connection.contentType ?: ""
                val inputStream = connection.inputStream

                if (contentType.contains("multipart") || urlString.contains("mjpg")) {
                    val buffer = ByteArray(100000)
                    var totalRead = 0
                    var foundStart = false
                    var startIdx = 0

                    while (totalRead < buffer.size - 1) {
                        val b = inputStream.read()
                        if (b == -1) break
                        buffer[totalRead] = b.toByte()

                        if (!foundStart && totalRead > 0 &&
                            buffer[totalRead - 1] == 0xFF.toByte() && buffer[totalRead] == 0xD8.toByte()) {
                            foundStart = true
                            startIdx = totalRead - 1
                        }

                        if (foundStart && totalRead > startIdx + 2 &&
                            buffer[totalRead - 1] == 0xFF.toByte() && buffer[totalRead] == 0xD9.toByte()) {
                            val jpegData = buffer.copyOfRange(startIdx, totalRead + 1)
                            inputStream.close()
                            return ImageIO.read(ByteArrayInputStream(jpegData))
                        }
                        totalRead++
                    }
                    inputStream.close()
                    null
                } else {
                    val image = ImageIO.read(inputStream)
                    inputStream.close()
                    image
                }
            } else null
        } catch (e: Exception) { null }
    }

    private fun createDemoImage(): BufferedImage {
        val width = 640
        val height = 480
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        val random = Random()

        val hour = LocalDateTime.now().hour
        val isNight = hour < 6 || hour >= 19

        val timeSeed = System.currentTimeMillis() / 1000
        random.setSeed(timeSeed)

        val trafficMultiplier = when (hour) {
            in 7..9 -> 2.5
            in 12..14 -> 1.5
            in 17..19 -> 2.8
            in 22..24, in 0..5 -> 0.3
            else -> 1.0
        }

        val baseVehicles = random.nextInt(8)
        val vehicleCount = maxOf(0, minOf(20, (baseVehicles * trafficMultiplier).toInt() + random.nextInt(5) - 2))

        val skyColor = if (isNight) Color(15, 15, 35) else Color(135, 180, 220)
        val roadColor = if (isNight) Color(30, 30, 35) else Color(60, 60, 65)

        g.color = skyColor
        g.fillRect(0, 0, width, height / 3)

        if (!isNight && random.nextBoolean()) {
            g.color = Color(255, 255, 255, 150)
            for (i in 0 until 3) {
                val cx = random.nextInt(width)
                val cy = random.nextInt(height / 4)
                g.fillOval(cx, cy, 80 + random.nextInt(60), 30 + random.nextInt(20))
            }
        }

        g.color = roadColor
        g.fillRect(0, height / 3, width, height * 2 / 3)

        g.color = Color(80, 80, 85)
        g.fillRect(0, height / 3, width, 15)
        g.fillRect(0, height - 20, width, 20)

        g.color = if (isNight) Color(100, 100, 80) else Color(230, 230, 210)
        for (y in height / 3 + 50 until height - 30 step 50) {
            g.fillRect(width / 3 - 2, y, 4, 25)
            g.fillRect(width * 2 / 3 - 2, y, 4, 25)
        }

        g.color = Color(230, 230, 210)
        g.fillRect(20, height / 3 + 20, 3, height * 2 / 3 - 40)
        g.fillRect(width - 23, height / 3 + 20, 3, height * 2 / 3 - 40)

        val vehicleColors = if (isNight) {
            arrayOf(Color(30, 30, 35), Color(45, 45, 50), Color(25, 25, 30), Color(60, 60, 65))
        } else {
            arrayOf(Color.WHITE, Color(220, 220, 220), Color(40, 40, 45),
                Color(180, 20, 20), Color(20, 20, 150), Color(150, 150, 160),
                Color(200, 180, 100), Color(100, 100, 100), Color(30, 80, 30))
        }

        data class Vehicle(val x: Int, val y: Int, val w: Int, val h: Int, val color: Color, val type: String)
        val vehicles = mutableListOf<Vehicle>()
        val lanes = listOf(width / 6, width / 2, width * 5 / 6)

        for (i in 0 until vehicleCount) {
            var attempts = 0
            var placed = false

            while (!placed && attempts < 20) {
                val lane = lanes[random.nextInt(lanes.size)]
                val vy = height / 3 + 40 + random.nextInt(height / 2)

                val vehicleType = when (random.nextInt(10)) {
                    in 0..5 -> "sedan"
                    in 6..7 -> "suv"
                    8 -> "truck"
                    else -> "bus"
                }

                val (vw, vh) = when (vehicleType) {
                    "sedan" -> Pair(45 + random.nextInt(15), 22 + random.nextInt(8))
                    "suv" -> Pair(55 + random.nextInt(15), 28 + random.nextInt(8))
                    "truck" -> Pair(70 + random.nextInt(30), 25 + random.nextInt(10))
                    "bus" -> Pair(90 + random.nextInt(20), 28 + random.nextInt(8))
                    else -> Pair(50, 25)
                }

                val vx = lane - vw / 2 + random.nextInt(30) - 15

                val overlaps = vehicles.any { v ->
                    val dx = kotlin.math.abs((vx + vw/2) - (v.x + v.w/2))
                    val dy = kotlin.math.abs((vy + vh/2) - (v.y + v.h/2))
                    dx < (vw + v.w) / 2 + 10 && dy < (vh + v.h) / 2 + 10
                }

                if (!overlaps && vx > 30 && vx + vw < width - 30) {
                    val color = if (vehicleType == "bus") Color(255, 180, 0)
                                else vehicleColors[random.nextInt(vehicleColors.size)]
                    vehicles.add(Vehicle(vx, vy, vw, vh, color, vehicleType))
                    placed = true
                }
                attempts++
            }
        }

        for (v in vehicles) {
            g.color = Color(0, 0, 0, 50)
            g.fillRect(v.x + 3, v.y + 3, v.w, v.h)
            g.color = v.color
            g.fillRoundRect(v.x, v.y, v.w, v.h, 5, 5)
            g.color = Color(100, 150, 200, if (isNight) 100 else 180)
            g.fillRect(v.x + v.w / 4, v.y + 2, v.w / 2, v.h / 3)

            if (isNight) {
                g.color = Color(255, 255, 200, 200)
                g.fillOval(v.x + 2, v.y + v.h / 2 - 3, 6, 6)
                g.fillOval(v.x + v.w - 8, v.y + v.h / 2 - 3, 6, 6)
                g.color = Color(255, 50, 50, 180)
                g.fillOval(v.x + v.w - 5, v.y + 2, 4, 4)
                g.fillOval(v.x + v.w - 5, v.y + v.h - 6, 4, 4)
            }
        }

        if (isNight) {
            for (lampX in listOf(80, 280, 480)) {
                g.color = Color(50, 50, 55)
                g.fillRect(lampX - 3, height / 4 - 20, 6, height / 8)
                g.color = Color(255, 220, 150, 180)
                g.fillOval(lampX - 25, height / 4 - 35, 50, 25)
                g.color = Color(255, 220, 150, 40)
                g.fillOval(lampX - 60, height / 4 - 20, 120, 200)
            }
        }

        g.color = if (isNight) Color(20, 20, 30) else Color(150, 140, 130)
        for (bx in listOf(50, 200, 400, 550)) {
            val bw = 60 + random.nextInt(40)
            val bh = 40 + random.nextInt(30)
            g.fillRect(bx, height / 3 - bh, bw, bh)
            if (isNight) {
                g.color = Color(255, 220, 150, 150)
                for (wy in (height / 3 - bh + 5) until (height / 3 - 5) step 12) {
                    for (wx in (bx + 5) until (bx + bw - 5) step 12) {
                        if (random.nextBoolean()) g.fillRect(wx, wy, 8, 8)
                    }
                }
                g.color = Color(20, 20, 30)
            }
        }

        g.color = Color(0, 0, 0, 150)
        g.fillRect(5, 5, 250, 25)
        g.fillRect(width - 75, 5, 70, 25)

        g.color = Color.WHITE
        g.font = g.font.deriveFont(14f)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        g.drawString("INTERSECTION - $timestamp", 10, 22)

        g.color = Color(255, 50, 50)
        g.drawString("● REC", width - 65, 22)

        g.color = Color(0, 0, 0, 150)
        g.fillRect(5, height - 30, 120, 25)
        g.color = Color.WHITE
        g.drawString("Vehicles: $vehicleCount", 10, height - 12)

        g.dispose()
        return image
    }

    fun analyzeImage(image: BufferedImage, providedImageData: ByteArray? = null): AnalysisResult {
        val w = image.width
        val h = image.height

        val sceneAnalysis = analyzeSceneType(image)

        if (!sceneAnalysis.isTrafficScene) {
            return createNonTrafficResult(w, h, sceneAnalysis, providedImageData, image)
        }

        var redSum = 0L; var greenSum = 0L; var blueSum = 0L
        var darkPixels = 0; var veryDarkPixels = 0; var grayPixels = 0
        var bluePixels = 0; var yellowPixels = 0; var lightPixels = 0
        var vehicleColorPixels = 0
        var roadPixels = 0

        for (y in 0 until h step 2) {
            for (x in 0 until w step 2) {
                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF

                redSum += r; greenSum += g; blueSum += b
                val brightness = (r + g + b) / 3
                val saturation = maxOf(r, g, b) - minOf(r, g, b)

                if (brightness < 30) veryDarkPixels++
                if (brightness < 60) darkPixels++
                if (brightness > 200) lightPixels++
                if (saturation < 30) grayPixels++
                if (b > r + 30 && b > g + 20 && b > 100) bluePixels++
                if (r > 150 && g > 100 && b < 100) yellowPixels++

                if (brightness in 40..120 && saturation < 40 && y > h / 3) roadPixels++

                val isWhiteVehicle = brightness > 180 && saturation < 50 && y > h / 3
                val isDarkVehicle = brightness in 20..80 && saturation < 30 && y > h / 3
                val isRedVehicle = r > 120 && r > g + 40 && r > b + 40 && y > h / 3
                val isBlueVehicle = b > 100 && b > r + 20 && b > g && y > h / 3
                val isSilverVehicle = brightness in 140..200 && saturation < 40 && y > h / 3

                if (isWhiteVehicle || isDarkVehicle || isRedVehicle || isBlueVehicle || isSilverVehicle) {
                    vehicleColorPixels++
                }
            }
        }

        val samples = (w / 2) * (h / 2)
        val avgR = (redSum / samples).toInt()
        val avgG = (greenSum / samples).toInt()
        val avgB = (blueSum / samples).toInt()
        val brightness = (avgR + avgG + avgB) / 3

        val darkRatio = darkPixels.toDouble() / samples
        val veryDarkRatio = veryDarkPixels.toDouble() / samples
        val lightRatio = lightPixels.toDouble() / samples

        var edges = 0
        var roadEdges = 0
        for (y in 1 until h - 1 step 3) {
            for (x in 1 until w - 1 step 3) {
                val left = getBrightness(image.getRGB(x - 1, y))
                val right = getBrightness(image.getRGB(x + 1, y))
                val top = getBrightness(image.getRGB(x, y - 1))
                val bottom = getBrightness(image.getRGB(x, y + 1))
                val grad = sqrt(((right - left) * (right - left) + (bottom - top) * (bottom - top)).toDouble())
                if (grad > 30) {
                    edges++
                    if (y > h / 3) roadEdges++
                }
            }
        }

        val edgeDensity = edges.toDouble() / ((w / 3) * (h / 3))
        val roadEdgeDensity = roadEdges.toDouble() / ((w / 3) * (h * 2 / 9))
        val hazeLevel = grayPixels.toDouble() / samples
        val skyRatio = bluePixels.toDouble() / samples
        val yellowRatio = yellowPixels.toDouble() / samples
        val vehicleRatio = vehicleColorPixels.toDouble() / samples
        val roadRatio = roadPixels.toDouble() / samples

        val currentHour = LocalDateTime.now().hour
        val isImageDark = veryDarkRatio > 0.3 || (brightness < 50 && darkRatio > 0.5)
        val isImageBright = brightness > 150 && lightRatio > 0.2
        val hasStreetLights = yellowRatio > 0.05 && darkRatio > 0.3
        val isNightByTime = currentHour < 6 || currentHour >= 20

        val timeEstimate = when {
            isImageDark && hasStreetLights -> "🌙 Night (lights on)"
            isImageDark || (isNightByTime && brightness < 100) -> "🌙 Night"
            isImageBright && skyRatio > 0.1 -> "☀️ Sunny"
            brightness > 120 -> "🌤️ Daytime"
            brightness > 80 -> "⛅ Cloudy"
            else -> "🌆 Dawn/Dusk"
        }

        val weather = when {
            hazeLevel > 0.6 && brightness < 100 -> "🌫️ Foggy"
            hazeLevel > 0.5 -> "☁️ Very Cloudy"
            darkRatio > 0.7 && !isNightByTime && !isImageDark -> "🌧️ Rainy"
            skyRatio > 0.15 && brightness > 150 -> "☀️ Clear"
            skyRatio > 0.08 -> "⛅ Partly Cloudy"
            isImageDark -> "🌙 Night"
            else -> "🌤️ Slightly Cloudy"
        }

        val crowdLevel = when {
            roadEdgeDensity > 0.5 -> "VERY HIGH 🔴"
            roadEdgeDensity > 0.3 -> "HIGH 🟠"
            roadEdgeDensity > 0.18 -> "MEDIUM 🟡"
            roadEdgeDensity > 0.1 -> "LOW 🟢"
            else -> "EMPTY ⚪"
        }
        val estimatedPeople = (roadEdgeDensity * 100).toInt()

        // Use shared module for crowd level determination
        val sharedCrowdLevel = ImageAnalyzer.determineCrowdLevel(estimatedPeople)

        val vehicleBlobs = countVehicleBlobs(image)

        val trafficScore = roadEdgeDensity * 0.4 + vehicleRatio * 0.3 + (vehicleBlobs / 20.0) * 0.3

        // Use shared module for traffic level determination
        val sharedTrafficLevel = ImageAnalyzer.determineTrafficLevel(vehicleBlobs, true)
        val trafficLevel = "${sharedTrafficLevel.description} ${sharedTrafficLevel.emoji}"

        val edgeBasedEstimate = maxOf(0, (roadEdgeDensity * 15).toInt())
        val estimatedVehicles = when {
            vehicleBlobs > 0 -> vehicleBlobs + (edgeBasedEstimate / 3)
            edgeBasedEstimate > 0 -> edgeBasedEstimate
            else -> 0
        }

        val airQuality = when {
            hazeLevel > 0.5 && !isImageDark -> "😷 POOR"
            hazeLevel > 0.35 && !isImageDark -> "😐 MODERATE"
            isImageDark -> "🌙 Night"
            hazeLevel > 0.2 -> "🙂 GOOD"
            else -> "😊 EXCELLENT"
        }

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))

        val fullReport = """
📊 ANALYSIS REPORT
$timestamp | ${w}x${h}

🚗 TRAFFIC: $trafficLevel
   Vehicles: ~$estimatedVehicles

👥 CROWD: $crowdLevel
   People: ~$estimatedPeople

🌤️ CONDITIONS
   Time: $timeEstimate
   Weather: $weather
   Air Quality: $airQuality

✅ Analyzed on device
🔒 No data sent to server
        """.trimIndent()

        val imageData = providedImageData ?: run {
            val baos = ByteArrayOutputStream()
            ImageIO.write(image, "jpg", baos)
            baos.toByteArray()
        }

        return AnalysisResult(
            trafficLevel = trafficLevel,
            crowdLevel = crowdLevel,
            weather = weather,
            airQuality = airQuality,
            timeEstimate = timeEstimate,
            estimatedVehicles = estimatedVehicles,
            estimatedPeople = estimatedPeople,
            fullReport = fullReport,
            imageData = imageData
        )
    }

    private fun countVehicleBlobs(image: BufferedImage): Int {
        val w = image.width
        val h = image.height
        val roadStartY = h / 3

        if (w < 100 || h < 100) return 0

        val scale = 8
        val scaledW = w / scale
        val scaledH = (h - roadStartY) / scale

        if (scaledW <= 10 || scaledH <= 10) return 0

        val mask = Array(scaledH) { BooleanArray(scaledW) }
        val visited = Array(scaledH) { BooleanArray(scaledW) }

        val roadBrightnesses = mutableListOf<Int>()
        for (sy in 0 until scaledH step 2) {
            for (sx in 0 until scaledW step 2) {
                val x = sx * scale
                val y = roadStartY + sy * scale
                if (x >= w || y >= h) continue
                val rgb = image.getRGB(x, y)
                val brightness = (((rgb shr 16) and 0xFF) + ((rgb shr 8) and 0xFF) + (rgb and 0xFF)) / 3
                roadBrightnesses.add(brightness)
            }
        }
        val medianRoadBrightness = if (roadBrightnesses.isNotEmpty()) {
            roadBrightnesses.sorted()[roadBrightnesses.size / 2]
        } else 70

        for (sy in 0 until scaledH) {
            for (sx in 0 until scaledW) {
                val x = sx * scale
                val y = roadStartY + sy * scale

                if (x >= w || y >= h) continue

                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF

                // Use shared module for vehicle pixel detection
                val isVehicle = ImageAnalyzer.isVehiclePixel(r, g, b, medianRoadBrightness)

                mask[sy][sx] = isVehicle
            }
        }

        val cleanMask = Array(scaledH) { BooleanArray(scaledW) }
        for (sy in 1 until scaledH - 1) {
            for (sx in 1 until scaledW - 1) {
                if (mask[sy][sx]) {
                    var neighborCount = 0
                    for (dy in -1..1) {
                        for (dx in -1..1) {
                            if (dy == 0 && dx == 0) continue
                            if (mask[sy + dy][sx + dx]) neighborCount++
                        }
                    }
                    cleanMask[sy][sx] = neighborCount >= 2
                }
            }
        }

        var blobCount = 0

        for (sy in 0 until scaledH) {
            for (sx in 0 until scaledW) {
                if (cleanMask[sy][sx] && !visited[sy][sx]) {
                    val blobInfo = floodFillWithShape(cleanMask, visited, sx, sy, scaledW, scaledH)
                    val blobSize = blobInfo.first
                    val aspectRatio = blobInfo.second
                    val blobWidth = (aspectRatio * 10).toInt().coerceAtLeast(1)
                    val blobHeight = 10

                    // Use shared module for blob validation
                    if (ImageAnalyzer.isValidVehicleBlob(blobSize, blobWidth, blobHeight, 10, 80)) {
                        blobCount++
                    }
                }
            }
        }

        return minOf(blobCount, 25)
    }

    private fun floodFillWithShape(mask: Array<BooleanArray>, visited: Array<BooleanArray>,
                          startX: Int, startY: Int, w: Int, h: Int): Pair<Int, Float> {
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(Pair(startX, startY))
        var size = 0
        var minX = startX
        var maxX = startX
        var minY = startY
        var maxY = startY

        while (stack.isNotEmpty()) {
            val (x, y) = stack.removeLast()

            if (x < 0 || x >= w || y < 0 || y >= h) continue
            if (visited[y][x] || !mask[y][x]) continue

            visited[y][x] = true
            size++

            minX = minOf(minX, x)
            maxX = maxOf(maxX, x)
            minY = minOf(minY, y)
            maxY = maxOf(maxY, y)

            stack.addLast(Pair(x + 1, y))
            stack.addLast(Pair(x - 1, y))
            stack.addLast(Pair(x, y + 1))
            stack.addLast(Pair(x, y - 1))
        }

        val blobWidth = maxX - minX + 1
        val blobHeight = maxY - minY + 1
        val aspectRatio = if (blobHeight > 0) blobWidth.toFloat() / blobHeight else 1f

        return Pair(size, aspectRatio)
    }

    private fun getBrightness(rgb: Int): Int {
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        return (r + g + b) / 3
    }

    data class SceneAnalysis(
        val isTrafficScene: Boolean,
        val isOutdoor: Boolean,
        val isIndoor: Boolean,
        val sceneType: String,
        val confidence: Double,
        val skyRatio: Double,
        val roadRatio: Double,
        val brownRatio: Double,
        val greenRatio: Double,
        val avgBrightness: Int
    )

    private fun analyzeSceneType(image: BufferedImage): SceneAnalysis {
        val w = image.width
        val h = image.height

        var skyPixels = 0
        var roadPixels = 0
        var brownPixels = 0
        var greenPixels = 0
        var totalSamples = 0

        var redSum = 0L
        var greenSum = 0L
        var blueSum = 0L

        for (y in 0 until h / 3 step 4) {
            for (x in 0 until w step 4) {
                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF

                if (b > 150 && b > r + 20 && b > g - 30 && g > 100) skyPixels++
                if (r > 180 && g > 180 && b > 180 &&
                    kotlin.math.abs(r - g) < 30 && kotlin.math.abs(g - b) < 30) skyPixels++
                totalSamples++
            }
        }

        for (y in h / 3 until h step 4) {
            for (x in 0 until w step 4) {
                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF
                val brightness = (r + g + b) / 3
                val saturation = maxOf(r, g, b) - minOf(r, g, b)

                redSum += r
                greenSum += g
                blueSum += b

                if (brightness in 40..120 && saturation < 40) roadPixels++
                if (r > g && r > b && r in 80..220 && g in 50..180 && saturation in 15..120) brownPixels++
                if (g > r && g > b && g > 80 && saturation > 30) greenPixels++
                totalSamples++
            }
        }

        val skyRatio = if (totalSamples > 0) skyPixels.toDouble() / (totalSamples / 3) else 0.0
        val roadRatio = if (totalSamples > 0) roadPixels.toDouble() / (totalSamples * 2 / 3) else 0.0
        val brownRatio = if (totalSamples > 0) brownPixels.toDouble() / (totalSamples * 2 / 3) else 0.0
        val greenRatio = if (totalSamples > 0) greenPixels.toDouble() / (totalSamples * 2 / 3) else 0.0
        val avgBrightness = if (totalSamples > 0) ((redSum + greenSum + blueSum) / (totalSamples * 3)).toInt() else 128

        val isOutdoor = skyRatio > 0.15 || (avgBrightness > 100 && roadRatio > 0.1)
        val isIndoor = !isOutdoor && (brownRatio > 0.15 || (skyRatio < 0.05 && roadRatio < 0.1))

        val isTrafficScene = (skyRatio > 0.08 || avgBrightness > 130) &&
                            roadRatio > 0.06 &&
                            brownRatio < 0.20 &&
                            greenRatio < 0.35

        val confidence = when {
            isTrafficScene && skyRatio > 0.2 && roadRatio > 0.15 -> 0.9
            isTrafficScene && roadRatio > 0.1 -> 0.7
            isTrafficScene -> 0.5
            else -> 0.3
        }

        val sceneType = when {
            isTrafficScene -> "🚗 Traffic/Road Scene"
            greenRatio > 0.3 -> "🌳 Nature/Park"
            isIndoor && brownRatio > 0.15 -> "🏛️ Indoor (Historic Building)"
            isIndoor -> "🏠 Indoor"
            isOutdoor -> "🏙️ Outdoor (No Traffic)"
            else -> "❓ Unknown"
        }

        return SceneAnalysis(
            isTrafficScene = isTrafficScene,
            isOutdoor = isOutdoor,
            isIndoor = isIndoor,
            sceneType = sceneType,
            confidence = confidence,
            skyRatio = skyRatio,
            roadRatio = roadRatio,
            brownRatio = brownRatio,
            greenRatio = greenRatio,
            avgBrightness = avgBrightness
        )
    }

    private fun createNonTrafficResult(
        w: Int,
        h: Int,
        scene: SceneAnalysis,
        providedImageData: ByteArray?,
        image: BufferedImage
    ): AnalysisResult {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))

        val estimatedPeople = countPeopleInScene(image)

        val crowdLevel = when {
            estimatedPeople >= 30 -> "VERY HIGH 🔴"
            estimatedPeople >= 15 -> "HIGH 🟠"
            estimatedPeople >= 8 -> "MEDIUM 🟡"
            estimatedPeople >= 3 -> "LOW 🟢"
            estimatedPeople >= 1 -> "VERY LOW 🟢"
            else -> "EMPTY ⚪"
        }

        val fullReport = """
📊 CROWD ANALYSIS REPORT
$timestamp | ${w}x${h}

🎯 SCENE DETECTION
   Type: ${scene.sceneType}
   
   ℹ️ Indoor/historic building detected
   🚗 Vehicle analysis disabled
   👥 Crowd analysis only

👥 CROWD ANALYSIS
   Estimated: ~$estimatedPeople people
   Level: $crowdLevel

✅ Analysis completed
🔒 No data sent to server
        """.trimIndent()

        val imageData = providedImageData ?: run {
            val baos = ByteArrayOutputStream()
            ImageIO.write(image, "jpg", baos)
            baos.toByteArray()
        }

        return AnalysisResult(
            trafficLevel = "INDOOR 🏛️",
            crowdLevel = crowdLevel,
            weather = "Indoor",
            airQuality = "Normal",
            timeEstimate = if (scene.avgBrightness > 120) "Bright" else "Dim",
            estimatedVehicles = 0,
            estimatedPeople = estimatedPeople,
            fullReport = fullReport,
            imageData = imageData
        )
    }

    private fun countPeopleInScene(image: BufferedImage): Int {
        val w = image.width
        val h = image.height

        var skinTonePixels = 0
        var totalEdges = 0

        for (y in 0 until h step 3) {
            for (x in 0 until w step 3) {
                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF

                val isSkinTone = (r > 95 && g > 40 && b > 20 &&
                                  r > g && r > b &&
                                  kotlin.math.abs(r - g) > 15 &&
                                  r - g < 100 && r - b < 100)

                if (isSkinTone) skinTonePixels++
            }
        }

        for (y in 1 until h - 1 step 4) {
            for (x in 1 until w - 1 step 4) {
                val left = getBrightness(image.getRGB(x - 1, y))
                val right = getBrightness(image.getRGB(x + 1, y))
                val top = getBrightness(image.getRGB(x, y - 1))
                val bottom = getBrightness(image.getRGB(x, y + 1))
                val grad = sqrt(((right - left) * (right - left) + (bottom - top) * (bottom - top)).toDouble())
                if (grad > 25) totalEdges++
            }
        }

        val samples = (w / 3) * (h / 3)
        val skinRatio = skinTonePixels.toDouble() / samples
        val edgeDensity = totalEdges.toDouble() / ((w / 4) * (h / 4))

        val peopleEstimate = when {
            skinRatio > 0.15 -> (skinRatio * 100 + edgeDensity * 20).toInt()
            skinRatio > 0.08 -> (skinRatio * 80 + edgeDensity * 15).toInt()
            skinRatio > 0.03 -> (skinRatio * 60 + edgeDensity * 10).toInt()
            edgeDensity > 0.3 -> (edgeDensity * 30).toInt()
            else -> (skinRatio * 40 + edgeDensity * 8).toInt()
        }

        return maxOf(0, minOf(peopleEstimate, 100))
    }
}

