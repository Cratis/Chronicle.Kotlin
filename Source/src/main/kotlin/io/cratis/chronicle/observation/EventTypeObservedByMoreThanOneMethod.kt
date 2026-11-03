// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import kotlin.reflect.KClass

/**
 * Thrown when an event type would reach more than one method of an observer - two batch methods
 * observe it, or a batch method observes it and a per-event handler takes it too.
 *
 * Each event type reaches exactly one method, so which method handles an event is never in doubt.
 */
class EventTypeObservedByMoreThanOneMethod(observerClass: KClass<*>, eventTypeId: String) : IllegalArgumentException(
    "'$eventTypeId' reaches more than one method of '${observerClass.simpleName}'. Each event type " +
        "reaches exactly one method: either one batch method observes it, or one per-event handler takes it."
)
