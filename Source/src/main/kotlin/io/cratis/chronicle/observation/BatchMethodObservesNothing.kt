// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import kotlin.reflect.KClass

/**
 * Thrown when a batch method declares no [Observes], so there is nothing it would ever be called with.
 */
class BatchMethodObservesNothing(observerClass: KClass<*>, methodName: String) : IllegalArgumentException(
    "'${observerClass.simpleName}.$methodName' takes an EventBatch but declares no @Observes, " +
        "so no event would ever reach it. Add @Observes once per event type it handles."
)
