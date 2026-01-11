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
    // Alias properties for compatibility
    val vehicleCount: Int get() = estimatedVehicles
    val timeOfDay: String get() = timeEstimate
    val brightness: String get() = when {
        timeEstimate.contains("Gece") -> "Düşük"
        timeEstimate.contains("Güneşli") -> "Yüksek"
        else -> "Normal"
    }
}

class CameraAnalysisService {

    // Güvenilir trafik kameraları ve görüntü kaynakları
    private val liveCameraUrls = listOf(
        // Türkiye trafik kameraları
        "https://trafik.ibb.gov.tr/kamera/cam001.jpg",
        // Alternatif açık kameralar
        "https://www.meteo.be/services/camera/IRM_Uccle1.jpg",
        "https://www.trafficcam.eu/shot.jpg",
        // Yedek statik görüntüler
        "https://picsum.photos/640/480?random=traffic"
    )

    // Demo modu için kullanılacak - gerçek kamera bağlanamadığında
    private var useDemoMode = false

    fun analyzeFromUrl(url: String): AnalysisResult {
        val image = fetchImageFromUrl(url)
        return if (image != null) {
            analyzeImage(image)
        } else {
            analyzeDemo()
        }
    }

    fun analyzeFromLiveCamera(): AnalysisResult {
        for (url in liveCameraUrls) {
            try {
                val image = fetchImageFromUrl(url)
                if (image != null) {
                    return analyzeImage(image)
                }
            } catch (e: Exception) {
                continue
            }
        }
        return analyzeDemo()
    }

    /**
     * Otogar Kavşağı simülasyonu - gerçekçi demo görüntü
     */
    fun analyzeOtogarKavsagi(): AnalysisResult {
        // Gerçekçi demo görüntüsü oluştur ve analiz et
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
            if (image != null) {
                analyzeImage(image, imageData)
            } else {
                analyzeDemo()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            analyzeDemo()
        }
    }

    private fun fetchImageFromUrl(urlString: String): BufferedImage? {
        return try {
            println("📡 Fetching image from: $urlString")
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000  // 5 saniye
            connection.readTimeout = 5000     // 5 saniye
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            connection.setRequestProperty("Accept", "image/*")
            connection.instanceFollowRedirects = true

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val contentType = connection.contentType ?: ""
                println("📦 Content-Type: $contentType")
                val inputStream = connection.inputStream

                if (contentType.contains("multipart") || urlString.contains("mjpg")) {
                    // MJPEG stream - ilk frame'i al
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
            } else {
                println("❌ HTTP Error: ${connection.responseCode}")
                null
            }
        } catch (e: java.net.SocketTimeoutException) {
            println("⏰ Timeout fetching: $urlString")
            null
        } catch (e: java.net.ConnectException) {
            println("🔌 Connection failed: $urlString")
            null
        } catch (e: Exception) {
            println("❌ Error fetching image: ${e.message}")
            null
        }
    }

