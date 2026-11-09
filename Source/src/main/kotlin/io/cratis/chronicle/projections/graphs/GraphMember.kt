// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

/**
 * A property of a node or an edge class, with the annotations that sit on it - on the Kotlin property, or on
 * the backing field of a Java class or record.
 */
internal data class GraphMember(val name: String, val type: KType, val annotations: List<Annotation>) {
    /** Whether the member carries an annotation of type [TAnnotation]. */
    inline fun <reified TAnnotation : Annotation> has(): Boolean = annotations.any { it is TAnnotation }

    /** The annotation of type [TAnnotation] on the member, when it carries one. */
    inline fun <reified TAnnotation : Annotation> find(): TAnnotation? = annotations.filterIsInstance<TAnnotation>().firstOrNull()

    companion object {
        /** Every property of [type], in no particular order. */
        fun of(type: KClass<*>): List<GraphMember> =
            type.memberProperties.map { property ->
                GraphMember(
                    property.name,
                    property.returnType,
                    (property.annotations + (property.javaField?.annotations?.toList() ?: emptyList())).distinct()
                )
            }
    }
}
