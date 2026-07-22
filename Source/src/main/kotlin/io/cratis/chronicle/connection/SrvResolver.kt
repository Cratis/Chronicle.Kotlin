// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xbill.DNS.Lookup
import org.xbill.DNS.Record
import org.xbill.DNS.SRVRecord
import org.xbill.DNS.SimpleResolver
import org.xbill.DNS.Type

/**
 * Resolves a `chronicle+srv://` host into concrete server addresses via DNS SRV records.
 *
 * Queries `_chronicle._tcp.<host>`. Records are sorted ascending by priority, then descending by
 * weight, so the first address in the returned list is always the most preferred target.
 *
 * @param lookup Performs the raw SRV query for a fully-qualified query name and an optional name
 *   server override, returning the raw DNS records (or null/empty when none are found). Defaults
 *   to a real lookup via dnsjava's [Lookup]. Tests substitute a fake here instead of mocking
 *   dnsjava's `final` [Lookup] class directly, so no real network access or mocking-framework
 *   support for final classes is required to exercise the sorting/mapping/error-handling logic.
 */
class SrvResolver(
    private val lookup: (query: String, nameServer: String?) -> Array<Record>? = ::performLookup
) {
    /**
     * Resolves [host] into the server addresses published for it via DNS SRV records.
     *
     * @param host The host to resolve, e.g. the host component of a `chronicle+srv://` connection string.
     * @param nameServer An optional DNS server (`host` or `host:port`, default port 53) to query
     *   instead of the system's configured resolver.
     * @throws ChronicleSrvResolutionException when the lookup returns no SRV records.
     */
    suspend fun resolve(host: String, nameServer: String? = null): List<ChronicleServerAddress> =
        withContext(Dispatchers.IO) {
            val records = lookup("$SRV_QUERY_PREFIX$host", nameServer)
                .orEmpty()
                .filterIsInstance<SRVRecord>()

            if (records.isEmpty()) {
                throw ChronicleSrvResolutionException(host)
            }

            records
                .sortedWith(compareBy<SRVRecord> { it.priority }.thenByDescending { it.weight })
                .map { ChronicleServerAddress(it.target.toString(true), it.port) }
        }

    companion object {
        private const val SRV_QUERY_PREFIX = "_chronicle._tcp."

        private fun performLookup(query: String, nameServer: String?): Array<Record>? {
            val lookup = Lookup(query, Type.SRV)
            if (nameServer != null) {
                lookup.setResolver(resolverFor(nameServer))
            }
            return lookup.run()
        }

        private fun resolverFor(nameServer: String): SimpleResolver {
            val colonIndex = nameServer.indexOf(':')
            if (colonIndex < 0) return SimpleResolver(nameServer)
            return SimpleResolver(nameServer.substring(0, colonIndex)).apply {
                port = nameServer.substring(colonIndex + 1).toInt()
            }
        }
    }
}
