// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import kotlinx.coroutines.Job

/**
 * Discovers the handler methods on [IReadModelReactor] instances and keeps them fed with read model
 * changes.
 */
interface IReadModelReactors {
    /**
     * Starts watching every read model [reactor] declares a handler for, dispatching each change to
     * the matching handlers.
     *
     * Returns the [Job] backing the subscriptions so a single reactor can be cancelled on its own;
     * use [stop] to cancel every reactor registered here.
     *
     * @param reactor The reactor to register.
     * @return The [Job] backing the reactor's subscriptions.
     * @throws io.cratis.chronicle.observation.InvalidHandlerSignature When a handler method cannot be dispatched to.
     */
    fun register(reactor: IReadModelReactor): Job

    /** Cancels every registered reactor's subscriptions. */
    fun stop()
}
