// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.eventSources

import kotlin.reflect.KClass

/** Thrown when two discovered event source definitions share a name. */
class DuplicateEventSourceName(val name: String, val types: List<KClass<*>>) : RuntimeException(
    "The event source name '$name' is used by more than one definition: " +
        types.joinToString { it.qualifiedName ?: it.toString() }
)
