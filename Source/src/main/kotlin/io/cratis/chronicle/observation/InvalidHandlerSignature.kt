// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import kotlin.reflect.KClass

/**
 * Thrown when a method is shaped like a handler - its first parameter is an event type - but its
 * remaining parameters are not something the client can dispatch to.
 *
 * Left unchecked, such a method registers successfully and then fails on every event it receives, or
 * is silently skipped so the observer subscribes to nothing. Failing at registration turns a
 * confusing runtime symptom into an obvious startup error.
 */
class InvalidHandlerSignature(
    observerClass: KClass<*>,
    methodName: String,
    reason: String
) : IllegalArgumentException(
    "'${observerClass.simpleName}.$methodName' is not a valid handler: $reason"
)
