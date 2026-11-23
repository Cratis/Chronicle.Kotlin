// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.connection

import Cratis.Chronicle.Contracts.Clients.Clients
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** What the client says when the kernel reports, on its keep-alive, that it is busy. */
class KernelStatusReportTests {
    private val reports = mutableListOf<String>()
    private val report = KernelStatusReport { reports += it }

    private fun keepAlive(status: Clients.KernelStatus) =
        Clients.ConnectionKeepAlive.newBuilder().setConnectionId("c").setStatus(status).build()

    @Test
    fun `a kernel that is accepting is not reported`() {
        report.observe(keepAlive(Clients.KernelStatus.Accepting))
        report.observe(Clients.ConnectionKeepAlive.getDefaultInstance())

        assertEquals(emptyList<String>(), reports)
    }

    @Test
    fun `a kernel that becomes busy is reported once, however many keep-alives say so`() {
        report.observe(keepAlive(Clients.KernelStatus.Recovering))
        report.observe(keepAlive(Clients.KernelStatus.Recovering))
        report.observe(keepAlive(Clients.KernelStatus.Recovering))

        assertEquals(1, reports.size)
        assertTrue(reports.single().contains("Recovering"))
    }

    @Test
    fun `a change from one busy status to another is reported`() {
        report.observe(keepAlive(Clients.KernelStatus.Degraded))
        report.observe(keepAlive(Clients.KernelStatus.Recovering))

        assertEquals(2, reports.size)
        assertTrue(reports.last().contains("Recovering"))
    }

    @Test
    fun `a kernel that is accepting again is reported`() {
        report.observe(keepAlive(Clients.KernelStatus.Degraded))
        report.observe(keepAlive(Clients.KernelStatus.Accepting))

        assertEquals(2, reports.size)
        assertTrue(reports.last().contains("accepting"))
    }

    @Test
    fun `a status this client does not know does not crash and is reported by number`() {
        report.observe(Clients.ConnectionKeepAlive.newBuilder().setStatusValue(42).build())

        assertEquals(1, reports.size)
        assertTrue(reports.single().contains("status 42"))
    }
}
