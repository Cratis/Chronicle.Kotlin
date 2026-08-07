// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring

import io.cratis.chronicle.IEventStore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.ApplicationListener
import java.time.Duration

/**
 * Holds the application back until Chronicle knows about every artifact it owns.
 *
 * Without this, the first request could reach a kernel that has not yet been told about the event types
 * it is being asked to append — a startup race that only shows up under load. Waiting here turns that
 * into a deterministic, visible part of startup.
 *
 * The wait is bounded: if the kernel cannot be reached in time the application still starts, logs why,
 * and keeps trying in the background, so a temporarily unavailable kernel degrades rather than blocks.
 *
 * @param eventStore The event store to register into.
 * @param timeout How long to wait for registration before starting anyway.
 */
class ChronicleStartup(
    private val eventStore: IEventStore,
    private val timeout: Duration = Duration.ofSeconds(30)
) : ApplicationListener<ApplicationReadyEvent> {
    private val logger = LoggerFactory.getLogger(ChronicleStartup::class.java)

    override fun onApplicationEvent(event: ApplicationReadyEvent) {
        val registered = runBlocking {
            withTimeoutOrNull(timeout.toMillis()) { eventStore.awaitRegistration() } != null
        }

        if (registered) {
            logger.info("Chronicle artifacts registered with event store '{}'", eventStore.name)
        } else {
            logger.warn(
                "Chronicle artifacts were not registered within {}. The application is starting anyway and " +
                    "registration will complete as soon as the kernel is reachable.",
                timeout
            )
        }
    }
}
