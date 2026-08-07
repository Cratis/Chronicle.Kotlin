// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.observation.IReducersService
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking

/**
 * Reducers for Java, without the coroutines.
 *
 * As with reactors, registering by hand is rarely needed - a `@Reducer` on the classpath is
 * discovered and registered on connect. This is for the case where registration was turned off, or
 * the reducer is built with dependencies the client cannot supply.
 *
 * @param reducers The reducers service to forward to.
 */
class BlockingReducers(private val reducers: IReducersService) {

    /** The suspending service underneath. */
    fun unwrap(): IReducersService = reducers

    /**
     * Registers [reducer] and starts it folding.
     *
     * Registering a reducer registers the read model it produces too.
     *
     * @param reducer The reducer instance.
     * @return The job running the observation, so it can be cancelled.
     */
    fun register(reducer: Any): Job = runBlocking { reducers.register(reducer) }
}
