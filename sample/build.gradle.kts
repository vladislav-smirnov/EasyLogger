import io.github.airdaydreamers.easylogger.EasyLoggerGradleExtension
import io.github.airdaydreamers.easylogger.LogType

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    kotlin("jvm") // Let application plugin manage the version / ensure Kotlin nature
    id("io.github.airdaydreamers.easylogger.plugin") version "0.0.22"
    application
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.example.sample.MainKt")
}

// Configure the EasyLogger Gradle plugin
extensions.configure<EasyLoggerGradleExtension>("easyLogger") {
    enabled = true
    annotations = listOf("io.github.airdaydreamers.easylogger.annotation.DebugLog")
    logType = LogType.PRINTLN
}

dependencies {
    implementation(libs.kotlin.stdlib) // Use the locally defined ktVersion
    // No direct dependency on :kotlin-plugin, it's applied by the 'easyLogger plugin'
}
