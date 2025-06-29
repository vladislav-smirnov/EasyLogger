# EasyLogger

A Kotlin compiler plugin that provides simple and efficient function logging capabilities through annotations.

---

## Features

- Simple annotation-based logging
- Function entry and exit logging
- Parameter value logging
- Return value logging
- Support for both println and Android Log.d outputs
- Gradle plugin for easy integration

---

## Installation

Add the plugin to your project's `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.airdaydreamers.easylogger.plugin") version "0.0.22"
}
```

---

## Configuration

Configure the plugin in your build.gradle.kts:

```kotlin
import io.github.airdaydreamers.easylogger.EasyLoggerGradleExtension
import io.github.airdaydreamers.easylogger.LogType

extensions.configure<EasyLoggerGradleExtension>("easyLogger") {
    enabled = true // Enable/disable the plugin
    annotations = listOf("io.github.airdaydreamers.easylogger.annotation.DebugLog") // Specify annotations to trigger logging
    logType = LogType.PRINTLN // Use PRINTLN or LOG_D (for Android)
}
```

---

## Usage

1. Add the @DebugLog annotation to any function you want to log:

```kotlin
@DebugLog
fun greet(name: String): String {
    return "Hello, $name!"
}
```

2. The plugin will automatically generate logging for:
   - Function entry with parameter values
   - Function exit with return value
   - Execution time in milliseconds
  
Example output:
```
⇢ greet(name=World)
⇠ greet [ran in 1 ms] = Hello, World!
```

---

## Android Support

For Android projects, you can use `LogType.LOG_D` which will output logs using Android's `Log.d`:

```kotlin
extensions.configure<EasyLoggerGradleExtension>("easyLogger") {
    logType = LogType.LOG_D
}
```
---

## Requirements

- Kotlin 1.9.x or higher
- Gradle 8.x or higher
- JDK 17 or higher

---

## Contributing

Contributions are welcome! Feel free to fork the repository, open issues, and submit pull requests if you'd like to contribute to the project!
Please make sure your code follows the project's guidelines and includes relevant documentation or tests.

---

Author
------
Vladislav Smirnov - @vladislav-smirnov on GitHub

---

## License

This project is licensed under the Apache License 2.0.  
See the [LICENSE](LICENSE) file for more details.

```
Copyright © 2025 Vladislav Smirnov

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
