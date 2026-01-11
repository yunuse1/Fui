package com.urban.insights

import kotlinx.serialization.Serializable

@Serializable
enum class DataType {
    NOISE_LEVEL,
    AIR_QUALITY,
    CROWD_DENSITY,
    TRAFFIC_FLOW
}

@Serializable
enum class AggregationType {
    AVERAGE,
    SUM,
    MAX,
    COUNT,
    HISTOGRAM
}

@Serializable
data class PrivacyContract(
    val dataType: DataType,
    val aggregation: AggregationType,
    val epsilon: Double, // Differential Privacy budget (e.g., 0.1 - 1.0)
    val minBatchSize: Int, // Minimum number of contributions before aggregation
    val description: String
)

class PrivacyContractBuilder {
    var dataType: DataType = DataType.NOISE_LEVEL
    var aggregation: AggregationType = AggregationType.AVERAGE
    var epsilon: Double = 1.0
    var minBatchSize: Int = 10
    var description: String = ""

    fun build(): PrivacyContract {
        return PrivacyContract(dataType, aggregation, epsilon, minBatchSize, description)
    }
}

fun privacyContract(block: PrivacyContractBuilder.() -> Unit): PrivacyContract {
    val builder = PrivacyContractBuilder()
    builder.block()
    return builder.build()
}
