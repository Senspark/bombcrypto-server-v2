import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask

plugins {
    java
    id("io.gitlab.arturbosch.detekt")
    kotlin("jvm")
    kotlin("plugin.serialization")
}

detekt {
    // preconfigure defaults
    buildUponDefaultConfig = true

    // activate all available (even unstable) rules.
    allRules = false

    // point to your custom config defining rules to run, overwriting default behavior
    config = files("$projectDir/detekt.yaml")

    // a way of suppressing issues before introducing detekt
    baseline = file("$projectDir/config/baseline.xml")
}

tasks.withType<Detekt>().configureEach {
    reports {
        // observe findings in your browser with structure and code snippets
        html.required.set(true)

        // checkstyle like format mainly for integrations like Jenkins
        xml.required.set(true)

        // similar to the console output, contains issue signature to manually edit baseline files
        txt.required.set(true)

        // standardized SARIF format (https://sarifweb.azurewebsites.net/) to support integrations with Github Code
        // Scanning
        sarif.required.set(true)

        // simple Markdown format
        md.required.set(true)
    }
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = "11"
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    jvmTarget = "11"
}

sourceSets {
    main {
        java.srcDir("src")
    }
    test {
        java.srcDir("src_test")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named("build") {
    dependsOn("copyDependencies")
}

tasks.register<Copy>("copyDependencies") {
    println("Copying dependencies...")
    from(configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") })
    into("${layout.buildDirectory.get()}/dependencies")
}

var koinVersion = "4.0.0-RC1"

dependencies {
    compileOnly(project(":SmartFoxLibs"))
    compileOnly(project(":Common"))
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("com.google.code.gson:gson:2.10") // FIXME: remove.
    implementation("com.slack.api:slack-api-client:1.39.0")
    testImplementation(kotlin("test"))
    testImplementation(project(":SmartFoxLibs"))
    testImplementation(project(":Common"))
    testImplementation("com.h2database:h2:2.1.214")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
    testImplementation("org.apache.velocity:velocity-engine-core:2.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    implementation(platform("io.insert-koin:koin-bom:$koinVersion"))
    implementation("io.insert-koin:koin-core")
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    testImplementation("io.insert-koin:koin-test-junit5:$koinVersion")

//    testImplementation(project(":simple-rest-api")) // for open a simple REST API to write Test cases
}
tasks.withType<Test> {
    maxHeapSize = "2g"
}
