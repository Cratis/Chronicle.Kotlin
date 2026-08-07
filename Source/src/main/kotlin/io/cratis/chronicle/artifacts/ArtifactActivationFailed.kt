// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.artifacts

import kotlin.reflect.KClass

/**
 * Thrown when a discovered artifact could not be instantiated.
 *
 * The usual cause is a constructor that needs arguments the activator cannot supply. Either give the
 * artifact a constructor that needs nothing, give its parameters defaults, or activate it through a
 * container by configuring an [IArtifactActivator] that can resolve them.
 *
 * @param type The artifact class that could not be activated.
 * @param cause The underlying failure.
 */
class ArtifactActivationFailed(type: KClass<*>, cause: Throwable) : Exception(
    "Could not activate the Chronicle artifact '${type.qualifiedName ?: type.java.name}'. " +
        "Artifacts are activated through a constructor that takes no arguments. Give it one, give " +
        "every parameter a default value, or configure an IArtifactActivator that can resolve its dependencies.",
    cause
)
