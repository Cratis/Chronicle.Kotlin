// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.artifacts

/**
 * Held shut until the client has finished telling the kernel what the application is made of.
 *
 * Registration happens on connect, in the background. Without something to wait on, the very first
 * append after `getEventStore` is a race: the client may not have registered the event type yet, and
 * the kernel rejects events of a type it has never been told about. That race is not one an
 * application should have to know exists - so the first append waits here instead.
 *
 * Once the first pass is through, waiting costs nothing.
 */
fun interface IRegistrationGate {
    /** Returns once the first registration pass has been through, immediately thereafter. */
    suspend fun awaitOpen()

    companion object {
        /**
         * A gate that is never shut, for an event sequence constructed on its own rather than by an
         * event store - there is no registration to wait for.
         */
        @JvmField
        val open: IRegistrationGate = IRegistrationGate { }
    }
}
