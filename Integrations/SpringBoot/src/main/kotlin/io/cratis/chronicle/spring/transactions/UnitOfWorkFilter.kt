// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring.transactions

import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.correlation.CorrelationId
import io.cratis.chronicle.correlation.correlationIdManager
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.runBlocking
import org.springframework.core.Ordered
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Runs each HTTP request inside a unit of work, committing it when the request completes.
 *
 * A handler can then append several events across several aggregates and have them land together or
 * not at all, without opening and committing a transaction by hand. A request that throws rolls the
 * unit of work back on the way out.
 *
 * @param eventStore The event store whose unit of work manager the request runs against.
 */
class UnitOfWorkFilter(private val eventStore: IEventStore) : OncePerRequestFilter(), Ordered {
    override fun getOrder(): Int = ORDER

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val correlationId = CorrelationId(correlationIdManager.current)
        val unitOfWork = eventStore.unitOfWorkManager.begin(correlationId)

        try {
            filterChain.doFilter(request, response)
            // A handler that committed - or rolled back - on its own has said what it wanted; committing
            // again here would be a second, empty transaction.
            if (!unitOfWork.isCompleted) {
                runBlocking { unitOfWork.commit() }
            }
        } catch (throwable: Throwable) {
            if (!unitOfWork.isCompleted) {
                runBlocking { unitOfWork.rollback() }
            }
            throw throwable
        } finally {
            correlationIdManager.clear()
        }
    }

    private companion object {
        /** Inside the identity and causation filters, so both are in place before any event is staged. */
        const val ORDER = Ordered.LOWEST_PRECEDENCE - 10
    }
}
