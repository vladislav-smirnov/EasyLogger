package io.github.airdaydreamers.easylogger

import com.google.auto.service.AutoService
import io.github.airdaydreamers.easylogger.plugin.internal.PluginVersion
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

@AutoService(KotlinCompilerPluginSupportPlugin::class) // NOTE: Don't like it. Will use manually later.
class EasyLoggerGradleSubplugin : KotlinCompilerPluginSupportPlugin {

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
    return kotlinCompilation.target.project.plugins.hasPlugin(EasyLoggerGradlePlugin::class.java)
  }

  override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.target.project
    val extension = project.extensions.findByType(EasyLoggerGradleExtension::class.java)
        ?: EasyLoggerGradleExtension()

    if (extension.enabled && extension.annotations.isEmpty()) {
      project.logger.error("EasyLogger is enabled, but no annotations were set")
    }

    return project.provider {
      val annotationOptions = extension.annotations.map {
          SubpluginOption(
              key = "LogAnnotation",
              value = it
          )
      }
      val enabledOption = SubpluginOption(key = "enabled", value = extension.enabled.toString())
      annotationOptions + enabledOption
    }
  }

  override fun getCompilerPluginId(): String = "io.github.airdaydreamers.easylogger.plugin"

  override fun getPluginArtifact(): SubpluginArtifact {
    return SubpluginArtifact(
        groupId = "io.github.airdaydreamers.easylogger.plugin",
        artifactId = "kotlin-plugin",
        version = PluginVersion.VERSION
    )
  }
}