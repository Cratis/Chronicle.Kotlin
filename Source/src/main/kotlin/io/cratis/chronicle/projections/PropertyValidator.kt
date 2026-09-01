// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/**
 * Validates that a property name referenced by a model-bound projection annotation actually exists
 * on the type it is checked against - typically an event type, or
 * [io.cratis.chronicle.events.EventContext].
 *
 * Kotlin emits whatever string a projection annotation was handed straight onto the wire. Without
 * this, a typo registers a broken server-side projection that only fails once the kernel evaluates it
 * against a real event, and it fails silently - a non-matching property path simply yields no value.
 */
internal object PropertyValidator {

    /**
     * Validates that [propertyName] exists on [type].
     *
     * Only the leading segment of a dot-separated path is checked - everything after the first `.` is
     * a navigation into that segment's own value, which this validator has no visibility into.
     *
     * @throws InvalidPropertyForType when the leading segment does not exist on [type].
     * @return [propertyName], unchanged, so a call can be chained inline where the mapping is built.
     */
    fun validatePropertyExists(type: KClass<*>, propertyName: String): String {
        val leadingSegment = propertyName.substringBefore('.')
        val exists = type.memberProperties.any { it.name.equals(leadingSegment, ignoreCase = true) }
        if (!exists) throw InvalidPropertyForType(type, propertyName)
        return propertyName
    }

    /**
     * Validates [key] against [type] unless it is a well-known projection expression rather than a
     * property name - the [EVENT_SOURCE_ID_KEY] sentinel, or a `$`-prefixed expression such as a
     * constant or composite key.
     *
     * @return [key], unchanged, so a call can be chained inline where the key is resolved.
     */
    fun validateKeyIfExplicit(type: KClass<*>, key: String): String {
        if (key == EVENT_SOURCE_ID_KEY || key.startsWith('$')) return key
        return validatePropertyExists(type, key)
    }
}
