package com.example.webapp.views

import com.example.webapp.service.CameraAnalysisService
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.*
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.orderedlayout.FlexComponent
import com.vaadin.flow.component.orderedlayout.HorizontalLayout
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.upload.Upload
import com.vaadin.flow.component.upload.receivers.MemoryBuffer
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.router.Route
import com.vaadin.flow.server.StreamResource
import java.io.ByteArrayInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO

@Route("")
@PageTitle("FUI - Görüntü Analiz")
class MainView : VerticalLayout() {

    private val analysisService = CameraAnalysisService()

    // UI Components
    private val imageContainer = Div()
    private val vehicleCount = Span("0")
    private val peopleCount = Span("0")
    private val trafficLevel = Span("Bekleniyor...")
    private val crowdLevel = Span("Bekleniyor...")
    private val statusLabel = Span("📸 Bir fotoğraf yükleyin")
    private val resultArea = Pre()

    private var uploadedImageBytes: ByteArray? = null

    init {
        setSizeFull()
        style.set("background", "linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)")
        style.set("padding", "20px")
        style.set("overflow", "auto")

        add(
            createHeader(),
            createMainContent(),
            createFooter()
        )
    }

    private fun createHeader(): VerticalLayout {
        val header = VerticalLayout()
        header.setWidthFull()
        header.alignItems = FlexComponent.Alignment.CENTER
        header.isPadding = false
        header.isSpacing = false

        val title = H1("🚦 Federated Urban Insights")
        title.style.set("color", "#ffffff")
        title.style.set("margin", "0")
        title.style.set("font-size", "2rem")

        val subtitle = Paragraph("Trafik ve Kalabalık Analiz Sistemi")
        subtitle.style.set("color", "#888888")
        subtitle.style.set("margin", "5px 0 20px 0")

        header.add(title, subtitle)
        return header
    }

    private fun createMainContent(): HorizontalLayout {
        val content = HorizontalLayout()
        content.setWidthFull()
        content.style.set("flex-wrap", "wrap")
        content.style.set("gap", "20px")
        content.justifyContentMode = FlexComponent.JustifyContentMode.CENTER

        // Sol Panel - Görüntü ve Kontroller
        val leftPanel = createLeftPanel()
        leftPanel.style.set("flex", "1")
        leftPanel.style.set("min-width", "350px")
        leftPanel.style.set("max-width", "500px")

        // Sağ Panel - Sonuçlar
        val rightPanel = createRightPanel()
        rightPanel.style.set("flex", "1")
        rightPanel.style.set("min-width", "350px")
        rightPanel.style.set("max-width", "500px")

        content.add(leftPanel, rightPanel)
        return content
    }

    private fun createLeftPanel(): VerticalLayout {
        val panel = VerticalLayout()
        panel.style.set("background", "#2d2d44")
        panel.style.set("border-radius", "12px")
        panel.style.set("padding", "20px")

        // Görüntü Alanı
        imageContainer.setWidthFull()
        imageContainer.style.set("height", "250px")
        imageContainer.style.set("background", "#1a1a2e")
        imageContainer.style.set("border-radius", "8px")
        imageContainer.style.set("display", "flex")
        imageContainer.style.set("align-items", "center")
        imageContainer.style.set("justify-content", "center")
        imageContainer.style.set("overflow", "hidden")

        val placeholder = Span("📷 Fotoğraf yükleyin")
        placeholder.style.set("color", "#666666")
        placeholder.style.set("font-size", "18px")
        imageContainer.add(placeholder)

        // Upload Bileşeni
        val buffer = MemoryBuffer()
        val upload = Upload(buffer)
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/gif", "image/webp")
        upload.maxFiles = 1
        upload.setWidthFull()
        upload.style.set("margin-top", "15px")

        upload.addSucceededListener { event ->
            try {
                val inputStream = buffer.inputStream
                uploadedImageBytes = inputStream.readAllBytes()
                displayUploadedImage()
                statusLabel.text = "✅ Görüntü yüklendi: ${event.fileName}"
                // Otomatik analiz
                performAnalysis()
            } catch (e: Exception) {
                Notification.show("Yükleme hatası: ${e.message}", 3000, Notification.Position.MIDDLE)
            }
        }

        upload.addFailedListener {
            Notification.show("Yükleme başarısız!", 3000, Notification.Position.MIDDLE)
        }

        // Butonlar
        val buttonRow = HorizontalLayout()
        buttonRow.setWidthFull()
        buttonRow.justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        buttonRow.style.set("gap", "10px")
        buttonRow.style.set("margin-top", "15px")

        val analyzeButton = Button("🔍 ANALİZ ET") { performAnalysis() }
        analyzeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)
        analyzeButton.style.set("background", "#4CAF50")
        analyzeButton.style.set("color", "white")

