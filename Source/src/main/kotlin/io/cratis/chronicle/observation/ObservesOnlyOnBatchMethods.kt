// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import kotlin.reflect.KClass

/**
 * Thrown when [Observes] is put on a method that does not take an [EventBatch] as its first parameter.
 *
 * A per-event handler already says what it handles through the type of its event parameter.
 */
class ObservesOnlyOnBatchMethods(observerClass: KClass<*>, methodName: String) : IllegalArgumentException(
    "'${observerClass.simpleName}.$methodName' declares @Observes, but only a batch method - one whose " +
        "first parameter is an EventBatch - can observe event types that way."
)
