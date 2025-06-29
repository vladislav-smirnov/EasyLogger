import org.gradle.api.publish.maven.MavenPublication

plugins {
    kotlin("jvm") // Let java-library manage the version
    alias(libs.plugins.ksp)
    `java-library`
    `maven-publish`
}

group = "io.github.airdaydreamers.easylogger.plugin"
version = project.property("pluginVersion") as String

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(kotlin("gradle-plugin-api"))

    implementation(libs.kotlin.stdlib)
    compileOnly(libs.kotlin.compilerEmbeddable)

    compileOnly(libs.autoService.annotationsLegacy)
    ksp(libs.autoService.ksp)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
    repositories {
        mavenLocal()
    }
}
