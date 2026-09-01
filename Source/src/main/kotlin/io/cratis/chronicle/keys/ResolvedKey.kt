// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.keys

/**
 * The key a [KeyBuilder] resolved for an event, once every call on it has been made.
 */
sealed class ResolvedKey {
    /** The default - the event source id. */
    object EventSourceId : ResolvedKey()

    /** A single property on the event. */
    data class Property(val name: String) : ResolvedKey()

    /** A value from the event context. */
    data class Context(val property: String) : ResolvedKey()

    /** More than one property on the event, combined in order. */
    data class Composite(val properties: List<String>) : ResolvedKey()
}
