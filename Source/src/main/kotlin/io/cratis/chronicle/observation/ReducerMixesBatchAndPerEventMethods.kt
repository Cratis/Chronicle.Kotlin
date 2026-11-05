// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import kotlin.reflect.KClass

/**
 * Thrown when a reducer declares a batch method alongside per-event handlers. Unlike a reactor, a
 * reducer cannot mix the two: it is one fold over an event source's events, in order.
 */
class ReducerMixesBatchAndPerEventMethods(reducerClass: KClass<*>) : IllegalArgumentException(
    "'${reducerClass.simpleName}' has a batch method and per-event handlers. A reducer has either " +
        "per-event handlers or one batch method, never both."
)
