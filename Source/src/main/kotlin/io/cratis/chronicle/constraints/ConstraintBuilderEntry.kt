// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.constraints

import kotlin.reflect.KClass

sealed class ConstraintBuilderEntry {
    data class UniqueForEntry(
        val eventClass: KClass<*>,
        val message: String,
        val scope: ConstraintScope? = null
    ) : ConstraintBuilderEntry()

    /**
     * A unique constraint over one or more event types, each with one or more properties.
     *
     * Not a data class on purpose: this type used to be one over a single event type and property, and
     * a data class cannot keep its old `copy` and `componentN` members once its shape changes. They
     * are kept, deprecated, so code compiled against the single-property shape keeps linking.
     *
     * @param eventDefinitions The event types and properties the constraint covers - at least one. Copied.
     * @param ignoreCasing Whether values are compared without regard to casing.
     * @param message What to tell the caller when the constraint is violated.
     * @param scope The scope the uniqueness is checked within, or `null` for global.
     */
    class UniqueEntry(
        eventDefinitions: List<UniqueEventDefinition>,
        val ignoreCasing: Boolean,
        val message: String,
        val scope: ConstraintScope? = null
    ) : ConstraintBuilderEntry() {
        /** The event types and properties the constraint covers. Copied on construction, so it cannot change afterwards. */
        val eventDefinitions: List<UniqueEventDefinition> = eventDefinitions.toList()

        init {
            require(this.eventDefinitions.isNotEmpty()) { "A unique constraint needs at least one event type." }
        }

        /** The single-event, single-property form this type had before multi-property constraints. */
        @Deprecated("Pass the event definitions instead - a unique constraint can cover several properties and event types.")
        constructor(
            eventClass: KClass<*>,
            propertyName: String,
            ignoreCasing: Boolean,
            message: String,
            scope: ConstraintScope? = null
        ) : this(listOf(UniqueEventDefinition(eventClass, listOf(propertyName))), ignoreCasing, message, scope)

        /** The event type of the first definition. */
        @Deprecated("Read eventDefinitions - a unique constraint can cover several event types.")
        val eventClass: KClass<*>
            get() = eventDefinitions.first().eventClass

        /** The first property of the first definition. */
        @Deprecated("Read eventDefinitions - a unique constraint can cover several properties.")
        val propertyName: String
            get() = eventDefinitions.first().properties.first()

        @Deprecated("Read eventDefinitions instead.")
        operator fun component1(): KClass<*> = eventDefinitions.first().eventClass

        @Deprecated("Read eventDefinitions instead.")
        operator fun component2(): String = eventDefinitions.first().properties.first()

        @Deprecated("Read ignoreCasing instead.")
        operator fun component3(): Boolean = ignoreCasing

        @Deprecated("Read message instead.")
        operator fun component4(): String = message

        @Deprecated("Read scope instead.")
        operator fun component5(): ConstraintScope? = scope

        /**
         * The copy this type had before multi-property constraints. Leaving [eventClass] and
         * [propertyName] as they are keeps every event definition; naming a different one replaces the
         * definitions with that single one.
         */
        @Deprecated("Construct a UniqueEntry with the event definitions you want instead.")
        fun copy(
            eventClass: KClass<*> = eventDefinitions.first().eventClass,
            propertyName: String = eventDefinitions.first().properties.first(),
            ignoreCasing: Boolean = this.ignoreCasing,
            message: String = this.message,
            scope: ConstraintScope? = this.scope
        ): UniqueEntry {
            val first = eventDefinitions.first()
            val unchanged = eventClass == first.eventClass && propertyName == first.properties.first()
            val definitions = if (unchanged) eventDefinitions else listOf(UniqueEventDefinition(eventClass, listOf(propertyName)))
            return UniqueEntry(definitions, ignoreCasing, message, scope)
        }

        /** A copy of this entry within the given scope, keeping every event definition. */
        internal fun withScope(scope: ConstraintScope?): UniqueEntry = UniqueEntry(eventDefinitions, ignoreCasing, message, scope)

        override fun equals(other: Any?): Boolean =
            other is UniqueEntry &&
                eventDefinitions == other.eventDefinitions &&
                ignoreCasing == other.ignoreCasing &&
                message == other.message &&
                scope == other.scope

        override fun hashCode(): Int {
            var result = eventDefinitions.hashCode()
            result = 31 * result + ignoreCasing.hashCode()
            result = 31 * result + message.hashCode()
            result = 31 * result + (scope?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String =
            "UniqueEntry(eventDefinitions=$eventDefinitions, ignoreCasing=$ignoreCasing, message=$message, scope=$scope)"
    }
}
