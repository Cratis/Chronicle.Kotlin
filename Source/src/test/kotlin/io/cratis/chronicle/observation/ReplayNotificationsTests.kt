// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.eventSequences.EventSequenceNumber
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A reactor can be told a replay is starting and finishing, so it can do the things that belong
 * around a replay rather than inside a handler. What has to hold: both methods are optional but not
 * both absent, either may suspend, and the context is optional.
 */
class ReplayNotificationsTests {

    private val context = ReplayContext("EmployeeAlerts", "employee-1", EventSequenceNumber(7))

    @Reactor
    class BothNotifications : ICanBeNotifiedAboutReplay {
        val seen = mutableListOf<String>()
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
        fun replayBegan(context: ReplayContext) = seen.add("began:${context.partition}").let { }
        suspend fun replayEnded(context: ReplayContext) {
            delay(1)
            seen.add("ended:${context.sequenceNumber.value}")
        }
    }

    @Reactor
    class BeginOnly : ICanBeNotifiedAboutReplay {
        var began = false
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
        fun replayBegan() {
            began = true
        }
    }

    @Reactor
    class NotInterested {
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
    }

    @Reactor
    class MarkedButSilent : ICanBeNotifiedAboutReplay {
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
    }

    @Reactor
    class FailingNotification : ICanBeNotifiedAboutReplay {
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
        fun replayBegan(@Suppress("UNUSED_PARAMETER") context: ReplayContext): Unit =
            throw IllegalStateException("cache is gone")
    }

    @Reactor
    class WrongNotificationParameter : ICanBeNotifiedAboutReplay {
        fun bookReturned(@Suppress("UNUSED_PARAMETER") event: BookReturned) = Unit
        fun replayBegan(@Suppress("UNUSED_PARAMETER") reason: String) = Unit
    }

    @Test
    fun `a reactor that did not ask has nothing to notify`() {
        assertTrue(ReplayNotifications.from(NotInterested::class).isEmpty)
    }

    @Test
    fun `both notifications are found and called`() = runBlocking {
        val notifications = ReplayNotifications.from(BothNotifications::class)
        val reactor = BothNotifications()

        notifications.notifyBegan(reactor, context)
        notifications.notifyEnded(reactor, context)

        assertEquals(listOf("began:employee-1", "ended:7"), reactor.seen)
    }

    @Test
    fun `a notification that wants no context is called without one`() = runBlocking {
        val notifications = ReplayNotifications.from(BeginOnly::class)
        val reactor = BeginOnly()

        notifications.notifyBegan(reactor, context)
        // Declaring only one is fine - the other simply never fires.
        notifications.notifyEnded(reactor, context)

        assertTrue(reactor.began)
    }

    @Test
    fun `asking to be notified and declaring nothing is rejected`() {
        val error = assertThrows(ObserverHasNoHandlers::class.java) {
            ReplayNotifications.from(MarkedButSilent::class)
        }
        assertTrue(error.message!!.contains("replayBegan"))
        assertTrue(error.message!!.contains("replayEnded"))
    }

    @Test
    fun `a notification with a parameter that is not a replay context is rejected`() {
        val error = assertThrows(InvalidHandlerSignature::class.java) {
            ReplayNotifications.from(WrongNotificationParameter::class)
        }
        assertTrue(error.message!!.contains("ReplayContext"))
    }

    @Test
    fun `a notification that throws is reported as a failure rather than escaping`() = runBlocking {
        val outcome = ReactorObservationOutcome()
        val notifications = ReplayNotifications.from(FailingNotification::class)

        // Mirrors what the dispatch does: the throw is recorded, not propagated, so the kernel is
        // told the partition failed instead of the whole observation being torn down.
        try {
            notifications.notifyBegan(FailingNotification(), context)
        } catch (e: Exception) {
            outcome.failed(e, "replay notification")
        }

        assertFalse(outcome.isSuccess)
        assertTrue(outcome.exceptions.single().contains("cache is gone"), outcome.exceptions.toString())
    }

    @Test
    fun `registration carries the notifications it found`() {
        val registration = ReactorRegistration.from(BothNotifications::class)
        assertTrue(!registration.replayNotifications.isEmpty)
    }
}
