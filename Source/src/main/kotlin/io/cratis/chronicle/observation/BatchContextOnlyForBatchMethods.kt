// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import kotlin.reflect.KClass

/**
 * Thrown when a per-event handler declares a [BatchContext] parameter. The context describes a page
 * of events, which only a batch method is called with.
 */
class BatchContextOnlyForBatchMethods(observerClass: KClass<*>, methodName: String) : IllegalArgumentException(
    "'${observerClass.simpleName}.$methodName' takes a BatchContext, but it handles one event at a time. " +
        "Only a batch method - one whose first parameter is an EventBatch - can take a BatchContext."
)
