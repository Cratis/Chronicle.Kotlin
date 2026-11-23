// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import Cratis.Chronicle.Contracts.Clients.Clients
import java.util.concurrent.atomic.AtomicInteger

/**
 * Says, when the kernel reports that it is busy and again when it stops, what the keep-alive carried - rather than
 * leaving the client's users to guess from timeouts.
 *
 * Only a change is reported, so a kernel that stays busy is one line, not one per keep-alive. A kernel that sends no
 * status is accepting, and a status this client does not know is reported by its number.
 *
 * @param report Where the report goes.
 */
internal class KernelStatusReport(private val report: (String) -> Unit = { System.err.println("[Chronicle] $it") }) {
    private val last = AtomicInteger(ACCEPTING)

    /** Takes the status the latest keep-alive carried. */
    fun observe(keepAlive: Clients.ConnectionKeepAlive) {
        val status = keepAlive.statusValue
        if (last.getAndSet(status) == status) return

        if (status == ACCEPTING) {
            report("The kernel is accepting calls normally again")
        } else {
            val name = Clients.KernelStatus.forNumber(status)?.name ?: "status $status"
            report("The kernel reports that it is busy ($name); calls may be slow or be asked to retry until it is accepting again")
        }
    }

    private companion object {
        const val ACCEPTING = 0
    }
}
