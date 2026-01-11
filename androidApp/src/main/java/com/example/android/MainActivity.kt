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
// Import shared module for cross-platform analysis logic
import com.urban.insights.ImageAnalyzer
import com.urban.insights.TrafficLevel
import com.urban.insights.CrowdLevel
import com.urban.insights.SceneType

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

        imageView = findViewById(R.id.imageView)
        resultText = findViewById(R.id.resultText)
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        vehicleCountText = findViewById(R.id.vehicleCountText)
        peopleCountText = findViewById(R.id.peopleCountText)
        trafficLevelText = findViewById(R.id.trafficLevelText)
        crowdLevelText = findViewById(R.id.crowdLevelText)

        findViewById<Button>(R.id.galleryButton).setOnClickListener { openGallery() }
        findViewById<Button>(R.id.cameraButton).setOnClickListener { openCamera() }
        findViewById<Button>(R.id.analyzeButton).setOnClickListener { analyzeImage() }
        findViewById<Button>(R.id.demoButton).setOnClickListener { loadDemoImage() }

        checkPermissions()

        statusText.text = "📸 Select or take a photo"
        resetStats()
    }

    private fun resetStats() {
        vehicleCountText.text = "0"
        peopleCountText.text = "0"
        trafficLevelText.text = "Waiting..."
        crowdLevelText.text = "Waiting..."
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
            Toast.makeText(this, "Camera not found", Toast.LENGTH_SHORT).show()
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
                        analyzeImage()
                    }
                }
                REQUEST_IMAGE_CAPTURE -> {
                    val bitmap = data?.extras?.get("data") as? Bitmap
                    bitmap?.let {
                        selectedBitmap = it
                        imageView.setImageBitmap(it)
                        statusText.text = "✅ Photo captured"
                        resetStats()
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
            statusText.text = "✅ Image loaded"
            resetStats()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDemoImage() {
        statusText.text = "🎮 Creating demo image..."
        progressBar.visibility = ProgressBar.VISIBLE

        Thread {
            val bitmap = createDemoImage()
            runOnUiThread {
                selectedBitmap = bitmap
                imageView.setImageBitmap(bitmap)
                statusText.text = "🎮 Demo image ready"
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

        val lineColor = if (isNight) Color.rgb(80, 80, 60) else Color.rgb(220, 220, 200)

        val trafficMultiplier = when (hour) {
            in 7..9 -> 2.2
            in 12..14 -> 1.4
            in 17..19 -> 2.5
            in 22..24, in 0..5 -> 0.3
            else -> 1.0
        }
        val vehicleCount = maxOf(1, minOf(18, ((3 + random.nextInt(6)) * trafficMultiplier).toInt()))

        for (y in 0 until height) {
            for (x in 0 until width) {
                val noise = random.nextInt(10) - 5
                val color = when {
                    y < height / 4 -> {
                        if (isNight) Color.rgb(15 + noise, 15 + noise, 35 + noise)
                        else Color.rgb(135 + noise, 180 + noise, 220 + noise)
                    }
                    y < height / 3 -> {
                        if (isNight) Color.rgb(25 + noise, 25 + noise, 30 + noise)
                        else Color.rgb(110 + noise, 110 + noise, 120 + noise)
                    }
                    else -> {
                        if (isNight) Color.rgb(35 + noise, 35 + noise, 40 + noise)
                        else Color.rgb(65 + noise, 65 + noise, 70 + noise)
                    }
                }
                bitmap.setPixel(x, y, color)
            }
        }

        for (y in height / 3 until height) {
            for (dx in 0..3) {
                if (20 + dx < width) bitmap.setPixel(20 + dx, y, lineColor)
                if (width - 25 + dx < width) bitmap.setPixel(width - 25 + dx, y, lineColor)
            }
            if ((y / 30) % 2 == 0) {
                for (dx in -2..2) {
                    val px = width / 2 + dx
                    if (px in 0 until width) bitmap.setPixel(px, y, lineColor)
                }
            }
        }

        val vehicleColors = if (isNight) {
            listOf(Color.rgb(40, 40, 45), Color.rgb(55, 55, 60), Color.rgb(30, 30, 35))
        } else {
            listOf(Color.WHITE, Color.rgb(200, 200, 200), Color.rgb(50, 50, 55),
                   Color.rgb(180, 30, 30), Color.rgb(30, 30, 160), Color.rgb(255, 200, 50))
        }

        val placedVehicles = mutableListOf<IntArray>()

        for (i in 0 until vehicleCount) {
            var attempts = 0
            while (attempts < 30) {
                val vx = 40 + random.nextInt(width - 140)
                val vy = height / 3 + 30 + random.nextInt(height / 2 - 50)
                val vw = 45 + random.nextInt(35)
                val vh = 22 + random.nextInt(12)

                val overlaps = placedVehicles.any { v ->
                    val dx = abs((vx + vw/2) - (v[0] + v[2]/2))
                    val dy = abs((vy + vh/2) - (v[1] + v[3]/2))
                    dx < (vw + v[2]) / 2 + 15 && dy < (vh + v[3]) / 2 + 15
                }

                if (!overlaps) {
                    placedVehicles.add(intArrayOf(vx, vy, vw, vh))
                    val vehicleColor = vehicleColors[random.nextInt(vehicleColors.size)]

                    for (dy in 0 until vh) {
                        for (dx in 0 until vw) {
                            val px = vx + dx
                            val py = vy + dy
                            if (px in 0 until width && py in height/3 until height) {
                                bitmap.setPixel(px, py, vehicleColor)
                            }
                        }
                    }

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

        return bitmap
    }

    private fun analyzeImage() {
        val bitmap = selectedBitmap
        if (bitmap == null) {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            return
        }
        progressBar.visibility = ProgressBar.VISIBLE
        statusText.text = "🔄 Analyzing..."

        Thread {
            try {
                val result = performSmartAnalysis(bitmap)
                runOnUiThread {
                    progressBar.visibility = ProgressBar.GONE
                    val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                    statusText.text = "✅ Analysis completed - $timestamp"

                    vehicleCountText.text = result.vehicleCount.toString()
                    peopleCountText.text = result.peopleCount.toString()
                    trafficLevelText.text = result.trafficLevel
                    crowdLevelText.text = result.crowdLevel

                    trafficLevelText.setTextColor(getLevelColor(result.trafficLevel))
                    crowdLevelText.setTextColor(getLevelColor(result.crowdLevel))

                    resultText.text = result.fullReport
                }
            } catch (e: Exception) {
                runOnUiThread {
                    progressBar.visibility = ProgressBar.GONE
                    statusText.text = "❌ Analysis error"
                    resultText.text = "Error: ${e.message}"
                }
            }
        }.start()
    }

    data class AnalysisResult(
        val vehicleCount: Int,
        val peopleCount: Int,
        val trafficLevel: String,
        val crowdLevel: String,
        val fullReport: String,
        val sceneType: String,
        val isValidTrafficScene: Boolean
    )

    private fun performSmartAnalysis(bitmap: Bitmap): AnalysisResult {
        val sceneAnalysis = analyzeSceneType(bitmap)

        return if (sceneAnalysis.isTrafficScene) {
            performTrafficAnalysis(bitmap, sceneAnalysis)
        } else {
            createNonTrafficResult(bitmap, sceneAnalysis)
        }
    }

    data class SceneAnalysis(
        val isTrafficScene: Boolean,
        val isOutdoor: Boolean,
        val isIndoor: Boolean,
        val sceneType: String,
        val confidence: Double,
        val skyRatio: Double,
        val roadRatio: Double,
        val horizontalLines: Int,
        val avgBrightness: Int,
        val colorVariety: Double
    )

    private fun analyzeSceneType(bitmap: Bitmap): SceneAnalysis {
        val w = bitmap.width
        val h = bitmap.height

        var skyPixels = 0
        var roadPixels = 0
        var brownPixels = 0
        var greenPixels = 0
        var totalSamples = 0

        var redSum = 0L; var greenSum = 0L; var blueSum = 0L
        val colorSet = mutableSetOf<Int>()

        for (y in 0 until h / 3 step 4) {
            for (x in 0 until w step 4) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                if (b > 150 && b > r + 20 && b > g - 30 && g > 100) skyPixels++
                if (r > 180 && g > 180 && b > 180 && abs(r - g) < 30 && abs(g - b) < 30) skyPixels++
                totalSamples++
            }
        }

        for (y in h / 3 until h step 4) {
            for (x in 0 until w step 4) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val brightness = (r + g + b) / 3
                val saturation = maxOf(r, g, b) - minOf(r, g, b)

                redSum += r; greenSum += g; blueSum += b
                colorSet.add((r / 32) * 1000000 + (g / 32) * 1000 + (b / 32))

                if (brightness in 40..120 && saturation < 40) roadPixels++
                if (r > g && r > b && r in 80..200 && g in 50..150 && saturation in 20..100) brownPixels++
                if (g > r && g > b && g > 80 && saturation > 30) greenPixels++
                totalSamples++
            }
        }

        var horizontalLines = 0
        for (y in h / 4 until h * 3 / 4 step 10) {
            var linePixels = 0
            for (x in 10 until w - 10) {
                val pixel = bitmap.getPixel(x, y)
                val brightness = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                if (brightness > 180) linePixels++
            }
            if (linePixels > w * 0.3) horizontalLines++
        }

        val skyRatio = skyPixels.toDouble() / (totalSamples / 3)
        val roadRatio = roadPixels.toDouble() / (totalSamples * 2 / 3)
        val brownRatio = brownPixels.toDouble() / (totalSamples * 2 / 3)
        val greenRatio = greenPixels.toDouble() / (totalSamples * 2 / 3)
        val avgBrightness = ((redSum + greenSum + blueSum) / (totalSamples * 3)).toInt()
        val colorVariety = colorSet.size.toDouble() / 100

        // Use shared module for scene classification
        val sharedSceneType = ImageAnalyzer.classifyScene(
            skyRatio, roadRatio, greenRatio, brownRatio, avgBrightness, colorVariety
        )
        val isTrafficScene = sharedSceneType == SceneType.TRAFFIC
        val isOutdoor = sharedSceneType == SceneType.OUTDOOR || sharedSceneType == SceneType.TRAFFIC || sharedSceneType == SceneType.NATURE
        val isIndoor = sharedSceneType == SceneType.INDOOR || sharedSceneType == SceneType.INDOOR_HISTORIC

        // Use shared module for confidence calculation
        val confidence = ImageAnalyzer.calculateConfidence(sharedSceneType, skyRatio, roadRatio)

        val sceneType = "${sharedSceneType.emoji} ${sharedSceneType.description}"

        return SceneAnalysis(
            isTrafficScene = isTrafficScene,
            isOutdoor = isOutdoor,
            isIndoor = isIndoor,
            sceneType = sceneType,
            confidence = confidence,
            skyRatio = skyRatio,
            roadRatio = roadRatio,
            horizontalLines = horizontalLines,
            avgBrightness = avgBrightness,
            colorVariety = colorVariety
        )
    }

    private fun performTrafficAnalysis(bitmap: Bitmap, scene: SceneAnalysis): AnalysisResult {
        val w = bitmap.width
        val h = bitmap.height
        val roadStartY = h / 3

        val vehicleBlobs = countVehicleBlobs(bitmap, roadStartY)

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

        val reliabilityFactor = scene.confidence
        val adjustedVehicles = (vehicleBlobs * reliabilityFactor).toInt()

        // Use shared module for traffic level determination
        val sharedTrafficLevel = ImageAnalyzer.determineTrafficLevel(adjustedVehicles, true)
        val trafficLevel = "${sharedTrafficLevel.description} ${sharedTrafficLevel.emoji}"

        val estimatedPeople = (roadEdgeDensity * 50 * reliabilityFactor).toInt()
        // Use shared module for crowd level determination
        val sharedCrowdLevel = ImageAnalyzer.determineCrowdLevel(estimatedPeople)
        val crowdLevel = "${sharedCrowdLevel.description} ${sharedCrowdLevel.emoji}"

        val timestamp = java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

        val fullReport = """
📊 TRAFFIC ANALYSIS REPORT
$timestamp | ${w}x${h}

🎯 SCENE DETECTION
   Type: ${scene.sceneType}
   Confidence: ${(scene.confidence * 100).toInt()}%

🚗 VEHICLE ANALYSIS
   Detected: $vehicleBlobs vehicles
   Adjusted: $adjustedVehicles vehicles
   Traffic: $trafficLevel

👥 CROWD ANALYSIS
   Estimated: ~$estimatedPeople people
   Level: $crowdLevel

✅ Analysis completed on device
🔒 No data sent to server
        """.trimIndent()

        return AnalysisResult(
            vehicleCount = adjustedVehicles,
            peopleCount = estimatedPeople,
            trafficLevel = trafficLevel,
            crowdLevel = crowdLevel,
            fullReport = fullReport,
            sceneType = scene.sceneType,
            isValidTrafficScene = true
        )
    }

    private fun createNonTrafficResult(bitmap: Bitmap, scene: SceneAnalysis): AnalysisResult {
        val w = bitmap.width
        val h = bitmap.height
        val timestamp = java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())

        val estimatedPeople = countPeopleInScene(bitmap)

        // Use shared module for crowd level determination
        val sharedCrowdLevel = ImageAnalyzer.determineCrowdLevel(estimatedPeople)
        val crowdLevel = "${sharedCrowdLevel.description} ${sharedCrowdLevel.emoji}"

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

✅ Analysis completed on device
🔒 No data sent to server
        """.trimIndent()

        return AnalysisResult(
            vehicleCount = 0,
            peopleCount = estimatedPeople,
            trafficLevel = "INDOOR 🏛️",
            crowdLevel = crowdLevel,
            fullReport = fullReport,
            sceneType = scene.sceneType,
            isValidTrafficScene = false
        )
    }

    private fun countPeopleInScene(bitmap: Bitmap): Int {
        val w = bitmap.width
        val h = bitmap.height

        var skinTonePixels = 0
        var totalEdges = 0

        for (y in 0 until h step 3) {
            for (x in 0 until w step 3) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                // Use shared module for skin tone detection
                if (ImageAnalyzer.isSkinTone(r, g, b)) skinTonePixels++
            }
        }

        for (y in 1 until h - 1 step 4) {
            for (x in 1 until w - 1 step 4) {
                val left = getBrightness(bitmap.getPixel(x - 1, y))
                val right = getBrightness(bitmap.getPixel(x + 1, y))
                val top = getBrightness(bitmap.getPixel(x, y - 1))
                val bottom = getBrightness(bitmap.getPixel(x, y + 1))
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

    private fun getLevelColor(level: String): Int {
        return when {
            level.contains("INDOOR") || level.contains("🏛️") -> Color.parseColor("#9C27B0")
            level.contains("VERY HIGH") || level.contains("🔴") -> Color.parseColor("#F44336")
            level.contains("HIGH") || level.contains("🟠") -> Color.parseColor("#FF9800")
            level.contains("MEDIUM") || level.contains("🟡") -> Color.parseColor("#FFEB3B")
            level.contains("LOW") || level.contains("🟢") -> Color.parseColor("#4CAF50")
            else -> Color.WHITE
        }
    }

    private fun countVehicleBlobs(bitmap: Bitmap, roadStartY: Int): Int {
        val w = bitmap.width
        val h = bitmap.height

        if (w < 100 || h < 100) return 0

        val scale = 6
        val scaledW = w / scale
        val scaledH = (h - roadStartY) / scale

        if (scaledW <= 10 || scaledH <= 10) return 0

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

                val diffFromRoad = abs(brightness - avgRoadBrightness)

                // Use shared module for vehicle pixel detection
                val isVehicle = ImageAnalyzer.isVehiclePixel(r, g, b, avgRoadBrightness)

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
                    val (blobSize, bounds) = floodFillWithBounds(cleanMask, visited, sx, sy, scaledW, scaledH)

                    val blobWidth = bounds[2] - bounds[0] + 1
                    val blobHeight = bounds[3] - bounds[1] + 1

                    // Use shared module for blob validation
                    if (ImageAnalyzer.isValidVehicleBlob(blobSize, blobWidth, blobHeight)) {
                        blobCount++
                    }
                }
            }
        }

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

