// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * The outcome of working out which handler should run for an observed event.
 */
internal sealed interface ReactorHandlerResolution {
    /** A handler should run. */
    data class Invoke(val handler: EventHandlerMethod) : ReactorHandlerResolution

    /**
     * A handler exists but is deliberately not being run because it is marked [OnceOnly] and the
     * event is arriving as part of a replay. The event still counts as observed.
     */
    data object SkippedForReplay : ReactorHandlerResolution

    /** The reactor has no handler for this event type at all. */
    data object NotHandled : ReactorHandlerResolution
}
