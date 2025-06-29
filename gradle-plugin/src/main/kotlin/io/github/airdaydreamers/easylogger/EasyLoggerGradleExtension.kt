package io.github.airdaydreamers.easylogger

enum class LogType {
  PRINTLN,
  LOG_D
}

open class EasyLoggerGradleExtension {
  var logType: LogType = LogType.PRINTLN
  var enabled: Boolean = true
  var annotations: List<String> = emptyList()
}
