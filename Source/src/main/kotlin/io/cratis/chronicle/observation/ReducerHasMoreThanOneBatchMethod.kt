// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import kotlin.reflect.KClass

/**
 * Thrown when a reducer declares more than one batch method. A reducer is one fold over the events of
 * an event source in order, and splitting the events between methods would break the fold.
 */
class ReducerHasMoreThanOneBatchMethod(reducerClass: KClass<*>) : IllegalArgumentException(
    "'${reducerClass.simpleName}' has more than one batch method. A reducer folds the events of one " +
        "event source in order, so it has exactly one batch method observing all of its event types."
)
