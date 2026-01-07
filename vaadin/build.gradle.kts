plugins {
    kotlin("jvm")
    id("com.vaadin") version "24.1.12" apply false
    application
}

dependencies {
    implementation(project(":common"))
    implementation("com.vaadin:vaadin-core:24.1.12")
}

application {
    mainClass.set("com.example.vaadin.VaadinApplicationKt")
}

