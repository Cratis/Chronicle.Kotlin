// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * How an observer's identity and sequence are read off its class.
 *
 * Registration works this out from the `@Reactor` or `@Reducer` annotation. Anything else that has
 * to name the same observer afterwards - asking which partitions it is failing on, retrying one -
 * has to arrive at exactly the same answer, so both go through here.
 */
internal object ObserverDeclaration {
    /** The identifier [observerClass] is registered under, defaulting to its simple name. */
    fun idOf(observerClass: KClass<*>): String =
        observerClass.findAnnotation<Reactor>()?.id?.ifEmpty { null }
            ?: observerClass.findAnnotation<Reducer>()?.id?.ifEmpty { null }
            ?: observerClass.simpleName
            ?: throw IllegalArgumentException(
                "An observer has to be a named class - an anonymous one has no identity to register under"
            )

    /** The event sequence [observerClass] observes, defaulting to the event log. */
    fun eventSequenceIdOf(observerClass: KClass<*>): String =
        EventSequence.idOf(
            observerClass,
            observerClass.findAnnotation<Reactor>()?.eventSequence
                ?: observerClass.findAnnotation<Reducer>()?.eventSequence
        )
}
