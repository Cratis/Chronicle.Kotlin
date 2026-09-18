// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Thrown when a type is declared [VariantOf] a logical identity without naming the event that
 * activates it with [EntersOn], or without calling `entersOn(...)` on the declarative builder.
 *
 * @param variantClass The variant that declared no entering event.
 */
class VariantMustDeclareEntersOnEvent(
    variantClass: KClass<*>
) : IllegalStateException(
    "'${variantClass.simpleName}' is declared a variant but names no entering event. " +
        "Add @EntersOn(...) or call entersOn(...) to say which event activates it."
)
