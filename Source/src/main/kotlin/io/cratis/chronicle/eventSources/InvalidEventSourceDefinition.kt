// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSources

import kotlin.reflect.KClass

/** Thrown when an event source definition is malformed, such as a blank name. */
class InvalidEventSourceDefinition(val type: KClass<*>, reason: String) : RuntimeException(
    "The event source definition on '${type.qualifiedName ?: type}' is invalid: $reason"
)
