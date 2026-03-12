plugins {
    id("io.gitlab.arturbosch.detekt") version "1.22.0-RC2" apply false
    kotlin("jvm") version "2.0.0-RC1" apply false
    kotlin("plugin.serialization") version "2.0.0-RC1" apply false
    id("com.github.ben-manes.versions") version "0.51.0" // For dependencyUpdates gradle task (./gradlew dependencyUpdates)
}

allprojects {
    repositories {
        mavenCentral()
    }
}