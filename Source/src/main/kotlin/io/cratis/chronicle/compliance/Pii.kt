// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.compliance

/**
 * Marks a field, property, or class as containing personally identifiable information (PII).
 *
 * @property description Optional description of the PII data.
 */
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Pii(val description: String = "")
