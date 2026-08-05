// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.identities

interface IIdentityManagerService {
    /**
     * Renames the identity with the given [subject] to [name].
     *
     * @param subject The unique identifier (subject) of the identity to rename.
     * @param name The new human-readable name for the identity.
     */
    suspend fun rename(subject: String, name: String)
}
