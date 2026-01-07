package com.example.services

import com.example.models.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

private val logger = LoggerFactory.getLogger("DatabaseService")

/**
 * SQLite Veritabanı tabloları - Local-first, privacy-focused
 */
object AnalysisRecords : Table("analysis_record") {
    val id = integer("id").autoIncrement()
    val timestamp = varchar("timestamp", 50)  // ISO format string
    val filename = varchar("filename", 255)
    val deviceId = varchar("device_id", 100).nullable()
    val location = varchar("location", 255).nullable()

    // Temel görüntü analizi
    val imageWidth = integer("image_width").default(0)
    val imageHeight = integer("image_height").default(0)
    val avgBrightness = double("avg_brightness").default(0.0)

    // Araç tespiti
    val totalVehicles = integer("total_vehicles").default(0)
    val busCount = integer("bus_count").default(0)
    val carCount = integer("car_count").default(0)
    val truckCount = integer("truck_count").default(0)
    val motorcycleCount = integer("motorcycle_count").default(0)
    val bicycleCount = integer("bicycle_count").default(0)
    val vehicleDensity = varchar("vehicle_density", 50).nullable()

    // Kalabalık analizi
    val estimatedPeople = integer("estimated_people").default(0)
    val crowdDensityLevel = varchar("crowd_density_level", 50).nullable()
    val crowdDensityPercentage = double("crowd_density_percentage").default(0.0)

    // Hava kalitesi
    val hazeLevel = double("haze_level").default(0.0)
    val visibilityScore = double("visibility_score").default(0.0)
    val aqiCategory = varchar("aqi_category", 50).nullable()
    val aqiValue = integer("aqi_value").default(0)
    val smogDetected = bool("smog_detected").default(false)
    val dustDetected = bool("dust_detected").default(false)
    val fogDetected = bool("fog_detected").default(false)

    // Trafik analizi
    val congestionLevel = varchar("congestion_level", 50).nullable()
    val congestionPercentage = double("congestion_percentage").default(0.0)
    val roadOccupancy = double("road_occupancy").default(0.0)
    val incidentDetected = bool("incident_detected").default(false)

    // İşlem süresi
    val processingTimeMs = long("processing_time_ms").default(0)

    override val primaryKey = PrimaryKey(id)
}

/**
 * Local-first Veritabanı servisi
 * - SQLite kullanır (dosya tabanlı, sunucu gerektirmez)
 * - Veriler cihazda kalır (privacy-focused)
 * - In-memory cache ile hızlı erişim
 */
object DatabaseService {
    private var initialized = false
    private val dbPath = "data/fui_local.db"

    // In-memory cache - hızlı erişim için
    private val recentRecordsCache = ConcurrentHashMap<Int, AnalysisRecordDto>()
    private var cacheLastUpdated: Instant = Instant.now()

    /**
     * Local SQLite veritabanını başlatır
     */
    fun init() {
        if (initialized) return

        try {
            // data klasörünü oluştur
            val dataDir = File("data")
            if (!dataDir.exists()) {
                dataDir.mkdirs()
                logger.info("Created local data directory: ${dataDir.absolutePath}")
            }

            // SQLite bağlantısı - local file
            Database.connect(
                url = "jdbc:sqlite:$dbPath",
                driver = "org.sqlite.JDBC"
            )

            // Tabloları oluştur
            transaction {
                SchemaUtils.create(AnalysisRecords)
            }

            initialized = true
            logger.info("Local SQLite database initialized: $dbPath")
            logger.info("Privacy mode: All data stays on this device")
        } catch (e: Exception) {
            logger.error("Failed to initialize local database: ${e.message}", e)
            initialized = false
        }
    }

    /**
     * Veritabanının hazır olup olmadığını kontrol eder
     */
    fun isReady(): Boolean = initialized

    /**
     * Veritabanı tipini döner
     */
    fun getDbType(): String = "SQLite (Local)"

    /**
     * Veritabanı yolunu döner
     */
    fun getDbPath(): String = File(dbPath).absolutePath

