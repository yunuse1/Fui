package com.example.models

import kotlinx.serialization.Serializable

// (Models içeriğini buraya kopyaladım - sunucu tarafında ortak kullanılacak)
@Serializable
data class ImageAnalysisResult(
    val width: Int,
    val height: Int,
    val avgRed: Int,
    val avgGreen: Int,
    val avgBlue: Int,
    val avgBrightness: Double,
    val histogram: Map<String, Int>
)

// Daha fazla model eklenebilir...
