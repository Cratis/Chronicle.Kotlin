// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

import kotlin.reflect.KClass

sealed class ConstraintBuilderEntry {
    data class UniqueForEntry(
        val eventClass: KClass<*>,
        val message: String
    ) : ConstraintBuilderEntry()

    data class UniqueEntry(
        val eventClass: KClass<*>,
        val propertyName: String,
        val ignoreCasing: Boolean,
        val message: String
    ) : ConstraintBuilderEntry()
}
