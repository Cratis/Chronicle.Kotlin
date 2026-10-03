// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSources

import kotlin.reflect.KClass

/** Thrown when an event source definition declares the same stream name more than once. */
class DuplicateEventStreamName(val type: KClass<*>, val name: String) : RuntimeException(
    "The event source '${type.qualifiedName ?: type}' declares the stream '$name' more than once."
)