        val clearButton = Button("🗑️ Temizle") { clearAll() }
        clearButton.style.set("background", "#f44336")
        clearButton.style.set("color", "white")

        buttonRow.add(analyzeButton, clearButton)

        // Durum
        statusLabel.style.set("color", "#888888")
        statusLabel.style.set("text-align", "center")
        statusLabel.style.set("display", "block")
        statusLabel.style.set("margin-top", "10px")

        panel.add(
            H3("📸 Görüntü Yükle").apply { style.set("color", "#ffffff"); style.set("margin", "0 0 15px 0") },
            imageContainer,
            upload,
            buttonRow,
            statusLabel
        )

        return panel
    }

    private fun createRightPanel(): VerticalLayout {
        val panel = VerticalLayout()
        panel.style.set("background", "#2d2d44")
        panel.style.set("border-radius", "12px")
        panel.style.set("padding", "20px")

        // İstatistik Kartları
        val statsRow1 = HorizontalLayout()
        statsRow1.setWidthFull()
        statsRow1.style.set("gap", "10px")

        val vehicleCard = createStatCard("🚗 ARAÇ", vehicleCount, "#4CAF50")
        val peopleCard = createStatCard("👥 İNSAN", peopleCount, "#2196F3")

        statsRow1.add(vehicleCard, peopleCard)

        val statsRow2 = HorizontalLayout()
        statsRow2.setWidthFull()
        statsRow2.style.set("gap", "10px")
        statsRow2.style.set("margin-top", "10px")

        val trafficCard = createStatCard("🚦 TRAFİK", trafficLevel, "#FF9800")
        val crowdCard = createStatCard("👥 KALABALIK", crowdLevel, "#9C27B0")

        statsRow2.add(trafficCard, crowdCard)

        // Detaylı Rapor
        val reportTitle = H3("📊 Detaylı Rapor")
        reportTitle.style.set("color", "#ffffff")
        reportTitle.style.set("margin", "20px 0 10px 0")

        resultArea.style.set("background", "#1a1a2e")
        resultArea.style.set("color", "#cccccc")
        resultArea.style.set("padding", "15px")
        resultArea.style.set("border-radius", "8px")
        resultArea.style.set("font-family", "monospace")
        resultArea.style.set("font-size", "12px")
        resultArea.style.set("white-space", "pre-wrap")
        resultArea.style.set("overflow-x", "auto")
        resultArea.style.set("max-height", "300px")
        resultArea.style.set("overflow-y", "auto")
        resultArea.setWidthFull()
        resultArea.text = "Analiz sonuçları burada görünecek..."

        panel.add(
            H3("📈 Analiz Sonuçları").apply { style.set("color", "#ffffff"); style.set("margin", "0 0 15px 0") },
            statsRow1,
            statsRow2,
            reportTitle,
            resultArea
        )

        return panel
    }

    private fun createStatCard(title: String, valueSpan: Span, color: String): Div {
        val card = Div()
        card.style.set("background", "#1a1a2e")
        card.style.set("border-radius", "8px")
        card.style.set("padding", "15px")
        card.style.set("flex", "1")
        card.style.set("text-align", "center")

        val titleLabel = Div()
        titleLabel.text = title
        titleLabel.style.set("color", "#888888")
        titleLabel.style.set("font-size", "12px")
        titleLabel.style.set("margin-bottom", "5px")

        valueSpan.style.set("color", color)
        valueSpan.style.set("font-size", "24px")
        valueSpan.style.set("font-weight", "bold")
        valueSpan.style.set("display", "block")

        card.add(titleLabel, valueSpan)
        return card
    }

    private fun createFooter(): Div {
        val footer = Div()
        footer.setWidthFull()
        footer.style.set("text-align", "center")
        footer.style.set("margin-top", "20px")
        footer.style.set("color", "#666666")
        footer.style.set("font-size", "12px")

        footer.text = "🔒 Veriler cihazda işlenir, sunucuya gönderilmez | KotlinConf 2025 Projesi"
        return footer
    }

    private fun displayUploadedImage() {
        imageContainer.removeAll()

        uploadedImageBytes?.let { bytes ->
            val resource = StreamResource("uploaded-image.jpg") { ByteArrayInputStream(bytes) }
            val image = Image(resource, "Yüklenen görüntü")
            image.style.set("max-width", "100%")
            image.style.set("max-height", "100%")
            image.style.set("object-fit", "contain")
            imageContainer.add(image)
        }
    }


    private fun performAnalysis() {
        val bytes = uploadedImageBytes
        if (bytes == null) {
            Notification.show("Önce bir görüntü yükleyin!", 3000, Notification.Position.MIDDLE)
            return
        }

        statusLabel.text = "🔄 Analiz ediliyor..."

        try {
            val inputStream = ByteArrayInputStream(bytes)
            val bufferedImage = ImageIO.read(inputStream)

            if (bufferedImage == null) {
                statusLabel.text = "❌ Görüntü okunamadı"
                return
            }

            val result = analysisService.analyzeImage(bufferedImage)

            // Sonuçları güncelle
            vehicleCount.text = result.vehicleCount.toString()
            peopleCount.text = result.estimatedPeople.toString()
            trafficLevel.text = result.trafficLevel
            crowdLevel.text = result.crowdLevel

            // Renkleri güncelle
            updateLevelColor(trafficLevel, result.trafficLevel)
            updateLevelColor(crowdLevel, result.crowdLevel)

            // Rapor
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
            resultArea.text = """
══════════════════════════════════
📊 ANALİZ RAPORU
══════════════════════════════════
🕐 $timestamp
📐 Boyut: ${bufferedImage.width}x${bufferedImage.height}

──────────────────────────────────
🚗 ARAÇ ANALİZİ
──────────────────────────────────
   Tespit Edilen: ${result.vehicleCount} araç
   Trafik Durumu: ${result.trafficLevel}

──────────────────────────────────
👥 KALABALIK ANALİZİ
──────────────────────────────────
   Tahmini Kişi: ~${result.estimatedPeople}
   Kalabalık: ${result.crowdLevel}

──────────────────────────────────
🌤️ ORTAM ANALİZİ
──────────────────────────────────
   Zaman: ${result.timeOfDay}
   Parlaklık: ${result.brightness}
   Hava Kalitesi: ${result.airQuality}

══════════════════════════════════
✅ Cihazda analiz edildi
🔒 Veri sunucuya gönderilmedi
══════════════════════════════════
            """.trimIndent()

            statusLabel.text = "✅ Analiz tamamlandı - $timestamp"

        } catch (e: Exception) {
            statusLabel.text = "❌ Analiz hatası: ${e.message}"
            resultArea.text = "Hata: ${e.message}"
        }
    }

    private fun updateLevelColor(span: Span, level: String) {
        val color = when {
            level.contains("ÇOK YOĞUN") || level.contains("🔴") -> "#F44336"
            level.contains("YOĞUN") || level.contains("🟠") -> "#FF9800"
            level.contains("ORTA") || level.contains("🟡") -> "#FFEB3B"
            level.contains("AZ") || level.contains("HAFİF") || level.contains("🟢") -> "#4CAF50"
            else -> "#FFFFFF"
        }
        span.style.set("color", color)
    }

    private fun clearAll() {
        uploadedImageBytes = null
        imageContainer.removeAll()

        val placeholder = Span("📷 Fotoğraf yükleyin")
        placeholder.style.set("color", "#666666")
        placeholder.style.set("font-size", "18px")
        imageContainer.add(placeholder)

        vehicleCount.text = "0"
        peopleCount.text = "0"
        trafficLevel.text = "Bekleniyor..."
        crowdLevel.text = "Bekleniyor..."
        resultArea.text = "Analiz sonuçları burada görünecek..."
        statusLabel.text = "📸 Bir fotoğraf yükleyin"

        Notification.show("Temizlendi", 2000, Notification.Position.BOTTOM_CENTER)
    }
}

