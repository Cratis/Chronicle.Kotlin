// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.artifacts

import kotlin.reflect.KClass

/**
 * Turns a discovered artifact class into the instance the client registers.
 *
 * Discovery finds *what* an application consists of; activation decides *how* those pieces come into
 * being. Plain applications get [ArtifactActivator], which constructs artifacts directly. A container
 * — Spring, Guice, Koin — plugs in its own so that reactors, reducers, constraints and seeders can
 * take dependencies through their constructors like any other component.
 */
fun interface IArtifactActivator {
    /**
     * Creates — or resolves — the instance for [type].
     *
     * @param type The artifact class to activate.
     * @return The artifact instance.
     * @throws ArtifactActivationFailed When the artifact could not be activated.
     */
    fun activate(type: KClass<*>): Any
}
