// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.namespaces

/**
 * Decides which namespace within an event store the current piece of work belongs to.
 *
 * A namespace is Chronicle's tenancy boundary: the same event store, the same artifacts, entirely
 * separate streams of events. Rather than threading a namespace through every call, the host resolves
 * it from whatever it considers the tenant — an HTTP header, a subdomain, a claim on the current
 * principal — and the client picks up the right event store from there.
 */
fun interface IEventStoreNamespaceResolver {
    /**
     * Resolves the namespace for the current piece of work.
     *
     * @return The namespace name.
     */
    fun resolve(): String
}
