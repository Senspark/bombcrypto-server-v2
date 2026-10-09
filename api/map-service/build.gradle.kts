import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    application
    id("com.gradleup.shadow") version "8.3.5"
}

group = "com.senspark"
version = "1.0.0"

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.12"

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    // Same Redis client (and version) bombcrypto-server-v2's Common module uses for its streams --
    // MapService publishes each bomb's explode result to the Redis stream that server listens on.
    implementation("io.lettuce:lettuce-core:6.5.3.RELEASE")

    // Client side only, used by the com.senspark.mapservice.stresstest.* load-test tool below.
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    // Java engine (java.net.http) pools keep-alive connections; Ktor 2's CIO engine opens a fresh
    // TCP connection per request unless pipelining is on, which exhausts local ephemeral ports.
    implementation("io.ktor:ktor-client-java:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

application {
    mainClass.set("com.senspark.mapservice.MainKt")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    archiveBaseName.set("mapservice")
    archiveClassifier.set("")
    archiveVersion.set("")
    mergeServiceFiles()
}

// Load-test client for a running MapService instance -- see README § Stress testing.
// Usage: ./gradlew stressTest -Pargs="--users 10 --duration 60"
tasks.register<JavaExec>("stressTest") {
    group = "verification"
    description = "Runs the MapService stress-test client against a running instance (-Pargs=\"--users 10\")."
    mainClass.set("com.senspark.mapservice.stresstest.StressTestMainKt")
    classpath = sourceSets["main"].runtimeClasspath
    args = (project.findProperty("args") as String?)?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
}
