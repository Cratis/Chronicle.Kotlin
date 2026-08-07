// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring.auditing

import io.cratis.chronicle.auditing.CausationType
import io.cratis.chronicle.auditing.causationManager
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Records the HTTP request that led to an event, so the causation chain on every event appended while
 * handling it says which call it came from.
 *
 * Months later, "why does this record say what it says?" is answered by the event's own metadata rather
 * than by correlating log files.
 */
class CausationFilter : OncePerRequestFilter(), Ordered {
    override fun getOrder(): Int = ORDER

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        causationManager.defineRoot(
            buildMap {
                put(ROUTE, request.requestURI)
                put(METHOD, request.method)
                put(HOST, request.serverName)
                put(PROTOCOL, request.protocol)
                put(SCHEME, request.scheme)
                request.queryString?.let { put(QUERY, it) }
                request.getHeader("Origin")?.let { put(ORIGIN, it) }
                request.getHeader("Referer")?.let { put(REFERER, it) }
            }
        )
        causationManager.add(CAUSATION_TYPE, mapOf(ROUTE to request.requestURI, METHOD to request.method))

        try {
            filterChain.doFilter(request, response)
        } finally {
            causationManager.clear()
        }
    }

    companion object {
        /** The causation type recorded for events appended while handling an HTTP request. */
        val CAUSATION_TYPE: CausationType = CausationType("Spring Request")

        private const val ORDER = Ordered.LOWEST_PRECEDENCE - 20

        private const val ROUTE = "route"
        private const val METHOD = "method"
        private const val HOST = "host"
        private const val PROTOCOL = "protocol"
        private const val SCHEME = "scheme"
        private const val QUERY = "query"
        private const val ORIGIN = "origin"
        private const val REFERER = "referer"
    }
}
