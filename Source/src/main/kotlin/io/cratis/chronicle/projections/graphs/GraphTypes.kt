// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import io.cratis.chronicle.keys.Key
import io.cratis.chronicle.projections.UnknownReadModelProperty
import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties

/** The name a type is reported by in an error. */
internal fun KClass<*>.displayName(): String = qualifiedName ?: java.name

/** [name] when it is a property of [type], otherwise an [UnknownReadModelProperty]. */
internal fun requireProperty(type: KClass<*>, name: String): String {
    if (type.memberProperties.none { it.name == name }) throw UnknownReadModelProperty(type, name)
    return name
}

/** The name of the one property of [type] marked [Key], or a [NodeMustHaveExactlyOneKey]. */
internal fun nodeKeyOf(type: KClass<*>): String {
    val keys = GraphMember.of(type).filter { it.has<Key>() }
    if (keys.size != 1) throw NodeMustHaveExactlyOneKey(type, keys.size)
    return keys[0].name
}
