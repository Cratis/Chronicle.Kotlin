// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import kotlin.reflect.KClass

/**
 * Thrown when [Observes] names a class that is not annotated with [io.cratis.chronicle.events.EventType].
 */
class BatchMethodObservesUnknownEventType(observerClass: KClass<*>, methodName: String, eventType: KClass<*>) : IllegalArgumentException(
    "'${observerClass.simpleName}.$methodName' observes '${eventType.simpleName}', which is not an " +
        "event type. Observe classes annotated with @EventType."
)
