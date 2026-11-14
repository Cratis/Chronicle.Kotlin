// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections.graphs

/**
 * A member of a [GraphView], in declaration order.
 *
 * @property name The name of the member in the view.
 * @property kind What the member is.
 * @property source The name of the node property ([GraphViewMemberKind.Property]) or edge property
 *   ([GraphViewMemberKind.EdgeProperty]) the member reads; `null` for a relation.
 * @property edge The type of the edge a relation follows, such as `MEMBER_OF`; `null` for any other member.
 * @property direction The direction in which a relation follows the edge; `null` for any other member.
 * @property cardinality How many nodes a relation reaches; `null` for any other member.
 * @property view The name of the view a relation leads to; `null` for any other member.
 */
data class GraphViewMember(
    val name: String,
    val kind: GraphViewMemberKind,
    val source: String? = null,
    val edge: String? = null,
    val direction: EdgeDirection? = null,
    val cardinality: GraphViewCardinality? = null,
    val view: String? = null
)
