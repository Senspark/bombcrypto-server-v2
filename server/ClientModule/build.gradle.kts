plugins {
    java
    kotlin("jvm")
    kotlin("plugin.serialization")
}

group = "com.senspark"
version = "unspecified"
var koinVersion = "4.0.0-RC1"

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

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1-Beta")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation(project(":BombChainExtension"))
    implementation(project(":Common"))

    
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1-Beta")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation(files("libs/commons-lang-2.4.jar"))
    implementation(files("libs/jdom.jar"))
    implementation(files("libs/netty-3.2.2.Final.jar"))
    implementation(files("libs/sfs2x-client-core.jar"))
    implementation(files("libs/SFS2X_API_Java.jar"))
    implementation(files("libs/slf4j-api-1.6.1.jar"))
    implementation(files("libs/slf4j-simple-1.6.1.jar"))
    implementation(files("libs/json-lib-2.2.3-jdk15.jar"))
    implementation(files("libs/sfs2x.jar"))
    implementation(files("libs/sfs2x-core.jar"))
    implementation("net.sf.ezmorph:ezmorph:1.0.6")
    implementation("commons-logging:commons-logging:1.2")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("commons-beanutils:commons-beanutils:1.9.4")
    
    implementation(platform("io.insert-koin:koin-bom:$koinVersion"))
    implementation("io.insert-koin:koin-core")
    testImplementation("io.insert-koin:koin-test:$koinVersion")
    testImplementation("io.insert-koin:koin-test-junit5:$koinVersion")

    testImplementation("org.bouncycastle:bcprov-jdk15on:1.70")
    testImplementation("org.bouncycastle:bcpkix-jdk15on:1.70")

    implementation("org.fluentd:fluent-logger:0.3.4")
}

tasks.test {
    useJUnitPlatform()
}