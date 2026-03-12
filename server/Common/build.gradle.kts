plugins {
    java
    kotlin("jvm")
    kotlin("plugin.serialization")
}

sourceSets {
    main {
        java.srcDir("src")
    }
    test {
        java.srcDir("test")
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

tasks.test {
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

val exposedVersion = "0.49.0"

dependencies {
    compileOnly(project(":SmartFoxLibs"))
    
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    
    // __lib__ deps.
    api("com.squareup.okhttp3:okhttp:4.12.0")
    api("com.zaxxer:HikariCP:5.1.0")
    api("org.apache.commons:commons-lang3:3.12.0")
    api("org.jetbrains.exposed:exposed-core:$exposedVersion")
    api("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    api("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    api("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    api("org.postgresql:postgresql:42.7.4")
    api("io.lettuce:lettuce-core:6.5.3.RELEASE")
    api("org.fluentd:fluent-logger:0.3.4")
}