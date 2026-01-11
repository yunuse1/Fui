package com.example.services

import com.example.models.ImageAnalysisResult
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.Base64
import javax.imageio.ImageIO

object ImageService {

    /**
     * Converts Base64 encoded string to BufferedImage
     */
    fun decodeBase64Image(base64String: String): BufferedImage? {
        return try {
            val imageBytes = Base64.getDecoder().decode(base64String)
            val inputStream = ByteArrayInputStream(imageBytes)
            ImageIO.read(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Analyzes the image and returns results
     */
    fun analyzeImage(img: BufferedImage): ImageAnalysisResult {
        val w = img.width
        val h = img.height
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var brightnessSum = 0.0
        val histogram = mutableMapOf<Int, Int>()

        for (y in 0 until h) {
            for (x in 0 until w) {
                val rgb = img.getRGB(x, y)
                val c = Color(rgb, true)
                val r = c.red
                val g = c.green
                val b = c.blue
                rSum += r
                gSum += g
                bSum += b
                val brightness = (0.299 * r + 0.587 * g + 0.114 * b)
                brightnessSum += brightness
                val bin = (brightness / 10).toInt()
                histogram[bin] = (histogram[bin] ?: 0) + 1
            }
        }

        val pixelCount = (w.toLong() * h.toLong())
        val avgR = (rSum / pixelCount).toInt()
        val avgG = (gSum / pixelCount).toInt()
        val avgB = (bSum / pixelCount).toInt()
        val avgBrightness = brightnessSum / pixelCount

        val normalizedHistogram = mutableMapOf<String, Int>()
        for ((k, v) in histogram) {
            val kk = k.coerceIn(0, 25)
            val key = "bin_$kk"
            normalizedHistogram[key] = (normalizedHistogram[key] ?: 0) + v
        }

        return ImageAnalysisResult(
            width = w,
            height = h,
            avgRed = avgR,
            avgGreen = avgG,
            avgBlue = avgB,
            avgBrightness = (avgBrightness * 100 / 255.0),
            histogram = normalizedHistogram
        )
    }

    /**
     * Validates image dimensions
     */
    fun validateImageSize(img: BufferedImage, maxWidth: Int = 4096, maxHeight: Int = 4096): Boolean {
        return img.width <= maxWidth && img.height <= maxHeight
    }

    /**
     * Calculates total pixel count of the image
     */
    fun getPixelCount(img: BufferedImage): Long {
        return img.width.toLong() * img.height.toLong()
    }
}

