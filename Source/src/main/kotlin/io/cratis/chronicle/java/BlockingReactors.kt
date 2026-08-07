// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.observation.IReactorsService
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking

/**
 * Reactors for Java, without the coroutines.
 *
 * Registering by hand is rarely needed - a `@Reactor` on the classpath is discovered and registered
 * on connect. This is for the case where registration was turned off, or the reactor is built with
 * dependencies the client cannot supply.
 *
 * @param reactors The reactors service to forward to.
 */
class BlockingReactors(private val reactors: IReactorsService) {

    /** The suspending service underneath. */
    fun unwrap(): IReactorsService = reactors

    /**
     * Registers [reactor] and starts it observing.
     *
     * @param reactor The reactor instance.
     * @return The job running the observation, so it can be cancelled.
     */
    fun register(reactor: Any): Job = runBlocking { reactors.register(reactor) }
}
