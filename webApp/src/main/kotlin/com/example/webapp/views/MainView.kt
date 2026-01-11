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
@PageTitle("FUI - Image Analysis")
class MainView : VerticalLayout() {

    private val analysisService = CameraAnalysisService()

    private val imageContainer = Div()
    private val fileInfoContainer = Div()  // Custom file info with remove button
    private val vehicleCount = Span("0")
    private val peopleCount = Span("0")
    private val trafficLevel = Span("Waiting...")
    private val crowdLevel = Span("Waiting...")
    private val statusLabel = Span("📸 Upload an image")
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

        val subtitle = Paragraph("Traffic and Crowd Analysis System")
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

        val leftPanel = createLeftPanel()
        leftPanel.style.set("flex", "1")
        leftPanel.style.set("min-width", "350px")
        leftPanel.style.set("max-width", "500px")

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

        imageContainer.setWidthFull()
        imageContainer.style.set("height", "250px")
        imageContainer.style.set("background", "#1a1a2e")
        imageContainer.style.set("border-radius", "8px")
        imageContainer.style.set("display", "flex")
        imageContainer.style.set("align-items", "center")
        imageContainer.style.set("justify-content", "center")
        imageContainer.style.set("overflow", "hidden")

        val placeholder = Span("📷 Upload an image")
        placeholder.style.set("color", "#666666")
        placeholder.style.set("font-size", "18px")
        imageContainer.add(placeholder)

        val buffer = MemoryBuffer()
        val upload = Upload(buffer)
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/gif", "image/webp")
        upload.maxFiles = 1
        upload.setWidthFull()
        upload.style.set("margin-top", "15px")

        // Hide the default file list - we'll show our own
        upload.element.executeJs("""
            this.style.setProperty('--vaadin-upload-file-list-display', 'none');
            const style = document.createElement('style');
            style.textContent = 'vaadin-upload-file { display: none !important; }';
            document.head.appendChild(style);
        """)

        // Custom file info container (hidden initially)
        fileInfoContainer.setWidthFull()
        fileInfoContainer.style.set("display", "none")
        fileInfoContainer.style.set("margin-top", "10px")

        upload.addSucceededListener { event ->
            try {
                val inputStream = buffer.inputStream
                uploadedImageBytes = inputStream.readAllBytes()
                displayUploadedImage()
                showFileInfo(event.fileName)
                statusLabel.text = "✅ Image loaded: ${event.fileName}"
                performAnalysis()
            } catch (e: Exception) {
                Notification.show("Upload error: ${e.message}", 3000, Notification.Position.MIDDLE)
            }
        }

        upload.addFailedListener {
            Notification.show("Upload failed!", 3000, Notification.Position.MIDDLE)
        }

        val buttonRow = HorizontalLayout()
        buttonRow.setWidthFull()
        buttonRow.justifyContentMode = FlexComponent.JustifyContentMode.CENTER
        buttonRow.style.set("gap", "10px")
        buttonRow.style.set("margin-top", "15px")

