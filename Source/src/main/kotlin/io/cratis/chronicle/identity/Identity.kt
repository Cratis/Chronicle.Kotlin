// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.identity

/**
 * Represents an identity of something that is responsible for causing a state change.
 * An identity can be a user, a system, a service, or anything else that can be identified.
 *
 * @property subject The unique identifier (subject) of this identity.
 * @property name Human-readable name of the identity.
 * @property userName Optional username, defaults to empty string.
 * @property onBehalfOf Optional chain indicating who this identity acts on behalf of.
 */
data class Identity(
    val subject: String,
    val name: String,
    val userName: String = "",
    val onBehalfOf: Identity? = null
) {
    companion object {
        /** Identity used when identity is not set. */
        val notSet: Identity = Identity(
            subject = "1efc9b81-0612-4466-962c-86acc4e9a028",
            name = "[Not Set]",
            userName = "[Not Set]"
        )

        /** Identity used when the identity is not known. */
        val unknown: Identity = Identity(
            subject = "3321cf62-db16-425e-8173-99fcfefe11dd",
            name = "[Unknown]",
            userName = "[Unknown]"
        )

        /** Identity used when the system itself is the cause. */
        val system: Identity = Identity(
            subject = "5d032c92-9d5e-41eb-947a-ee5314ed0032",
            name = "[System]",
            userName = "[System]"
        )
    }

    /**
     * Returns a new [Identity] chain with duplicate subjects removed.
     * The first occurrence of each subject is kept.
     */
    fun withoutDuplicates(): Identity {
        val seen = linkedSetOf<String>()
        val chain = mutableListOf<Identity>()
        var current: Identity? = this
        while (current != null) {
            if (seen.add(current.subject)) {
                chain.add(current)
            }
            current = current.onBehalfOf
        }

        var result: Identity? = null
        for (i in chain.indices.reversed()) {
            val entry = chain[i]
            result = Identity(entry.subject, entry.name, entry.userName, result)
        }
        return result!!
    }
}
