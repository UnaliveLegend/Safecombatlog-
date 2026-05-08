plugins {
    java
    id("com.gradleup.shadow") version "8.3.0"
}

group = "dev.safecombatlog"
version = "1.0.0"
description = "Lightweight PvP combat logging plugin for Paper 1.21"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("SafeCombatLog-${version}.jar")
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
