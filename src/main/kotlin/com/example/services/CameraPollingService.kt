package com.example.services

import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.imageio.ImageIO

private val cameraLogger = LoggerFactory.getLogger("CameraPollingService")

object CameraPollingService {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    @Volatile
    var running: Boolean = false
        private set

    @Volatile
    private var _lastImageBytes: ByteArray? = null

    @Volatile
    private var _lastEstimatedPeople: Int? = null

    @Volatile
    private var _lastUpdatedMs: Long? = null

    fun start(cameraUrl: String, sampleRateMs: Long = 2000L): Boolean {
        synchronized(this) {
            if (running) return false
            running = true

            job = scope.launch {
                cameraLogger.info("Camera polling started for: $cameraUrl, sampleRate=${sampleRateMs}ms")

                while (isActive) {
                    val start = System.currentTimeMillis()
                    try {
                        val bytes = fetchImageBytes(cameraUrl)
                        if (bytes != null) {
                            _lastImageBytes = bytes

                            val img = try {
                                ImageIO.read(ByteArrayInputStream(bytes))
                            } catch (e: Exception) {
                                cameraLogger.warn("Failed to decode image: ${e.message}")
                                null
                            }

                            if (img != null) {
                                // AI analizi - sadece kalabalık / insan tahmini kullanıyoruz
                                try {
                                    val crowd = AIAnalysisService.analyzeCrowd(img)
                                    _lastEstimatedPeople = crowd.estimatedPeopleCount
                                    _lastUpdatedMs = System.currentTimeMillis()

                                    // Veritabanına kaydet (basicAnalysis null, vehicleDetection null)
                                    val savedId = DatabaseService.saveAnalysisResult(
                                        filename = "camera-snapshot",
                                        deviceId = null,
                                        location = cameraUrl,
                                        basicAnalysis = null,
                                        vehicleDetection = null,
                                        crowdAnalysis = crowd,
                                        airQuality = null,
                                        trafficAnalysis = null,
                                        processingTimeMs = System.currentTimeMillis() - start
                                    )

                                    if (savedId != null) {
                                        cameraLogger.info("Saved camera analysis to DB id=$savedId people=${crowd.estimatedPeopleCount}")
                                    }
                                } catch (e: Exception) {
                                    cameraLogger.warn("Analysis failed: ${e.message}")
                                }
                            }
                        } else {
                            cameraLogger.debug("No image bytes fetched from $cameraUrl")
                        }
                    } catch (e: CancellationException) {
                        cameraLogger.info("Camera polling cancelled")
                        break
                    } catch (e: Exception) {
                        cameraLogger.warn("Error during camera polling: ${e.message}")
                    }

                    val elapsed = System.currentTimeMillis() - start
                    val delayMs = (sampleRateMs - elapsed).coerceAtLeast(200L)
                    delay(delayMs)
                }

                cameraLogger.info("Camera polling loop ended for: $cameraUrl")
            }

            return true
        }
    }

    fun stop(): Boolean {
        synchronized(this) {
            if (!running) return false
            job?.cancel()
            job = null
            running = false
            cameraLogger.info("Camera polling stopped")
            return true
        }
    }

    fun getPreviewBytes(): ByteArray? = _lastImageBytes

    fun getStatus(): CameraStatus {
        return CameraStatus(
            running = running,
            lastEstimatedPeople = _lastEstimatedPeople,
            lastUpdatedMs = _lastUpdatedMs
        )
    }

    data class CameraStatus(
        val running: Boolean,
        val lastEstimatedPeople: Int?,
        val lastUpdatedMs: Long?
    )

    // Basit fetcher: önce doğrudan image content-type kontrolü yapar; eğer HTML dönmüşse sayfadaki ilk jpg/png src'yi regex ile bulup tekrar dener
    private fun fetchImageBytes(urlStr: String, timeoutMs: Int = 8000): ByteArray? {
        try {
            val url = URL(urlStr)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "FuiCameraPoller/1.0")
            }

            conn.connect()
            val contentType = conn.contentType ?: ""

            conn.inputStream.use { input ->
                val bytes = input.readBytes()
                conn.disconnect()

                if (contentType.startsWith("image")) {
                    return bytes
                }

                // Eğer HTML geldiyse, aranan pattern'leri kontrol et
                val html = String(bytes)
                val regex = "(https?://[^\\s>]+\\.(?:jpg|jpeg|png))".toRegex(RegexOption.IGNORE_CASE)
                val match = regex.find(html)
                if (match != null) {
                    val imgUrl = match.groupValues[1]
                    cameraLogger.debug("Found image URL in HTML: $imgUrl")
                    return fetchImageBytes(imgUrl, timeoutMs)
                }

                // Başka olasılık: data-src veya srcset içinde yerel yollar
                val srcRegex = "src=\\\"([^\\\"]+\\.(?:jpg|jpeg|png))\\\"".toRegex(RegexOption.IGNORE_CASE)
                val srcMatch = srcRegex.find(html)
                if (srcMatch != null) {
                    var imgUrl = srcMatch.groupValues[1]
                    if (imgUrl.startsWith("//")) imgUrl = "https:$imgUrl"
                    if (imgUrl.startsWith("/")) {
                        // relative to base
                        val base = url.protocol + "://" + url.host
                        imgUrl = base + imgUrl
                    }
                    cameraLogger.debug("Found image URL in HTML (src): $imgUrl")
                    return fetchImageBytes(imgUrl, timeoutMs)
                }

                return null
            }
        } catch (e: Exception) {
            cameraLogger.debug("fetchImageBytes error: ${e.message}")
            return null
        }
    }
}
