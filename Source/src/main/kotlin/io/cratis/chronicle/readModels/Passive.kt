// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

/**
 * Marks a model-bound projection's read model as passive.
 *
 * A passive projection is registered with the kernel but does not run actively - its read model is
 * computed on demand rather than kept up to date as events arrive. Use this for a read model that is
 * expensive to keep warm and only occasionally queried.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Passive
