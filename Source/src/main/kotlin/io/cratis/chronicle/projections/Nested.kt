// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

/**
 * Marks a single nullable property as a nested sub-object built from its own type's
 * [FromEvent]/[SetFrom] annotations, and optionally cleared by [ClearWith].
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Nested
