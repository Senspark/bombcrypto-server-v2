plugins {
    java
    kotlin("jvm")
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

dependencies {
    // SmartFox 2.19.0 dependencies
    api(files("libs/sfs2x-core.jar"))
    api(files("libs/sfs2x.jar"))
    // Fix api("net.sf.json-lib:json-lib:2.2.3") not found
    api(files("libs/json-lib-2.2.3-jdk15.jar"))
    api("commons-beanutils:commons-beanutils:1.9.4")
    api("commons-codec:commons-codec:1.15")
    api("org.apache.commons:commons-collections4:4.4")
    api("commons-dbcp:commons-dbcp:1.4")
    api("commons-io:commons-io:1.4")
    api("commons-lang:commons-lang:2.4")
    api("commons-logging:commons-logging:1.2")
    api("commons-pool:commons-pool:1.5.4")
    api("commons-validator:commons-validator:1.5.0")
    api("commons-vfs:commons-vfs:1.0")
    api("com.fasterxml.jackson.core:jackson-annotations:2.8.0")
    api("com.fasterxml.jackson.core:jackson-core:2.9.9")
    api("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    api("com.google.guava:guava:33.0.0-jre")
    api("com.maxmind.db:maxmind-db:1.2.1")
    api("com.maxmind.geoip2:geoip2:2.8.0")
    api("com.sun.activation:javax.activation:1.2.0")
    api("com.thoughtworks.xstream:xstream:1.4.20")
    api("joda-time:joda-time:2.2")
    api("net.sf.ezmorph:ezmorph:1.0.6")
    api("org.antlr:stringtemplate:3.2.1")
    api("org.apache.commons:commons-compress:1.26.0")
    api("org.apache.httpcomponents:httpclient:4.5.14")
    api("org.apache.httpcomponents:httpcore:4.4.4")
    api("org.scala-lang:scala-library:2.11.6")
    api("org.slf4j:slf4j-api:2.0.12")
    api("org.slf4j:slf4j-log4j12:2.0.12")
    api("xpp3:xpp3_min:1.1.4c")
}