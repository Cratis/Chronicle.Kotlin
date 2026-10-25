// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.correlation

import java.io.File
import java.net.URLClassLoader
import java.nio.file.Files
import javax.tools.ToolProvider
import org.junit.jupiter.api.Assertions.assertEquals

/** Executes a Java caller compiled against the old signatures with only the real production classes on the runtime path. */
class CorrelationBinaryCompatibilityTests {
    private fun resource(path: String): File =
        File(checkNotNull(javaClass.classLoader.getResource("correlation-binary/$path")) { path }.toURI())

    @org.junit.jupiter.api.Test
    fun `caller compiled against the old signatures links and runs against the new classes`() {
        val compiler = checkNotNull(ToolProvider.getSystemJavaCompiler()) { "a JDK is required" }
        val stubs = Files.createTempDirectory("correlation-stubs").toFile()
        val caller = Files.createTempDirectory("correlation-caller").toFile()
        try {
            val stubSources = resource("old").walkTopDown().filter { it.extension == "java" }.toList()
            // Stage 1: compile the frozen old signatures into a compile-only directory.
            val stubResult = compiler.run(null, null, null, "-d", stubs.path, *stubSources.map { it.path }.toTypedArray())
            assertEquals(0, stubResult, "old signature fixtures must compile")
            // Stage 2: compile the caller against the stubs only; its output holds just the caller class.
            val callerResult = compiler.run(null, null, null, "-cp", stubs.path, "-d", caller.path,
                resource("caller/OldCorrelationCaller.java").path)
            assertEquals(0, callerResult, "caller must compile against the old signatures")
            assertEquals(listOf("OldCorrelationCaller.class"), caller.walkTopDown().filter { it.isFile }.map { it.name }.toList())

            URLClassLoader(arrayOf(caller.toURI().toURL()), javaClass.classLoader).use { loader ->
                val type = loader.loadClass("OldCorrelationCaller")
                assertEquals("ok", type.getMethod("run").invoke(null))
                // Real, not stub, classes were linked.
                assertEquals(CorrelationIdManager::class.java, loader.loadClass("io.cratis.chronicle.correlation.CorrelationIdManager"))
            }
        } finally {
            stubs.deleteRecursively()
            caller.deleteRecursively()
        }
    }
}
