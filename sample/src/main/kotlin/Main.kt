package com.example.sample

import io.github.airdaydreamers.easylogger.annotation.DebugLog

class Sample {
    @DebugLog
    fun greet(name: String): String {
        val message = "Hello, $name!"
        return message
    }

    @DebugLog
    fun test(newNameExample: String): String {
        val message = "My name is $newNameExample!"

        return message
    }
}

fun main() {
    println("Starting sample application...")

    val sample = Sample()
    val result = sample.greet("World")
    val result123 = sample.test("WOOOW")

    println("Sample application finished.")
}
