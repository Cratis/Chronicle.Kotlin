// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.artifacts

import kotlin.reflect.KClass
import kotlin.reflect.jvm.isAccessible

/**
 * The [IArtifactActivator] used when nothing else is configured.
 *
 * Activates a Kotlin `object` by handing back its singleton instance, and anything else by calling a
 * constructor that needs no arguments — either a genuine no-arg constructor or one where every
 * parameter has a default value. That covers artifacts as they are normally written: reactors,
 * reducers, constraints and seeders with no dependencies of their own.
 *
 * An artifact that *does* need dependencies belongs in a container — see [IArtifactActivator].
 */
object ArtifactActivator : IArtifactActivator {
    override fun activate(type: KClass<*>): Any =
        try {
            type.singletonInstance() ?: type.newInstance()
        } catch (cause: Throwable) {
            throw ArtifactActivationFailed(type, cause)
        }

    /**
     * The singleton behind a Kotlin `object`, or `null` for an ordinary class.
     *
     * Falls back to reading the field the compiler generates, because asking Kotlin reflection for the
     * instance of an object that is not public fails rather than answering.
     */
    private fun KClass<*>.singletonInstance(): Any? =
        runCatching { objectInstance }.getOrNull()
            ?: runCatching { java.getDeclaredField("INSTANCE").apply { isAccessible = true }.get(null) }.getOrNull()

    /**
     * Calls the constructor that needs no arguments, opening it up first.
     *
     * An artifact kept private to its own file is a perfectly reasonable thing to write — small samples
     * and specs do it constantly — and there is no reason to make visibility the difference between an
     * artifact that registers and one that does not.
     */
    private fun KClass<*>.newInstance(): Any {
        val constructor = constructors.firstOrNull { candidate -> candidate.parameters.all { it.isOptional } }
            ?: throw NoSuchMethodException("'${qualifiedName ?: java.name}' has no constructor that can be called without arguments.")
        constructor.isAccessible = true
        return constructor.callBy(emptyMap())
    }
}
