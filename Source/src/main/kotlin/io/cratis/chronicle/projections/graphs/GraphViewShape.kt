// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

import io.cratis.chronicle.keys.Key
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf
import java.lang.reflect.Modifier

/** A member of a view class as declared, before it is checked against the graph. */
internal data class GraphViewMemberShape(
    val name: String,
    val kind: GraphViewMemberKind,
    val isKey: Boolean,
    val edgeSource: String,
    val edgeClass: KClass<*>?,
    val direction: FollowDirection,
    val many: Boolean,
    val target: KClass<*>?
)

/** A view class read from its annotations. */
internal data class GraphViewShape(
    val type: KClass<*>,
    val root: KClass<*>,
    val name: String,
    val members: List<GraphViewMemberShape>
) {
    companion object {
        /** Reads [type], which carries [ViewOf]. */
        fun read(type: KClass<*>): GraphViewShape {
            val annotation = type.findAnnotation<ViewOf>()!!
            val declared = GraphMember.of(type).associateBy { it.name }
            val members = propertyNamesInOrder(type).mapNotNull { declared[it] }.map(::member)
            return GraphViewShape(type, annotation.value, annotation.name.ifEmpty { type.simpleName ?: type.java.simpleName }, members)
        }

        /** The properties of [type] in declaration order, which reflection over properties does not give. */
        fun propertyNamesInOrder(type: KClass<*>): List<String> =
            type.java.declaredFields
                .filter { !Modifier.isStatic(it.modifiers) && !it.isSynthetic }
                .map { it.name }

        private fun member(member: GraphMember): GraphViewMemberShape {
            val follows = member.find<Follows>()
            val fromEdge = member.find<FromEdge>()
            val kind = when {
                follows != null -> GraphViewMemberKind.Relation
                fromEdge != null -> GraphViewMemberKind.EdgeProperty
                else -> GraphViewMemberKind.Property
            }
            val many = isCollection(member.type)
            return GraphViewMemberShape(
                member.name,
                kind,
                member.has<Key>(),
                fromEdge?.source?.ifEmpty { null } ?: member.name,
                follows?.value,
                follows?.direction ?: FollowDirection.Derived,
                many,
                if (kind == GraphViewMemberKind.Relation) targetOf(member.type, many) else null
            )
        }

        private fun isCollection(type: KType): Boolean {
            val classifier = type.classifier as? KClass<*> ?: return false
            return classifier.isSubclassOf(Iterable::class) || classifier.java.isArray
        }

        private fun targetOf(type: KType, many: Boolean): KClass<*>? =
            if (many) type.arguments.firstOrNull()?.type?.classifier as? KClass<*> else type.classifier as? KClass<*>
    }
}

/** Whether this class is a view over a property graph. */
internal fun KClass<*>.isGraphView(): Boolean = hasAnnotation<ViewOf>()