    /**
     * Analiz sonucunu local veritabanına kaydeder
     */
    fun saveAnalysisResult(
        filename: String,
        deviceId: String? = null,
        location: String? = null,
        basicAnalysis: ImageAnalysisResult?,
        vehicleDetection: VehicleDetectionResult?,
        crowdAnalysis: CrowdAnalysisResult?,
        airQuality: AirQualityResult?,
        trafficAnalysis: TrafficAnalysisResult?,
        processingTimeMs: Long = 0
    ): Int? {
        if (!initialized) {
            logger.warn("Database not initialized, skipping save")
            return null
        }

        return try {
            val insertedId = transaction {
                AnalysisRecords.insert {
                    it[AnalysisRecords.timestamp] = Instant.now().toString()
                    it[AnalysisRecords.filename] = filename
                    it[AnalysisRecords.deviceId] = deviceId
                    it[AnalysisRecords.location] = location

                    // Temel analiz
                    it[imageWidth] = basicAnalysis?.width ?: 0
                    it[imageHeight] = basicAnalysis?.height ?: 0
                    it[avgBrightness] = basicAnalysis?.avgBrightness ?: 0.0

                    // Araç tespiti
                    it[totalVehicles] = vehicleDetection?.totalVehicles ?: 0
                    it[busCount] = vehicleDetection?.busCount ?: 0
                    it[carCount] = vehicleDetection?.carCount ?: 0
                    it[truckCount] = vehicleDetection?.truckCount ?: 0
                    it[motorcycleCount] = vehicleDetection?.motorcycleCount ?: 0
                    it[bicycleCount] = vehicleDetection?.bicycleCount ?: 0
                    it[vehicleDensity] = vehicleDetection?.vehicleDensity

                    // Kalabalık
                    it[estimatedPeople] = crowdAnalysis?.estimatedPeopleCount ?: 0
                    it[crowdDensityLevel] = crowdAnalysis?.densityLevel
                    it[crowdDensityPercentage] = crowdAnalysis?.densityPercentage ?: 0.0

                    // Hava kalitesi
                    it[hazeLevel] = airQuality?.hazeLevel ?: 0.0
                    it[visibilityScore] = airQuality?.visibilityScore ?: 0.0
                    it[aqiCategory] = airQuality?.estimatedAQI
                    it[aqiValue] = airQuality?.aqiValue ?: 0
                    it[smogDetected] = airQuality?.pollutionIndicators?.smogDetected ?: false
                    it[dustDetected] = airQuality?.pollutionIndicators?.dustDetected ?: false
                    it[fogDetected] = airQuality?.pollutionIndicators?.fogDetected ?: false

                    // Trafik
                    it[congestionLevel] = trafficAnalysis?.congestionLevel
                    it[congestionPercentage] = trafficAnalysis?.congestionPercentage ?: 0.0
                    it[roadOccupancy] = trafficAnalysis?.roadOccupancy ?: 0.0
                    it[incidentDetected] = trafficAnalysis?.incidentDetected ?: false

                    it[AnalysisRecords.processingTimeMs] = processingTimeMs
                } get AnalysisRecords.id
            }

            // Cache'i güncelle
            invalidateCache()

            logger.info("Analysis saved to local database with ID: $insertedId")
            insertedId
        } catch (e: Exception) {
            logger.error("Failed to save analysis: ${e.message}", e)
            null
        }
    }

    /**
     * Son N kaydı getirir
     */
    fun getRecentRecords(limit: Int = 100): List<AnalysisRecordDto> {
        if (!initialized) return emptyList()

        return try {
            transaction {
                AnalysisRecords.selectAll()
                    .orderBy(AnalysisRecords.id, SortOrder.DESC)
                    .limit(limit)
                    .map { row ->
                        AnalysisRecordDto(
                            id = row[AnalysisRecords.id],
                            timestamp = row[AnalysisRecords.timestamp],
                            filename = row[AnalysisRecords.filename],
                            deviceId = row[AnalysisRecords.deviceId],
                            location = row[AnalysisRecords.location],
                            totalVehicles = row[AnalysisRecords.totalVehicles],
                            busCount = row[AnalysisRecords.busCount],
                            carCount = row[AnalysisRecords.carCount],
                            estimatedPeople = row[AnalysisRecords.estimatedPeople],
                            crowdDensityLevel = row[AnalysisRecords.crowdDensityLevel],
                            aqiValue = row[AnalysisRecords.aqiValue],
                            aqiCategory = row[AnalysisRecords.aqiCategory],
                            congestionLevel = row[AnalysisRecords.congestionLevel],
                            congestionPercentage = row[AnalysisRecords.congestionPercentage]
                        )
                    }
            }
        } catch (e: Exception) {
            logger.error("Failed to get records: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * İstatistikleri hesaplar
     */
    fun getStatistics(hours: Int = 24): StatisticsDto {
        if (!initialized) return StatisticsDto()

        return try {
            transaction {
                val records = AnalysisRecords.selectAll().toList()

                if (records.isEmpty()) return@transaction StatisticsDto()

                StatisticsDto(
                    totalRecords = records.size,
                    avgVehicles = records.map { it[AnalysisRecords.totalVehicles] }.average(),
                    avgPeople = records.map { it[AnalysisRecords.estimatedPeople] }.average(),
                    avgAqi = records.map { it[AnalysisRecords.aqiValue] }.average(),
                    avgCongestion = records.map { it[AnalysisRecords.congestionPercentage] }.average(),
                    totalBuses = records.sumOf { it[AnalysisRecords.busCount] },
                    totalCars = records.sumOf { it[AnalysisRecords.carCount] },
                    incidentCount = records.count { it[AnalysisRecords.incidentDetected] }
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to get statistics: ${e.message}", e)
            StatisticsDto()
        }
    }

    /**
     * Tüm verileri siler (GDPR compliance - right to be forgotten)
     */
    fun clearAllData(): Boolean {
        if (!initialized) return false

        return try {
            transaction {
                AnalysisRecords.deleteAll()
            }
            invalidateCache()
            logger.info("All local data cleared (privacy request)")
            true
        } catch (e: Exception) {
            logger.error("Failed to clear data: ${e.message}", e)
            false
        }
    }

    /**
     * Cache'i invalidate eder
     */
    private fun invalidateCache() {
        recentRecordsCache.clear()
        cacheLastUpdated = Instant.now()
    }

    /**
     * Bağlantıyı kapatır
     */
    fun close() {
        initialized = false
        recentRecordsCache.clear()
        logger.info("Local database connection closed")
    }
}