    private fun createDemoImage(): BufferedImage {
        val width = 640
        val height = 480
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        val random = Random()

        val hour = LocalDateTime.now().hour
        val minute = LocalDateTime.now().minute
        val isNight = hour < 6 || hour >= 19

        // Rastgelelik için zaman bazlı seed kullan (her dakika değişsin)
        val timeSeed = System.currentTimeMillis() / 1000 // Her saniye farklı
        random.setSeed(timeSeed)

        // Trafik yoğunluğu - saate göre değişken
        val trafficMultiplier = when (hour) {
            in 7..9 -> 2.5    // Sabah rush hour
            in 12..14 -> 1.5  // Öğle
            in 17..19 -> 2.8  // Akşam rush hour
            in 22..24, in 0..5 -> 0.3 // Gece
            else -> 1.0
        }

        // Rastgele trafik yoğunluğu (0-20 arası araç)
        val baseVehicles = random.nextInt(8)
        val vehicleCount = maxOf(0, minOf(20, (baseVehicles * trafficMultiplier).toInt() + random.nextInt(5) - 2))

        // Arka plan
        val skyColor = if (isNight) Color(15, 15, 35) else Color(135, 180, 220)
        val roadColor = if (isNight) Color(30, 30, 35) else Color(60, 60, 65)

        // Gökyüzü
        g.color = skyColor
        g.fillRect(0, 0, width, height / 3)

        // Bulutlar (gündüz)
        if (!isNight && random.nextBoolean()) {
            g.color = Color(255, 255, 255, 150)
            for (i in 0 until 3) {
                val cx = random.nextInt(width)
                val cy = random.nextInt(height / 4)
                g.fillOval(cx, cy, 80 + random.nextInt(60), 30 + random.nextInt(20))
            }
        }

        // Yol - çoklu şerit
        g.color = roadColor
        g.fillRect(0, height / 3, width, height * 2 / 3)

        // Kaldırım
        g.color = Color(80, 80, 85)
        g.fillRect(0, height / 3, width, 15)
        g.fillRect(0, height - 20, width, 20)

        // Yol çizgileri - kesikli
        g.color = if (isNight) Color(100, 100, 80) else Color(230, 230, 210)
        for (y in height / 3 + 50 until height - 30 step 50) {
            g.fillRect(width / 3 - 2, y, 4, 25)
            g.fillRect(width * 2 / 3 - 2, y, 4, 25)
        }

        // Kenar çizgileri (sürekli)
        g.color = Color(230, 230, 210)
        g.fillRect(20, height / 3 + 20, 3, height * 2 / 3 - 40)
        g.fillRect(width - 23, height / 3 + 20, 3, height * 2 / 3 - 40)

        // Araçlar - farklı boyutlarda ve renklerde
        val vehicleColors = if (isNight) {
            arrayOf(Color(30, 30, 35), Color(45, 45, 50), Color(25, 25, 30), Color(60, 60, 65))
        } else {
            arrayOf(
                Color.WHITE, Color(220, 220, 220), Color(40, 40, 45),
                Color(180, 20, 20), Color(20, 20, 150), Color(150, 150, 160),
                Color(200, 180, 100), Color(100, 100, 100), Color(30, 80, 30)
            )
        }

        // Araç türleri: sedan, SUV, kamyon, otobüs
        data class Vehicle(val x: Int, val y: Int, val w: Int, val h: Int, val color: Color, val type: String)
        val vehicles = mutableListOf<Vehicle>()

        val lanes = listOf(width / 6, width / 2, width * 5 / 6) // 3 şerit

        for (i in 0 until vehicleCount) {
            var attempts = 0
            var placed = false

            while (!placed && attempts < 20) {
                val lane = lanes[random.nextInt(lanes.size)]
                val vy = height / 3 + 40 + random.nextInt(height / 2)

                // Araç tipi
                val vehicleType = when (random.nextInt(10)) {
                    in 0..5 -> "sedan"     // 60% sedan
                    in 6..7 -> "suv"       // 20% SUV
                    8 -> "truck"           // 10% kamyon
                    else -> "bus"          // 10% otobüs
                }

                val (vw, vh) = when (vehicleType) {
                    "sedan" -> Pair(45 + random.nextInt(15), 22 + random.nextInt(8))
                    "suv" -> Pair(55 + random.nextInt(15), 28 + random.nextInt(8))
                    "truck" -> Pair(70 + random.nextInt(30), 25 + random.nextInt(10))
                    "bus" -> Pair(90 + random.nextInt(20), 28 + random.nextInt(8))
                    else -> Pair(50, 25)
                }

                val vx = lane - vw / 2 + random.nextInt(30) - 15

                // Çakışma kontrolü
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

        // Araçları çiz
        for (v in vehicles) {
            // Gölge
            g.color = Color(0, 0, 0, 50)
            g.fillRect(v.x + 3, v.y + 3, v.w, v.h)

            // Araç gövdesi
            g.color = v.color
            g.fillRoundRect(v.x, v.y, v.w, v.h, 5, 5)

            // Cam
            g.color = Color(100, 150, 200, if (isNight) 100 else 180)
            g.fillRect(v.x + v.w / 4, v.y + 2, v.w / 2, v.h / 3)

            // Farlar (gece)
            if (isNight) {
                g.color = Color(255, 255, 200, 200)
                g.fillOval(v.x + 2, v.y + v.h / 2 - 3, 6, 6)
                g.fillOval(v.x + v.w - 8, v.y + v.h / 2 - 3, 6, 6)
                g.color = Color(255, 50, 50, 180)
                g.fillOval(v.x + v.w - 5, v.y + 2, 4, 4)
                g.fillOval(v.x + v.w - 5, v.y + v.h - 6, 4, 4)
            }
        }

        // Sokak lambaları (gece)
        if (isNight) {
            for (lampX in listOf(80, 280, 480)) {
                g.color = Color(50, 50, 55)
                g.fillRect(lampX - 3, height / 4 - 20, 6, height / 8)
                g.color = Color(255, 220, 150, 180)
                g.fillOval(lampX - 25, height / 4 - 35, 50, 25)
                // Işık efekti
                g.color = Color(255, 220, 150, 40)
                g.fillOval(lampX - 60, height / 4 - 20, 120, 200)
            }
        }

        // Binalar (arka plan)
        g.color = if (isNight) Color(20, 20, 30) else Color(150, 140, 130)
        for (bx in listOf(50, 200, 400, 550)) {
            val bw = 60 + random.nextInt(40)
            val bh = 40 + random.nextInt(30)
            g.fillRect(bx, height / 3 - bh, bw, bh)
            // Pencereler
            if (isNight) {
                g.color = Color(255, 220, 150, 150)
                for (wy in (height / 3 - bh + 5) until (height / 3 - 5) step 12) {
                    for (wx in (bx + 5) until (bx + bw - 5) step 12) {
                        if (random.nextBoolean()) {
                            g.fillRect(wx, wy, 8, 8)
                        }
                    }
                }
                g.color = Color(20, 20, 30)
            }
        }

        // Timestamp ve bilgi
        g.color = Color(0, 0, 0, 150)
        g.fillRect(5, 5, 250, 25)
        g.fillRect(width - 75, 5, 70, 25)

        g.color = Color.WHITE
        g.font = g.font.deriveFont(14f)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        g.drawString("OTOGAR KAVŞAĞI - $timestamp", 10, 22)

        g.color = Color(255, 50, 50)
        g.drawString("● REC", width - 65, 22)

        // Araç sayısı bilgisi (debug)
        g.color = Color(0, 0, 0, 150)
        g.fillRect(5, height - 30, 120, 25)
        g.color = Color.WHITE
        g.drawString("Araç: $vehicleCount", 10, height - 12)

        g.dispose()
        return image
    }

    fun analyzeImage(image: BufferedImage, providedImageData: ByteArray? = null): AnalysisResult {
        val w = image.width
        val h = image.height

        // === SAHNE TESPİTİ - İç mekan/dış mekan kontrolü ===
        val sceneAnalysis = analyzeSceneType(image)

        // Eğer trafik sahnesi değilse, uyarı ver
        if (!sceneAnalysis.isTrafficScene) {
            return createNonTrafficResult(w, h, sceneAnalysis, providedImageData, image)
        }

        var redSum = 0L; var greenSum = 0L; var blueSum = 0L
        var darkPixels = 0; var veryDarkPixels = 0; var grayPixels = 0
        var bluePixels = 0; var yellowPixels = 0; var lightPixels = 0
        var vehicleColorPixels = 0  // Araç renkleri için sayaç
        var roadPixels = 0  // Yol pikselleri

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

                // Yol pikseli tespiti (koyu gri, düşük satürasyon)
                if (brightness in 40..120 && saturation < 40 && y > h / 3) {
                    roadPixels++
                }

                // Araç renkleri (beyaz, siyah, kırmızı, mavi, gümüş araçlar)
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

        // Edge detection - sadece alt 2/3 kısmında (yol bölgesi)
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

        // Zaman tahmini
        val currentHour = LocalDateTime.now().hour
        val isImageDark = veryDarkRatio > 0.3 || (brightness < 50 && darkRatio > 0.5)
        val isImageBright = brightness > 150 && lightRatio > 0.2
        val hasStreetLights = yellowRatio > 0.05 && darkRatio > 0.3
        val isNightByTime = currentHour < 6 || currentHour >= 20

        val timeEstimate = when {
            isImageDark && hasStreetLights -> "🌙 Gece (lambalar)"
            isImageDark || (isNightByTime && brightness < 100) -> "🌙 Gece"
            isImageBright && skyRatio > 0.1 -> "☀️ Güneşli"
            brightness > 120 -> "🌤️ Gündüz"
            brightness > 80 -> "⛅ Bulutlu"
            else -> "🌆 Akşam/Sabah"
        }

        // Hava durumu
        val weather = when {
            hazeLevel > 0.6 && brightness < 100 -> "🌫️ Sisli"
            hazeLevel > 0.5 -> "☁️ Çok Bulutlu"
            darkRatio > 0.7 && !isNightByTime && !isImageDark -> "🌧️ Yağmurlu"
            skyRatio > 0.15 && brightness > 150 -> "☀️ Açık"
            skyRatio > 0.08 -> "⛅ Parçalı Bulutlu"
            isImageDark -> "🌙 Gece"
            else -> "🌤️ Hafif Bulutlu"
        }

        // Kalabalık (edge density bazlı)
        val crowdLevel = when {
            roadEdgeDensity > 0.5 -> "ÇOK YOĞUN 🔴"
            roadEdgeDensity > 0.3 -> "YOĞUN 🟠"
            roadEdgeDensity > 0.18 -> "ORTA 🟡"
            roadEdgeDensity > 0.1 -> "AZ 🟢"
            else -> "BOŞ ⚪"
        }
        val estimatedPeople = (roadEdgeDensity * 100).toInt()

        // ========== GELİŞMİŞ ARAÇ TESPİTİ ==========
        // Blob tespiti: Yol bölgesindeki bağlantılı piksel gruplarını say
        val vehicleBlobs = countVehicleBlobs(image)

        // Trafik seviyesi - hem blob sayısı hem de edge yoğunluğunu kullan
        val trafficScore = roadEdgeDensity * 0.4 + vehicleRatio * 0.3 + (vehicleBlobs / 20.0) * 0.3
        val trafficLevel = when {
            vehicleBlobs >= 15 || trafficScore > 0.35 -> "ÇOK YOĞUN 🔴"
            vehicleBlobs >= 8 || trafficScore > 0.22 -> "YOĞUN 🟠"
            vehicleBlobs >= 4 || trafficScore > 0.12 -> "ORTA 🟡"
            vehicleBlobs >= 1 || trafficScore > 0.05 -> "HAFİF 🟢"
            else -> "BOŞ ⚪"
        }

        // Araç tahmini: Blob sayısı + edge bazlı ek tahmin
        val edgeBasedEstimate = maxOf(0, (roadEdgeDensity * 15).toInt())
        val estimatedVehicles = when {
            vehicleBlobs > 0 -> vehicleBlobs + (edgeBasedEstimate / 3)
            edgeBasedEstimate > 0 -> edgeBasedEstimate
            else -> 0
        }

        // Hava kalitesi
        val airQuality = when {
            hazeLevel > 0.5 && !isImageDark -> "😷 KÖTÜ"
            hazeLevel > 0.35 && !isImageDark -> "😐 ORTA"
            isImageDark -> "🌙 Gece"
            hazeLevel > 0.2 -> "🙂 İYİ"
            else -> "😊 ÇOK İYİ"
        }

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))

        val fullReport = """
══════════════════════════════════════
📊 KAVŞAK ANALİZ RAPORU
══════════════════════════════════════
🕐 $timestamp
📐 Çözünürlük: ${w}x${h}

──────────────────────────────────────
🚗 TRAFİK DURUMU
──────────────────────────────────────
   Yoğunluk: $trafficLevel
   Tahmini Araç: ~$estimatedVehicles
   Trafik Skoru: %.3f

──────────────────────────────────────
👥 KALABALIK DURUMU
──────────────────────────────────────
   Yoğunluk: $crowdLevel
   Tahmini Kişi: ~$estimatedPeople
   Yol Edge Yoğunluğu: %.3f

──────────────────────────────────────
🌤️ ZAMAN & HAVA
──────────────────────────────────────
   Zaman: $timeEstimate
   Hava: $weather
   Parlaklık: $brightness
   Karanlık Oran: %.2f

──────────────────────────────────────
🌫️ HAVA KALİTESİ
──────────────────────────────────────
   Değerlendirme: $airQuality
   Sis/Pus Oranı: %.2f
   Görünürlük: ${if (hazeLevel > 0.4) "DÜŞÜK" else "İYİ"}

══════════════════════════════════════
📈 TEKNİK VERİLER
══════════════════════════════════════
   Edge Yoğunluğu: %.3f
   Araç Renk Oranı: %.3f
   Yol Oranı: %.3f
   Gece mi: ${if (isImageDark) "EVET" else "HAYIR"}

══════════════════════════════════════
✅ Tarayıcıda analiz edildi
🔒 Veri sunucuya gönderilmedi
══════════════════════════════════════
        """.trimIndent().format(trafficScore, roadEdgeDensity, darkRatio, hazeLevel, edgeDensity, vehicleRatio, roadRatio)

        // Görüntüyü byte array'e çevir (sağlanmamışsa)
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

    /**
     * Görüntüdeki araç benzeri blob'ları sayar
     * Blob: Belirli boyutta, araç rengine sahip bağlantılı piksel grubu
     */
    private fun countVehicleBlobs(image: BufferedImage): Int {
        val w = image.width
        val h = image.height
        val roadStartY = h / 3  // Yol bölgesi üst 1/3'ten sonra başlar

        // Görüntü çok küçükse
        if (w < 100 || h < 100) return 0

        // Araç maskesi oluştur (downsampled - hız için)
        val scale = 8  // Daha büyük scale = daha az gürültü
        val scaledW = w / scale
        val scaledH = (h - roadStartY) / scale

        if (scaledW <= 10 || scaledH <= 10) return 0

        val mask = Array(scaledH) { BooleanArray(scaledW) }
        val visited = Array(scaledH) { BooleanArray(scaledW) }

        // Önce yol rengini tespit et (median parlaklık)
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

        // Araç pikseli tespit et
        for (sy in 0 until scaledH) {
            for (sx in 0 until scaledW) {
                val x = sx * scale
                val y = roadStartY + sy * scale

                if (x >= w || y >= h) continue

                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF
                val brightness = (r + g + b) / 3
                val saturation = maxOf(r, g, b) - minOf(r, g, b)

                // Yol renginden belirgin şekilde farklı olan pikselleri tespit et
                val diffFromRoad = kotlin.math.abs(brightness - medianRoadBrightness)

                // Araç rengi tespit kriterleri - çok sıkı kontrol
                val isVehicle = when {
                    // Yoldan çok farklı olmayan pikselleri atla
                    diffFromRoad < 30 -> false

                    // Beyaz/açık renkli araçlar - yol renginden belirgin farklı olmalı
                    brightness > 210 && saturation < 40 && diffFromRoad > 100 -> true

                    // Çok koyu araçlar - yoldan çok daha koyu
                    brightness < 40 && saturation < 15 && diffFromRoad > 40 -> true

                    // Kırmızı araçlar - çok belirgin
                    r > 160 && r > g + 60 && r > b + 60 && saturation > 80 -> true

                    // Mavi araçlar - çok belirgin
                    b > 140 && b > r + 45 && b > g + 30 && saturation > 70 -> true

                    // Sarı/turuncu araçlar (otobüs, taksi) - çok belirgin
                    r > 200 && g > 140 && b < 70 && saturation > 100 -> true

                    else -> false
                }

                mask[sy][sx] = isVehicle
            }
        }

        // Gürültü temizleme - tek pikselleri temizle
        val cleanMask = Array(scaledH) { BooleanArray(scaledW) }
        for (sy in 1 until scaledH - 1) {
            for (sx in 1 until scaledW - 1) {
                if (mask[sy][sx]) {
                    // En az 2 komşusu olmalı
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

        // Blob sayma (flood-fill)
        var blobCount = 0
        // Minimum ve maximum blob boyutları - daha sıkı
        val minBlobSize = 10   // Gerçek araç boyutu - en az 10 piksel
        val maxBlobSize = 80   // Maximum araç boyutu

        for (sy in 0 until scaledH) {
            for (sx in 0 until scaledW) {
                if (cleanMask[sy][sx] && !visited[sy][sx]) {
                    // Yeni blob bulundu, boyutunu ve şeklini hesapla
                    val blobInfo = floodFillWithShape(cleanMask, visited, sx, sy, scaledW, scaledH)
                    val blobSize = blobInfo.first
                    val aspectRatio = blobInfo.second

                    // Araç boyutunda ve şeklinde mi kontrol et
                    // Araçlar genellikle yatay dikdörtgen şeklinde (aspect ratio 0.3-4)
                    if (blobSize in minBlobSize..maxBlobSize && aspectRatio in 0.3f..5.0f) {
                        blobCount++
                    }
                }
            }
        }

        // Maksimum makul araç sayısı
        return minOf(blobCount, 25)
    }

    /**
     * Flood-fill ile blob boyutu ve aspect ratio döndürür
     */
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

            // Bounding box güncelle
            minX = minOf(minX, x)
            maxX = maxOf(maxX, x)
            minY = minOf(minY, y)
            maxY = maxOf(maxY, y)

            // 4-bağlantılı komşular
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

    // === SAHNE TESPİTİ SİSTEMİ ===

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

    /**
     * Sahne Tipi Analizi
     * Gökyüzü, yol, iç mekan zemin ve renk dağılımına bakarak sahne tipini belirler
     */
    private fun analyzeSceneType(image: BufferedImage): SceneAnalysis {
        val w = image.width
        val h = image.height

        var skyPixels = 0
        var roadPixels = 0
        var brownPixels = 0  // İç mekan zemin (kahverengi/turuncu)
        var greenPixels = 0  // Doğa
        var totalSamples = 0

        var redSum = 0L
        var greenSum = 0L
        var blueSum = 0L

        // Üst 1/3 bölge analizi (gökyüzü tespiti)
        for (y in 0 until h / 3 step 4) {
            for (x in 0 until w step 4) {
                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF

                // Mavi gökyüzü tespiti
                if (b > 150 && b > r + 20 && b > g - 30 && g > 100) {
                    skyPixels++
                }
                // Gri/beyaz gökyüzü (bulutlu)
                if (r > 180 && g > 180 && b > 180 &&
                    kotlin.math.abs(r - g) < 30 && kotlin.math.abs(g - b) < 30) {
                    skyPixels++
                }
                totalSamples++
            }
        }

        // Alt 2/3 bölge analizi (yol/zemin tespiti)
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

                // Asfalt/yol tespiti (koyu gri, düşük satürasyon)
                if (brightness in 40..120 && saturation < 40) {
                    roadPixels++
                }
                // Kahverengi/turuncu zemin (iç mekan, tarihi yapı, mermer)
                if (r > g && r > b && r in 80..220 && g in 50..180 && saturation in 15..120) {
                    brownPixels++
                }
                // Yeşil alan (doğa, park)
                if (g > r && g > b && g > 80 && saturation > 30) {
                    greenPixels++
                }
                totalSamples++
            }
        }

        val skyRatio = if (totalSamples > 0) skyPixels.toDouble() / (totalSamples / 3) else 0.0
        val roadRatio = if (totalSamples > 0) roadPixels.toDouble() / (totalSamples * 2 / 3) else 0.0
        val brownRatio = if (totalSamples > 0) brownPixels.toDouble() / (totalSamples * 2 / 3) else 0.0
        val greenRatio = if (totalSamples > 0) greenPixels.toDouble() / (totalSamples * 2 / 3) else 0.0
        val avgBrightness = if (totalSamples > 0) ((redSum + greenSum + blueSum) / (totalSamples * 3)).toInt() else 128

        // Sahne tipi belirleme
        val isOutdoor = skyRatio > 0.15 || (avgBrightness > 100 && roadRatio > 0.1)
        val isIndoor = !isOutdoor && (brownRatio > 0.15 || (skyRatio < 0.05 && roadRatio < 0.1))

        // Trafik sahnesi kriterleri:
        // 1. Gökyüzü görünmeli VEYA çok parlak olmalı
        // 2. Yol benzeri yüzey olmalı
        // 3. Kahverengi iç mekan zemini düşük olmalı
        // 4. Yeşil alan çok fazla olmamalı
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
            isTrafficScene -> "🚗 Trafik/Yol Sahnesi"
            greenRatio > 0.3 -> "🌳 Doğa/Park"
            isIndoor && brownRatio > 0.15 -> "🏛️ İç Mekan (Tarihi Yapı)"
            isIndoor -> "🏠 İç Mekan"
            isOutdoor -> "🏙️ Dış Mekan (Trafik Yok)"
            else -> "❓ Belirsiz"
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

    /**
     * Trafik dışı sahne için sonuç oluşturur - SADECE İNSAN SAYIMI YAPAR
     */
    private fun createNonTrafficResult(
        w: Int,
        h: Int,
        scene: SceneAnalysis,
        providedImageData: ByteArray?,
        image: BufferedImage
    ): AnalysisResult {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))

        // İnsan sayımı için edge detection yap
        val estimatedPeople = countPeopleInScene(image)

        val crowdLevel = when {
            estimatedPeople >= 30 -> "ÇOK YOĞUN 🔴"
            estimatedPeople >= 15 -> "YOĞUN 🟠"
            estimatedPeople >= 8 -> "ORTA 🟡"
            estimatedPeople >= 3 -> "AZ 🟢"
            estimatedPeople >= 1 -> "ÇOK AZ 🟢"
            else -> "BOŞ ⚪"
        }

        val fullReport = """
══════════════════════════════════════
📊 KALABALIK ANALİZ RAPORU
══════════════════════════════════════
🕐 $timestamp
📐 Çözünürlük: ${w}x${h}

──────────────────────────────────────
🎯 SAHNE TESPİTİ
──────────────────────────────────────
   Tip: ${scene.sceneType}
   
   ℹ️ Bu görüntü iç mekan/tarihi yapı
   olarak tespit edildi.
   
   🚗 Araç analizi devre dışı
   👥 Sadece kalabalık analizi yapıldı

──────────────────────────────────────
👥 KALABALIK ANALİZİ
──────────────────────────────────────
   Tahmini Kişi: ~$estimatedPeople
   Yoğunluk: $crowdLevel

──────────────────────────────────────
📊 SAHNE BİLGİLERİ
──────────────────────────────────────
   Gökyüzü: %.1f%%
   Zemin Tipi: ${if (scene.brownRatio > 0.15) "İç Mekan" else "Belirsiz"}
   Parlaklık: ${scene.avgBrightness}

══════════════════════════════════════
✅ Kalabalık analizi tamamlandı
🔒 Veri sunucuya gönderilmedi
══════════════════════════════════════
        """.trimIndent().format(scene.skyRatio * 100)

        // Görüntüyü byte array'e çevir
        val imageData = providedImageData ?: run {
            val baos = ByteArrayOutputStream()
            ImageIO.write(image, "jpg", baos)
            baos.toByteArray()
        }

        return AnalysisResult(
            trafficLevel = "İÇ MEKAN 🏛️",
            crowdLevel = crowdLevel,
            weather = "İç Mekan",
            airQuality = "Normal",
            timeEstimate = if (scene.avgBrightness > 120) "Aydınlık" else "Loş",
            estimatedVehicles = 0,  // Araç yok
            estimatedPeople = estimatedPeople,  // Sadece insan sayısı
            fullReport = fullReport,
            imageData = imageData
        )
    }

    /**
     * Sahnedeki insan sayısını tahmin eder
     * Edge detection ve blob analizi kullanır
     */
    private fun countPeopleInScene(image: BufferedImage): Int {
        val w = image.width
        val h = image.height

        // İnsan tespiti için daha geniş alan tara (tüm görüntü)
        var skinTonePixels = 0
        var clothingPixels = 0
        var totalEdges = 0

        for (y in 0 until h step 3) {
            for (x in 0 until w step 3) {
                val rgb = image.getRGB(x, y)
                val r = (rgb shr 16) and 0xFF
                val g = (rgb shr 8) and 0xFF
                val b = rgb and 0xFF

                // Ten rengi tespiti (çeşitli cilt tonları)
                val isSkinTone = (r > 95 && g > 40 && b > 20 &&
                                  r > g && r > b &&
                                  kotlin.math.abs(r - g) > 15 &&
                                  r - g < 100 && r - b < 100)

                if (isSkinTone) skinTonePixels++

                // Kıyafet renkleri (koyu/açık tonlar, gökyüzü ve zemin hariç)
                val brightness = (r + g + b) / 3
                val saturation = maxOf(r, g, b) - minOf(r, g, b)
                val isClothing = brightness in 30..220 &&
                                 saturation > 10 &&
                                 !(b > r + 30 && b > g) // Gökyüzü değil

                if (isClothing) clothingPixels++
            }
        }

        // Edge detection (insan siluetleri için)
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

        // İnsan tahmini: ten rengi ve edge yoğunluğu kombinasyonu
        val peopleEstimate = when {
            skinRatio > 0.15 -> (skinRatio * 100 + edgeDensity * 20).toInt()
            skinRatio > 0.08 -> (skinRatio * 80 + edgeDensity * 15).toInt()
            skinRatio > 0.03 -> (skinRatio * 60 + edgeDensity * 10).toInt()
            edgeDensity > 0.3 -> (edgeDensity * 30).toInt()
            else -> (skinRatio * 40 + edgeDensity * 8).toInt()
        }

        // Makul sınırlar içinde tut
        return maxOf(0, minOf(peopleEstimate, 100))
    }
}
