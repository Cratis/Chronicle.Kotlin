// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring.namespaces

import io.cratis.chronicle.EventStoreNamespaceName
import io.cratis.chronicle.namespaces.IEventStoreNamespaceResolver
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * Takes the namespace from a header on the current HTTP request — the usual choice when an API gateway
 * or a front-end already knows which tenant a call belongs to.
 *
 * Work happening outside a request, such as a scheduled job or a reactor reacting to an event, falls
 * back to [EventStoreNamespaceName.default].
 *
 * @param headerName The header carrying the namespace.
 */
class HttpHeaderNamespaceResolver(private val headerName: String) : IEventStoreNamespaceResolver {
    override fun resolve(): String =
        currentRequest()?.getHeader(headerName)?.takeIf { it.isNotBlank() }
            ?: EventStoreNamespaceName.default.value
}

/** The HTTP request being handled on this thread, or `null` when the work is not part of one. */
internal fun currentRequest(): jakarta.servlet.http.HttpServletRequest? =
    (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request
