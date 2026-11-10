// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.projections

/**
 * Where the key of a dictionary entry comes from when a projection writes into, or removes from, a
 * `Map<String, V>` property on a read model.
 *
 * The key is resolved per event by the kernel, so one projection keeps one entry per distinct key - a
 * counter per category, a total per currency, or a value per author.
 *
 * @property segment The dynamic key segment appended to the dictionary property path.
 */
class DictionaryKey private constructor(val segment: String) {
    override fun equals(other: Any?): Boolean = other is DictionaryKey && other.segment == segment

    override fun hashCode(): Int = segment.hashCode()

    override fun toString(): String = segment

    companion object {
        /**
         * Keys the entry by a property on the event content (`$eventContent`), for example `category`.
         * The kernel checks at registration that the path exists on the event; the match is case-sensitive.
         *
         * @param eventPropertyPath The property path on the event, dot-separated for nested properties.
         */
        @JvmStatic
        fun fromEvent(eventPropertyPath: String): DictionaryKey =
            DictionaryKey("\$eventContent.${requirePath(eventPropertyPath)}")

        /**
         * Keys the entry by a property on the event context (`$eventContext`), for example `eventType.id`.
         *
         * @param contextPropertyPath The property path on the event context, dot-separated for nested properties.
         */
        @JvmStatic
        fun fromContext(contextPropertyPath: String): DictionaryKey =
            DictionaryKey("\$eventContext.${requirePath(contextPropertyPath)}")

        /**
         * Keys the entry by an identity property of whoever caused the event (`$causedBy`).
         *
         * @param property Which identity property provides the key.
         */
        @JvmStatic
        fun fromCausedBy(property: CausedByProperty): DictionaryKey =
            DictionaryKey("\$causedBy.${property.expressionName}")

        private fun requirePath(path: String): String {
            require(path.isNotBlank()) { "A dictionary key needs a property path to read the key from." }
            return path
        }
    }
}
