import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JvmVendorSpec

buildscript {
    val ktVersion = "1.9.23"
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        mavenLocal()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$ktVersion")
    }
}

plugins {
    alias(libs.plugins.ksp) apply false
}

subprojects {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        mavenLocal()
    }

    apply(plugin = "maven-publish")
    version = property("pluginVersion") as String

    afterEvaluate {
        project.extensions.findByType(JavaPluginExtension::class.java)?.let { javaExtension ->
            javaExtension.toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
                vendor.set(JvmVendorSpec.AZUL)
            }
        }
    }
}
