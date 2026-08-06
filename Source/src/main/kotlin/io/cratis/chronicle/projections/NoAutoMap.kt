// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

/**
 * Disables AutoMap.
 *
 * Placed on a read model, [ChildrenFrom] element, or [Nested] type, it disables AutoMap entirely
 * for that type. Placed on a single property, it excludes just that property from AutoMap while
 * siblings keep auto-mapping — useful to stop an unrelated event's same-named property from
 * clobbering an explicitly [SetFrom]-mapped property.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class NoAutoMap
