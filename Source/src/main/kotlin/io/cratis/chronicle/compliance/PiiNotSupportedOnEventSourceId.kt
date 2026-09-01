// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.compliance

import kotlin.reflect.KClass

/**
 * Thrown when [Pii] is applied to a concept that is also an
 * [io.cratis.chronicle.concepts.EventSourceId].
 *
 * The event source id is what the kernel uses to look up the encryption key for every other
 * `@Pii` value belonging to that source. Encrypting the id itself would make its own key
 * unfindable, so the schema generator refuses to register a type in that shape.
 *
 * @param type The event source id concept that was incorrectly marked [Pii].
 */
class PiiNotSupportedOnEventSourceId(type: KClass<*>) : Exception(
    "'${type.qualifiedName ?: type.java.name}' cannot be marked @Pii because it is an EventSourceId. " +
        "Encrypting an event source id makes its own decryption key unfindable. If the identifier itself " +
        "is personal, use a random surrogate id as the event source id and keep the personal value in a " +
        "separate @Pii property instead."
)
