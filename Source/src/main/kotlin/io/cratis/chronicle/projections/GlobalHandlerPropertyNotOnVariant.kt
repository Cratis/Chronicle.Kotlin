// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

import kotlin.reflect.KClass

/**
 * Thrown when a [GlobalFor] shared handler maps a property that the variant it is being merged into
 * does not have.
 *
 * @param globalHandlerClass The shared handler declaring the mapping.
 * @param variantClass The variant the mapping was merged into.
 * @param propertyName The property the mapping targets.
 */
class GlobalHandlerPropertyNotOnVariant(
    globalHandlerClass: KClass<*>,
    variantClass: KClass<*>,
    propertyName: String
) : IllegalStateException(
    "'${globalHandlerClass.simpleName}' maps '$propertyName', but '${variantClass.simpleName}' has no such property."
)
