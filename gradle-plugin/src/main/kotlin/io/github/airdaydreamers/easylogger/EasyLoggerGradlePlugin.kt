package io.github.airdaydreamers.easylogger

import io.github.airdaydreamers.easylogger.plugin.internal.PluginVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class EasyLoggerGradlePlugin : Plugin<Project>, KotlinCompilerPluginSupportPlugin {

  override fun apply(project: Project) {
    project.extensions.create(
        "easyLogger",
        EasyLoggerGradleExtension::class.java
    )
  }

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

  override fun applyToCompilation(
      kotlinCompilation: KotlinCompilation<*>
  ): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.target.project
    val extension = project.extensions.findByType(EasyLoggerGradleExtension::class.java)
        ?: EasyLoggerGradleExtension()

    // Default to annotation if not specified by the user,
    // ensuring the sample works out of the box if they defined the annotation.
    val effectiveAnnotations = if (extension.annotations.isEmpty() && project.name == "sample") {
        // Special default for the sample project, assuming it defines annotation
        listOf("io.github.airdaydreamers.easylogger.annotation.DebugLog")
    } else {
        extension.annotations
    }

    // The SubpluginArtifact returned by getPluginArtifact() handles adding the
    // kotlin-plugin to the compiler plugin classpath.
    // Adding it explicitly here via project.dependencies.add(...) was causing issues.

    return project.provider {
      val options = mutableListOf<SubpluginOption>()
      options.add(SubpluginOption(key = "enabled", value = extension.enabled.toString()))
      options.add(SubpluginOption(key = "logType", value = extension.logType.toString()))
      // Pass each annotation as a separate option, using the key "logAnnotation"
      effectiveAnnotations.forEach { annotation ->
        options.add(SubpluginOption(key = "logAnnotation", value = annotation))
      }
      options
    }
  }

  override fun getCompilerPluginId(): String = "io.github.airdaydreamers.easylogger.plugin" // Matches EasyLoggerCommandLineProcessor

  override fun getPluginArtifact(): SubpluginArtifact {
    return SubpluginArtifact(
        groupId = "io.github.airdaydreamers.easylogger.plugin", // Matches the publishing group of kotlin-plugin
        artifactId = "kotlin-plugin", // Matches the artifactId of kotlin-plugin
        version = PluginVersion.VERSION
    )
  }
}