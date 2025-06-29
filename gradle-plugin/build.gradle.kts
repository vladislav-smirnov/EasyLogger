plugins {
    `java-gradle-plugin`
    kotlin("jvm") // Let java-gradle-plugin manage the version
    alias(libs.plugins.ksp)
    `maven-publish`
}

group = "dio.github.airdaydreamers.easylogger.plugin"
version = project.property("pluginVersion") as String

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    plugins {
        create("simplePlugin") {
            id = "io.github.airdaydreamers.easylogger.plugin"
            implementationClass = "io.github.airdaydreamers.easylogger.EasyLoggerGradlePlugin"
        }
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.gradlePluginApi)

    compileOnly(libs.autoService.annotations)
    ksp(libs.autoService.ksp)

    implementation(project(":kotlin-plugin"))
}

val generatedKotlinDir = layout.buildDirectory.dir("generated/kotlin/main")

kotlin.sourceSets.main {
    kotlin.srcDir(generatedKotlinDir) // The generated file will have its package structure
}

tasks.register("generatePluginVersionKt") {
    description = "Generates a Kotlin file with the plugin version."
    outputs.file(generatedKotlinDir.map { it.file("io/github/airdaydreamers/easylogger/plugin/internal/PluginVersion.kt") })

    doLast {
        val versionFile = outputs.files.singleFile
        versionFile.parentFile.mkdirs()
        versionFile.writeText("""
            package io.github.airdaydreamers.easylogger.plugin.internal

            internal object PluginVersion {
                const val VERSION = "${project.version}"
            }
        """.trimIndent())
        println("Generated ${versionFile.absolutePath} with version ${project.version}")
    }
}

tasks.named("compileKotlin") {
    dependsOn("generatePluginVersionKt")
}

tasks.matching { it.name.startsWith("ksp") && it.name.endsWith("Kotlin") }.configureEach {
    dependsOn("generatePluginVersionKt")
}

publishing {
    repositories {
        mavenLocal()
    }
}
