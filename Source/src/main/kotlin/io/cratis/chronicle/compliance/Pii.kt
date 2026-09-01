// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.compliance

/**
 * Marks a property, constructor parameter, field, or type as containing personally identifiable
 * information (PII).
 *
 * Declare it once on a [io.cratis.chronicle.concepts.ConceptAs] and every event or read model
 * property that reuses that concept is PII automatically - there is nothing to repeat at each call
 * site. It also works declared directly on an event or read model property, and on a composite
 * value object type, in which case every value the type holds is treated as PII wherever that type
 * appears.
 *
 * `@Pii` cannot be applied to an [io.cratis.chronicle.concepts.EventSourceId] - see
 * [PiiNotSupportedOnEventSourceId] for why.
 *
 * @property description Optional description of the PII data.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.CLASS, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class Pii(val description: String = "")
