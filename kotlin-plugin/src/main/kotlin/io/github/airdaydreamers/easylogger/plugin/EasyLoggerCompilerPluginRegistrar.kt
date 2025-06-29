package io.github.airdaydreamers.easylogger.plugin

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

enum class LogType {
    PRINTLN,
    LOG_D
}

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class EasyLoggerCompilerPluginRegistrar : CompilerPluginRegistrar() {

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        if (configuration[KEY_ENABLED] == false) {
            return
        }

        val logTypeString = configuration[KEY_LOG_TYPE]
        val logType = when (logTypeString?.uppercase()) {
            "LOG_D" -> LogType.LOG_D
            "PRINTLN" -> LogType.PRINTLN
            null -> LogType.PRINTLN // Default if not provided
            else -> {
                // Optionally, report a warning for invalid values
                // You can access messageCollector through the configuration if needed
                LogType.PRINTLN // Default for invalid values
            }
        }

        val logAnnotations = configuration[KEY_ANNOTATIONS]
            ?: error("easylogger requires at least one annotation option passed to it")

        // Register IR generation extension
        IrGenerationExtension.registerExtension(
            EasyLoggerIrGenerationExtension(
                logAnnotations = logAnnotations,
                messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY),
                logType = logType
            )
        )
    }
}