plugins {
    //trick: for the same plugin versions in all sub-modules
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
    kotlin("multiplatform") version "2.0.0" apply false
    kotlin("jvm") version "2.0.0" apply false
    kotlin("plugin.serialization") version "2.0.0" apply false
    kotlin("plugin.spring") version "2.0.0" apply false
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
