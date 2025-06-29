package io.github.airdaydreamers.easylogger.plugin

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CommandLineProcessor::class) // don't forget!
class EasyLoggerCommandLineProcessor : CommandLineProcessor {

  override val pluginId: String = "io.github.airdaydreamers.easylogger.plugin"

  override val pluginOptions: Collection<AbstractCliOption> = listOf(
      CliOption(
          optionName = "enabled", valueDescription = "<true|false>",
          description = "whether to enable the plugin or not",
          required = false,
          allowMultipleOccurrences = false
      ),
      CliOption(
          optionName = "logAnnotation", valueDescription = "<fqname>",
          description = "fully qualified name of the annotation(s) to use plugin",
          required = true,
          allowMultipleOccurrences = true
      ),
      CliOption(
          optionName = "logType", valueDescription = "<PRINTLN|LOG_D>",
          description = "the type of logging to use (PRINTLN or LOG_D)",
          required = false,
          allowMultipleOccurrences = false
      )
  )

  override fun processOption(
      option: AbstractCliOption,
      value: String,
      configuration: CompilerConfiguration
  ) = when (option.optionName) {
    "enabled" -> configuration.put(KEY_ENABLED, value.toBooleanStrictOrNull() ?: false)
    "logAnnotation" -> configuration.appendList(KEY_ANNOTATIONS, value)
    "logType" -> configuration.put(KEY_LOG_TYPE, value)
    else -> error("Unexpected configuration ${option.optionName}")
  }
}

val KEY_ENABLED = CompilerConfigurationKey<Boolean>("whether the plugin is enabled")
val KEY_ANNOTATIONS = CompilerConfigurationKey<List<String>>("our log annotations")
val KEY_LOG_TYPE = CompilerConfigurationKey<String>("the type of logging to use")
