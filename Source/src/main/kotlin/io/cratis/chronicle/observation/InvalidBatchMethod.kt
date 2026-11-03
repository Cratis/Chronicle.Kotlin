// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import kotlin.reflect.KClass

/**
 * Thrown when a batch method has a shape the client cannot call - a parameter it cannot supply, or a
 * return type that does not fit the observer.
 */
class InvalidBatchMethod(observerClass: KClass<*>, methodName: String, reason: String) : IllegalArgumentException(
    "'${observerClass.simpleName}.$methodName' is not a valid batch method: $reason"
)
