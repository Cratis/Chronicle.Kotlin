// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.artifacts

import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

private class WithoutArguments

private class WithDefaultedArguments(val greeting: String = "hello")

private class NeedingDependencies(val dependency: Any)

private object AsAnObject

class ArtifactActivatorTests {

    @Test
    fun `activates a class with a constructor that takes nothing`() {
        assertInstanceOf(WithoutArguments::class.java, ArtifactActivator.activate(WithoutArguments::class))
    }

    @Test
    fun `activates a class whose constructor arguments all have defaults`() {
        val activated = ArtifactActivator.activate(WithDefaultedArguments::class)
        assertInstanceOf(WithDefaultedArguments::class.java, activated)
    }

    @Test
    fun `hands back the singleton instance of an object declaration`() {
        assertSame(AsAnObject, ArtifactActivator.activate(AsAnObject::class))
    }

    @Test
    fun `explains itself when the artifact needs dependencies it cannot supply`() {
        assertThrows(ArtifactActivationFailed::class.java) {
            ArtifactActivator.activate(NeedingDependencies::class)
        }
    }
}