        val analyzeButton = Button("🔍 ANALYZE") { performAnalysis() }
        analyzeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY)
        analyzeButton.style.set("background", "#4CAF50")
        analyzeButton.style.set("color", "white")

        val clearButton = Button("🗑️ Clear") { clearAll() }
        clearButton.style.set("background", "#f44336")
        clearButton.style.set("color", "white")

        buttonRow.add(analyzeButton, clearButton)

        statusLabel.style.set("color", "#888888")
        statusLabel.style.set("text-align", "center")
        statusLabel.style.set("display", "block")
        statusLabel.style.set("margin-top", "10px")

        panel.add(
            H3("📸 Upload Image").apply { style.set("color", "#ffffff"); style.set("margin", "0 0 15px 0") },
            imageContainer,
            upload,
            fileInfoContainer,
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

        val statsRow1 = HorizontalLayout()
        statsRow1.setWidthFull()
        statsRow1.style.set("gap", "10px")

        val vehicleCard = createStatCard("🚗 VEHICLES", vehicleCount, "#4CAF50")
        val peopleCard = createStatCard("👥 PEOPLE", peopleCount, "#2196F3")

        statsRow1.add(vehicleCard, peopleCard)

        val statsRow2 = HorizontalLayout()
        statsRow2.setWidthFull()
        statsRow2.style.set("gap", "10px")
        statsRow2.style.set("margin-top", "10px")

        val trafficCard = createStatCard("🚦 TRAFFIC", trafficLevel, "#FF9800")
        val crowdCard = createStatCard("👥 CROWD", crowdLevel, "#9C27B0")

        statsRow2.add(trafficCard, crowdCard)

        val reportTitle = H3("📊 Detailed Report")
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
        resultArea.text = "Analysis results will appear here..."

        panel.add(
            H3("📈 Analysis Results").apply { style.set("color", "#ffffff"); style.set("margin", "0 0 15px 0") },
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

        footer.text = "🔒 Data processed on device, not sent to server | KotlinConf 2026 Project"
        return footer
    }

    private fun displayUploadedImage() {
        imageContainer.removeAll()

        uploadedImageBytes?.let { bytes ->
            // Create a wrapper for image and remove button
            val wrapper = Div()
            wrapper.style.set("position", "relative")
            wrapper.style.set("width", "100%")
            wrapper.style.set("height", "100%")
            wrapper.style.set("display", "flex")
            wrapper.style.set("align-items", "center")
            wrapper.style.set("justify-content", "center")

            val resource = StreamResource("uploaded-image.jpg") { ByteArrayInputStream(bytes) }
            val image = Image(resource, "Uploaded image")
            image.style.set("max-width", "100%")
            image.style.set("max-height", "100%")
            image.style.set("object-fit", "contain")

            // Add a prominent remove button on top right corner
            val removeBtn = Button("✕") { clearAll() }
            removeBtn.style.set("position", "absolute")
            removeBtn.style.set("top", "10px")
            removeBtn.style.set("right", "10px")
            removeBtn.style.set("width", "48px")
            removeBtn.style.set("height", "48px")
            removeBtn.style.set("min-width", "48px")
            removeBtn.style.set("padding", "0")
            removeBtn.style.set("background", "#f44336")
            removeBtn.style.set("color", "white")
            removeBtn.style.set("border", "3px solid white")
            removeBtn.style.set("border-radius", "50%")
            removeBtn.style.set("font-size", "28px")
            removeBtn.style.set("font-weight", "bold")
            removeBtn.style.set("cursor", "pointer")
            removeBtn.style.set("box-shadow", "0 4px 12px rgba(0,0,0,0.5)")
            removeBtn.style.set("z-index", "10")
            removeBtn.style.set("line-height", "1")
            removeBtn.style.set("transition", "transform 0.2s, box-shadow 0.2s")
            removeBtn.element.setAttribute("title", "Remove image")

            wrapper.add(image, removeBtn)
            imageContainer.add(wrapper)
        }
    }

    /**
     * Shows custom file info with prominent remove button
     */
    private fun showFileInfo(fileName: String) {
        fileInfoContainer.removeAll()
        fileInfoContainer.style.set("display", "flex")
        fileInfoContainer.style.set("align-items", "center")
        fileInfoContainer.style.set("justify-content", "space-between")
        fileInfoContainer.style.set("background", "#1a1a2e")
        fileInfoContainer.style.set("padding", "12px 16px")
        fileInfoContainer.style.set("border-radius", "8px")

        // File name with checkmark
        val fileNameSpan = Span("✓ $fileName")
        fileNameSpan.style.set("color", "#4CAF50")
        fileNameSpan.style.set("font-weight", "bold")
        fileNameSpan.style.set("font-size", "14px")

        // Big red remove button with more prominence
        val removeButton = Button("✕ Remove") { clearAll() }
        removeButton.style.set("background", "#f44336")
        removeButton.style.set("color", "white")
        removeButton.style.set("border", "2px solid #ff6659")
        removeButton.style.set("border-radius", "8px")
        removeButton.style.set("padding", "10px 20px")
        removeButton.style.set("font-size", "16px")
        removeButton.style.set("font-weight", "bold")
        removeButton.style.set("cursor", "pointer")
        removeButton.style.set("min-width", "100px")

        fileInfoContainer.add(fileNameSpan, removeButton)
    }

    private fun hideFileInfo() {
        fileInfoContainer.style.set("display", "none")
        fileInfoContainer.removeAll()
    }

    private fun performAnalysis() {
        val bytes = uploadedImageBytes
        if (bytes == null) {
            Notification.show("Please upload an image first!", 3000, Notification.Position.MIDDLE)
            return
        }

        statusLabel.text = "🔄 Analyzing..."

        try {
            val inputStream = ByteArrayInputStream(bytes)
            val bufferedImage = ImageIO.read(inputStream)

            if (bufferedImage == null) {
                statusLabel.text = "❌ Could not read image"
                return
            }

            val result = analysisService.analyzeImage(bufferedImage)

            vehicleCount.text = result.vehicleCount.toString()
            peopleCount.text = result.estimatedPeople.toString()
            trafficLevel.text = result.trafficLevel
            crowdLevel.text = result.crowdLevel

            updateLevelColor(trafficLevel, result.trafficLevel)
            updateLevelColor(crowdLevel, result.crowdLevel)

            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"))
            resultArea.text = """
📊 ANALYSIS REPORT
$timestamp | ${bufferedImage.width}x${bufferedImage.height}

🚗 VEHICLE ANALYSIS
   Detected: ${result.vehicleCount} vehicles
   Traffic: ${result.trafficLevel}

👥 CROWD ANALYSIS
   Estimated: ~${result.estimatedPeople} people
   Crowd: ${result.crowdLevel}

🌤️ ENVIRONMENT
   Time: ${result.timeOfDay}
   Brightness: ${result.brightness}
   Air Quality: ${result.airQuality}

✅ Analyzed on device
🔒 No data sent to server
            """.trimIndent()

            statusLabel.text = "✅ Analysis completed - $timestamp"

        } catch (e: Exception) {
            statusLabel.text = "❌ Analysis error: ${e.message}"
            resultArea.text = "Error: ${e.message}"
        }
    }

    private fun updateLevelColor(span: Span, level: String) {
        val color = when {
            level.contains("INDOOR") || level.contains("🏛️") -> "#9C27B0"
            level.contains("VERY HIGH") || level.contains("🔴") -> "#F44336"
            level.contains("HIGH") || level.contains("🟠") -> "#FF9800"
            level.contains("MEDIUM") || level.contains("🟡") -> "#FFEB3B"
            level.contains("LOW") || level.contains("🟢") -> "#4CAF50"
            else -> "#FFFFFF"
        }
        span.style.set("color", color)
    }

    private fun clearAll() {
        uploadedImageBytes = null
        imageContainer.removeAll()
        hideFileInfo()

        val placeholder = Span("📷 Upload an image")
        placeholder.style.set("color", "#666666")
        placeholder.style.set("font-size", "18px")
        imageContainer.add(placeholder)

        vehicleCount.text = "0"
        peopleCount.text = "0"
        trafficLevel.text = "Waiting..."
        crowdLevel.text = "Waiting..."
        resultArea.text = "Analysis results will appear here..."
        statusLabel.text = "📸 Upload an image"

        Notification.show("Cleared", 2000, Notification.Position.BOTTOM_CENTER)
    }
}

