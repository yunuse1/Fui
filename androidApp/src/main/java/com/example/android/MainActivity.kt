package com.example.android

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.sqrt
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var resultText: TextView
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var vehicleCountText: TextView
    private lateinit var peopleCountText: TextView
    private lateinit var trafficLevelText: TextView
    private lateinit var crowdLevelText: TextView

    private var selectedBitmap: Bitmap? = null

    companion object {
        private const val REQUEST_IMAGE_PICK = 1001
        private const val REQUEST_IMAGE_CAPTURE = 1002
        private const val PERMISSION_REQUEST_CODE = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // View'ları bağla
        imageView = findViewById(R.id.imageView)
        resultText = findViewById(R.id.resultText)
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        vehicleCountText = findViewById(R.id.vehicleCountText)
        peopleCountText = findViewById(R.id.peopleCountText)
        trafficLevelText = findViewById(R.id.trafficLevelText)
        crowdLevelText = findViewById(R.id.crowdLevelText)

        // Butonları bağla
        findViewById<Button>(R.id.galleryButton).setOnClickListener { openGallery() }
        findViewById<Button>(R.id.cameraButton).setOnClickListener { openCamera() }
        findViewById<Button>(R.id.analyzeButton).setOnClickListener { analyzeImage() }
        findViewById<Button>(R.id.demoButton).setOnClickListener { loadDemoImage() }

        checkPermissions()

        // Başlangıç durumu
        statusText.text = "📸 Bir fotoğraf seçin veya çekin"
        resetStats()
    }

    private fun resetStats() {
        vehicleCountText.text = "0"
        peopleCountText.text = "0"
        trafficLevelText.text = "Bekleniyor..."
        crowdLevelText.text = "Bekleniyor..."
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), PERMISSION_REQUEST_CODE)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_IMAGE_PICK)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        } else {
            Toast.makeText(this, "Kamera bulunamadı", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_PICK -> {
                    data?.data?.let { uri ->
                        loadImageFromUri(uri)
                        // Otomatik analiz
                        analyzeImage()
                    }
                }
                REQUEST_IMAGE_CAPTURE -> {
                    val bitmap = data?.extras?.get("data") as? Bitmap
                    bitmap?.let {
                        selectedBitmap = it
                        imageView.setImageBitmap(it)
                        statusText.text = "✅ Fotoğraf çekildi"
                        resetStats()
                        // Otomatik analiz
                        analyzeImage()
                    }
                }
            }
        }
    }

    private fun loadImageFromUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            selectedBitmap = bitmap
            imageView.setImageBitmap(bitmap)
            statusText.text = "✅ Görüntü yüklendi"
            resetStats()
        } catch (e: Exception) {
            Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDemoImage() {
        statusText.text = "🎮 Demo görüntüsü oluşturuluyor..."
        progressBar.visibility = ProgressBar.VISIBLE

        Thread {
            val bitmap = createDemoImage()
            runOnUiThread {
                selectedBitmap = bitmap
                imageView.setImageBitmap(bitmap)
                statusText.text = "🎮 Demo görüntüsü hazır"
                progressBar.visibility = ProgressBar.GONE
                analyzeImage()
            }
        }.start()
    }

    private fun createDemoImage(): Bitmap {
        val width = 640
        val height = 480
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val random = java.util.Random(System.currentTimeMillis())
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val isNight = hour < 6 || hour >= 19

        // Arka plan renkleri
        val skyColor = if (isNight) Color.rgb(15, 15, 35) else Color.rgb(135, 180, 220)
        val roadColor = if (isNight) Color.rgb(35, 35, 40) else Color.rgb(65, 65, 70)
        val lineColor = if (isNight) Color.rgb(80, 80, 60) else Color.rgb(220, 220, 200)

        // Trafik yoğunluğu - saate göre değişken
        val trafficMultiplier = when (hour) {
            in 7..9 -> 2.2
            in 12..14 -> 1.4
            in 17..19 -> 2.5
            in 22..24, in 0..5 -> 0.3
            else -> 1.0
        }
        val vehicleCount = maxOf(1, minOf(18, ((3 + random.nextInt(6)) * trafficMultiplier).toInt()))

        // Piksel piksel çiz
        for (y in 0 until height) {
            for (x in 0 until width) {
                val noise = random.nextInt(10) - 5
                val color = when {
                    y < height / 4 -> { // Gökyüzü
                        if (isNight) Color.rgb(15 + noise, 15 + noise, 35 + noise)
                        else Color.rgb(135 + noise, 180 + noise, 220 + noise)
                    }
                    y < height / 3 -> { // Binalar/ufuk
                        if (isNight) Color.rgb(25 + noise, 25 + noise, 30 + noise)
                        else Color.rgb(110 + noise, 110 + noise, 120 + noise)
                    }
                    else -> { // Yol
                        if (isNight) Color.rgb(35 + noise, 35 + noise, 40 + noise)
                        else Color.rgb(65 + noise, 65 + noise, 70 + noise)
                    }
                }
                bitmap.setPixel(x, y, color)
            }
        }

        // Yol çizgileri
        for (y in height / 3 until height) {
            // Kenar çizgileri
            for (dx in 0..3) {
                if (20 + dx < width) bitmap.setPixel(20 + dx, y, lineColor)
                if (width - 25 + dx < width) bitmap.setPixel(width - 25 + dx, y, lineColor)
            }
            // Orta çizgi (kesikli)
            if ((y / 30) % 2 == 0) {
                for (dx in -2..2) {
                    val px = width / 2 + dx
                    if (px in 0 until width) bitmap.setPixel(px, y, lineColor)
                }
            }
        }

        // Araçları çiz
        val vehicleColors = if (isNight) {
            listOf(Color.rgb(40, 40, 45), Color.rgb(55, 55, 60), Color.rgb(30, 30, 35))
        } else {
            listOf(Color.WHITE, Color.rgb(200, 200, 200), Color.rgb(50, 50, 55),
                   Color.rgb(180, 30, 30), Color.rgb(30, 30, 160), Color.rgb(255, 200, 50))
        }

        val placedVehicles = mutableListOf<IntArray>() // x, y, w, h

        for (i in 0 until vehicleCount) {
            var attempts = 0
            while (attempts < 30) {
                val vx = 40 + random.nextInt(width - 140)
                val vy = height / 3 + 30 + random.nextInt(height / 2 - 50)
                val vw = 45 + random.nextInt(35)
                val vh = 22 + random.nextInt(12)

                // Çakışma kontrolü
                val overlaps = placedVehicles.any { v ->
                    val dx = abs((vx + vw/2) - (v[0] + v[2]/2))
                    val dy = abs((vy + vh/2) - (v[1] + v[3]/2))
                    dx < (vw + v[2]) / 2 + 15 && dy < (vh + v[3]) / 2 + 15
                }

                if (!overlaps) {
                    placedVehicles.add(intArrayOf(vx, vy, vw, vh))
                    val vehicleColor = vehicleColors[random.nextInt(vehicleColors.size)]

                    // Araç gövdesi
                    for (dy in 0 until vh) {
                        for (dx in 0 until vw) {
                            val px = vx + dx
                            val py = vy + dy
                            if (px in 0 until width && py in height/3 until height) {
                                bitmap.setPixel(px, py, vehicleColor)
                            }
                        }
                    }

                    // Cam (üst kısım)
                    val windowColor = Color.rgb(100, 140, 180)
                    for (dy in 2 until vh / 3) {
                        for (dx in vw / 4 until vw * 3 / 4) {
                            val px = vx + dx
                            val py = vy + dy
                            if (px in 0 until width && py in 0 until height) {
                                bitmap.setPixel(px, py, windowColor)
                            }
                        }
                    }

                    // Gece farları
                    if (isNight) {
                        val headlightColor = Color.rgb(255, 255, 200)
                        val taillightColor = Color.rgb(255, 50, 50)

                        for (lx in 0..4) {
                            for (ly in 0..3) {
                                val hx = vx + lx
                                val hy = vy + vh/2 + ly
                                val tx = vx + vw - 5 + lx
                                if (hx in 0 until width && hy in 0 until height) {
                                    bitmap.setPixel(hx, hy, headlightColor)
                                }
                                if (tx in 0 until width && hy in 0 until height) {
                                    bitmap.setPixel(tx, hy, taillightColor)
                                }
                            }
                        }
                    }
                    break
                }
                attempts++
            }
        }

        // Zaman damgası
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        // Basit metin (bitmap'e yazamıyoruz ama log olarak)

        return bitmap
    }

    private fun analyzeImage() {
        val bitmap = selectedBitmap
        if (bitmap == null) {
            Toast.makeText(this, "Önce bir görüntü seçin", Toast.LENGTH_SHORT).show()
            return
        }
        progressBar.visibility = ProgressBar.VISIBLE
        statusText.text = "🔄 Görüntü analiz ediliyor..."

        Thread {
            try {
                val result = performAdvancedAnalysis(bitmap)
                runOnUiThread {
                    progressBar.visibility = ProgressBar.GONE
                    val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    statusText.text = "✅ Analiz tamamlandı - $timestamp"

                    // İstatistikleri güncelle
                    vehicleCountText.text = result.vehicleCount.toString()
                    peopleCountText.text = result.peopleCount.toString()
                    trafficLevelText.text = result.trafficLevel
                    crowdLevelText.text = result.crowdLevel

                    // Renkleri güncelle
                    trafficLevelText.setTextColor(getLevelColor(result.trafficLevel))
                    crowdLevelText.setTextColor(getLevelColor(result.crowdLevel))

                    resultText.text = result.fullReport
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressBar.visibility = ProgressBar.GONE
                    statusText.text = "❌ Analiz hatası"
                    resultText.text = "Hata: ${e.message}"
                }
            }
        }.start()
    }

    private fun getLevelColor(level: String): Int {
        return when {
            level.contains("ÇOK YOĞUN") || level.contains("🔴") -> Color.parseColor("#F44336")
            level.contains("YOĞUN") || level.contains("🟠") -> Color.parseColor("#FF9800")
            level.contains("ORTA") || level.contains("🟡") -> Color.parseColor("#FFEB3B")
            level.contains("AZ") || level.contains("HAFİF") || level.contains("🟢") -> Color.parseColor("#4CAF50")
            else -> Color.WHITE
        }
    }

    data class AnalysisResult(
        val vehicleCount: Int,
        val peopleCount: Int,
        val trafficLevel: String,
        val crowdLevel: String,
        val fullReport: String
    )

    private fun performAdvancedAnalysis(bitmap: Bitmap): AnalysisResult {
        val w = bitmap.width
        val h = bitmap.height
        val roadStartY = h / 3

        // Temel istatistikler
        var redSum = 0L; var greenSum = 0L; var blueSum = 0L
        var darkPixels = 0; var veryDarkPixels = 0; var grayPixels = 0
        var vehicleColorPixels = 0

        for (y in 0 until h step 2) {
            for (x in 0 until w step 2) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                redSum += r; greenSum += g; blueSum += b

                val brightness = (r + g + b) / 3
                val saturation = maxOf(r, g, b) - minOf(r, g, b)

                if (brightness < 30) veryDarkPixels++
                if (brightness < 60) darkPixels++
                if (saturation < 30) grayPixels++

                // Yol bölgesinde araç renkleri tespit et
                if (y > roadStartY) {
                    val isVehicleColor = when {
                        brightness > 180 && saturation < 50 -> true // Beyaz
                        brightness in 25..70 && saturation < 25 -> true // Siyah
                        r > 100 && r > g + 30 && r > b + 30 -> true // Kırmızı
                        b > 90 && b > r + 15 -> true // Mavi
                        brightness in 130..190 && saturation < 35 -> true // Gümüş
                        r > 150 && g > 100 && b < 80 -> true // Sarı/turuncu
                        else -> false
                    }
                    if (isVehicleColor) vehicleColorPixels++
                }
            }
        }

        val samples = (w / 2) * (h / 2)
        val avgR = (redSum / samples).toInt()
        val avgG = (greenSum / samples).toInt()
        val avgB = (blueSum / samples).toInt()
        val brightness = (avgR + avgG + avgB) / 3
        val darkRatio = darkPixels.toDouble() / samples
        val hazeLevel = grayPixels.toDouble() / samples

        // Edge detection - yol bölgesi
        var roadEdges = 0
        for (y in roadStartY + 1 until h - 1 step 3) {
            for (x in 1 until w - 1 step 3) {
                val l = getBrightness(bitmap.getPixel(x - 1, y))
                val r = getBrightness(bitmap.getPixel(x + 1, y))
                val t = getBrightness(bitmap.getPixel(x, y - 1))
                val b = getBrightness(bitmap.getPixel(x, y + 1))
                val grad = sqrt(((r - l) * (r - l) + (b - t) * (b - t)).toDouble())
                if (grad > 30) roadEdges++
            }
        }

        val roadSamples = ((w / 3) * ((h - roadStartY) / 3))
        val roadEdgeDensity = if (roadSamples > 0) roadEdges.toDouble() / roadSamples else 0.0

        // ===== ARAÇ SAYIMI (Blob Detection) =====
        val vehicleBlobs = countVehicleBlobs(bitmap, roadStartY)

        // ===== TRAFİK SEVİYESİ =====
        val trafficLevel = when {
            vehicleBlobs >= 12 -> "ÇOK YOĞUN 🔴"
            vehicleBlobs >= 6 -> "YOĞUN 🟠"
            vehicleBlobs >= 3 -> "ORTA 🟡"
            vehicleBlobs >= 1 -> "HAFİF 🟢"
            else -> "BOŞ ⚪"
        }

        // ===== KALABALIK SEVİYESİ =====
        val estimatedPeople = (roadEdgeDensity * 80).toInt()
        val crowdLevel = when {
            roadEdgeDensity > 0.45 -> "ÇOK YOĞUN 🔴"
            roadEdgeDensity > 0.28 -> "YOĞUN 🟠"
            roadEdgeDensity > 0.15 -> "ORTA 🟡"
            roadEdgeDensity > 0.08 -> "AZ 🟢"
            else -> "BOŞ ⚪"
        }

        // ===== ZAMAN TAHMİNİ =====
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val isNight = brightness < 60 || (currentHour < 6 || currentHour >= 20)

        val timeEstimate = when {
            brightness < 40 -> "🌙 Gece"
            brightness > 150 -> "☀️ Güneşli"
            brightness > 100 -> "🌤️ Gündüz"
            else -> "⛅ Bulutlu"
        }

        // ===== HAVA KALİTESİ =====
        val airQuality = when {
            hazeLevel > 0.5 && !isNight -> "😷 KÖTÜ"
            hazeLevel > 0.35 && !isNight -> "😐 ORTA"
            hazeLevel > 0.2 -> "🙂 İYİ"
            else -> "😊 ÇOK İYİ"
        }

        val timestamp = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

        val fullReport = """
══════════════════════════════
📊 ANALİZ RAPORU
══════════════════════════════
🕐 $timestamp
📐 Boyut: ${w}x${h}

──────────────────────────────
🚗 ARAÇ ANALİZİ
──────────────────────────────
   Tespit Edilen: $vehicleBlobs araç
   Trafik: $trafficLevel

──────────────────────────────
👥 KALABALIK ANALİZİ
──────────────────────────────
   Tahmini: ~$estimatedPeople kişi
   Seviye: $crowdLevel

──────────────────────────────
🌤️ ORTAM
──────────────────────────────
   Zaman: $timeEstimate
   Parlaklık: $brightness
   Hava Kalitesi: $airQuality

══════════════════════════════
✅ Cihazda analiz edildi
🔒 Veri sunucuya gönderilmedi
══════════════════════════════
        """.trimIndent()

        return AnalysisResult(
            vehicleCount = vehicleBlobs,
            peopleCount = estimatedPeople,
            trafficLevel = trafficLevel,
            crowdLevel = crowdLevel,
            fullReport = fullReport
        )
    }

    /**
     * Gelişmiş araç sayımı - Dikdörtgen blob algılama
     * Gerçek araçları tespit etmek için daha katı kurallar
     */
    private fun countVehicleBlobs(bitmap: Bitmap, roadStartY: Int): Int {
        val w = bitmap.width
        val h = bitmap.height

        // Görüntü çok küçükse
        if (w < 100 || h < 100) return 0

        val scale = 6 // Daha büyük ölçek = daha az gürültü
        val scaledW = w / scale
        val scaledH = (h - roadStartY) / scale

        if (scaledW <= 10 || scaledH <= 10) return 0

        // Önce yol bölgesinin ortalama rengini hesapla
        var roadBrightSum = 0L
        var roadSampleCount = 0
        for (sy in 0 until scaledH step 2) {
            for (sx in 0 until scaledW step 2) {
                val x = sx * scale
                val y = roadStartY + sy * scale
                if (x < w && y < h) {
                    val pixel = bitmap.getPixel(x, y)
                    roadBrightSum += (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                    roadSampleCount++
                }
            }
        }
        val avgRoadBrightness = if (roadSampleCount > 0) (roadBrightSum / roadSampleCount).toInt() else 80

        val mask = Array(scaledH) { BooleanArray(scaledW) }
        val visited = Array(scaledH) { BooleanArray(scaledW) }

        // Araç pikseli tespit - yol renginden belirgin şekilde farklı olanlar
        for (sy in 0 until scaledH) {
            for (sx in 0 until scaledW) {
                val x = sx * scale
                val y = roadStartY + sy * scale

                if (x >= w || y >= h) continue

                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val brightness = (r + g + b) / 3
                val saturation = maxOf(r, g, b) - minOf(r, g, b)

                // Yol renginden ne kadar farklı?
                val diffFromRoad = abs(brightness - avgRoadBrightness)

                // Daha katı araç tespiti
                val isVehicle = when {
                    // Yol rengine çok yakınsa araç değil
                    diffFromRoad < 25 -> false
                    // Çok açık beyaz araç (yoldan çok farklı olmalı)
                    brightness > 200 && saturation < 30 && diffFromRoad > 40 -> true
                    // Çok koyu siyah araç
                    brightness < 45 && saturation < 20 && diffFromRoad > 30 -> true
                    // Kırmızı araç (belirgin)
                    r > 140 && r > g + 50 && r > b + 50 -> true
                    // Mavi araç (belirgin)
                    b > 120 && b > r + 40 && b > g + 20 -> true
                    // Gümüş/metalik (yoldan farklı)
                    brightness in 150..200 && saturation < 25 && diffFromRoad > 35 -> true
                    // Sarı/turuncu
                    r > 180 && g > 120 && b < 100 && saturation > 60 -> true
                    else -> false
                }

                mask[sy][sx] = isVehicle
            }
        }

        // Gürültü azaltma - tek pikselleri temizle
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

        // Blob sayma - daha katı boyut kuralları
        var blobCount = 0
        // Bir araç en az 8 piksel (ölçeklenmiş) olmalı
        // Maksimum 80 piksel (dev bir blob değil)
        val minBlobSize = 8
        val maxBlobSize = 80

        for (sy in 0 until scaledH) {
            for (sx in 0 until scaledW) {
                if (cleanMask[sy][sx] && !visited[sy][sx]) {
                    val (blobSize, bounds) = floodFillWithBounds(cleanMask, visited, sx, sy, scaledW, scaledH)

                    // Boyut kontrolü
                    if (blobSize in minBlobSize..maxBlobSize) {
                        // En-boy oranı kontrolü (araçlar dikdörtgen olmalı)
                        val blobWidth = bounds[2] - bounds[0] + 1
                        val blobHeight = bounds[3] - bounds[1] + 1
                        val aspectRatio = if (blobHeight > 0) blobWidth.toFloat() / blobHeight else 0f

                        // Araçlar genellikle yatay (en/boy > 1) veya makul bir oran
                        if (aspectRatio in 0.3f..5f && blobWidth >= 2 && blobHeight >= 2) {
                            blobCount++
                        }
                    }
                }
            }
        }

        // Maksimum makul araç sayısı (görüntü başına)
        return minOf(blobCount, 25)
    }

    private fun floodFillWithBounds(mask: Array<BooleanArray>, visited: Array<BooleanArray>,
                          startX: Int, startY: Int, w: Int, h: Int): Pair<Int, IntArray> {
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.addLast(Pair(startX, startY))
        var size = 0
        var minX = startX; var maxX = startX
        var minY = startY; var maxY = startY

        while (stack.isNotEmpty()) {
            val (x, y) = stack.removeLast()

            if (x < 0 || x >= w || y < 0 || y >= h) continue
            if (visited[y][x] || !mask[y][x]) continue

            visited[y][x] = true
            size++

            minX = minOf(minX, x); maxX = maxOf(maxX, x)
            minY = minOf(minY, y); maxY = maxOf(maxY, y)

            stack.addLast(Pair(x + 1, y))
            stack.addLast(Pair(x - 1, y))
            stack.addLast(Pair(x, y + 1))
            stack.addLast(Pair(x, y - 1))
        }

        return Pair(size, intArrayOf(minX, minY, maxX, maxY))
    }

    private fun getBrightness(pixel: Int): Int {
        return (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
    }
}

