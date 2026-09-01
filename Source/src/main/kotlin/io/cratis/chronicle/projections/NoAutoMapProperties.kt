// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties

/**
 * Collects the property names a read model, [ChildrenFrom] element type, or [Nested] type excludes
 * from AutoMap.
 *
 * The root read model, a children element type and a nested type each carry their own, independent
 * set of property-level [NoAutoMap] annotations. Collecting only the root's left a property-level
 * [NoAutoMap] on a child or nested type compiling, emitting no diagnostic, and doing nothing - AutoMap
 * silently overwrote a value the author had sourced explicitly.
 */
internal object NoAutoMapProperties {
    /** Collects the [NoAutoMap] property names declared directly on [type]. */
    fun collectFrom(type: KClass<*>): List<String> =
        type.memberProperties.filter { it.findAnnotation<NoAutoMap>() != null }.map { it.name }
}
