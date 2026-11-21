// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.patterns

/**
 * The name of a facet - one dimension of the context a behavior pattern was observed in.
 *
 * The well-known names are the ones the kernel mines with; their values are matched exactly, so use
 * these rather than spelling the names out.
 *
 * @param value The name as the kernel knows it.
 */
data class FacetName(val value: String) {
    /** Whether this facet names the action itself rather than the context it happened in. */
    val isAction: Boolean get() = this == CommandType

    override fun toString(): String = value

    companion object {
        /** No facet name. */
        @JvmField val Unspecified = FacetName("")

        /** The type of command that was executed - the action. */
        @JvmField val CommandType = FacetName("CommandType")

        /** The type of initiator - for instance a user or a system. */
        @JvmField val InitiatorType = FacetName("InitiatorType")

        /** The identity of the initiator. */
        @JvmField val InitiatorId = FacetName("InitiatorId")

        /** Who the action was performed on behalf of. */
        @JvmField val OnBehalfOf = FacetName("OnBehalfOf")

        /** The command that caused the action. */
        @JvmField val CausedByCommand = FacetName("CausedByCommand")

        /** The root correlation the action belongs to. */
        @JvmField val CorrelationRootId = FacetName("CorrelationRootId")

        /** The type of aggregate the action was performed on. */
        @JvmField val AggregateType = FacetName("AggregateType")

        /** The year the action happened in. */
        @JvmField val Year = FacetName("Year")

        /** The month the action happened in. */
        @JvmField val Month = FacetName("Month")

        /** The day of the week the action happened on, such as `Monday`. */
        @JvmField val Day = FacetName("Day")

        /** The [io.cratis.chronicle.patterns.TimeBucket] of the day the action happened in. */
        @JvmField val TimeBucket = FacetName("TimeBucket")
    }
}
