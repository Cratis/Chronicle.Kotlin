// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java.for_ReadModelsJavaBridge.when_observing_materialized_instances_as_a_publisher

import io.cratis.chronicle.java.ReadModelsJavaBridge
import io.cratis.chronicle.java.for_ReadModelsJavaBridge.given.AnObservation
import io.cratis.chronicle.java.for_ReadModelsJavaBridge.given.Book
import io.cratis.chronicle.java.for_ReadModelsJavaBridge.given.RecordingSubscriber
import io.mockk.every
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class AndTheSubscriptionIsCancelled {
    @Test
    fun `should stop collecting the underlying observation`() {
        val observation = AnObservation()
        val cancelled = CompletableFuture<Unit>()
        val page = listOf(Book("Dune"))
        every { observation.materialized.observeInstances(Book::class, 0, 50) } returns flow {
            try {
                emit(page)
                awaitCancellation()
            } finally {
                cancelled.complete(Unit)
            }
        }
        val subscriber = RecordingSubscriber<List<Book>>()

        ReadModelsJavaBridge.observeMaterializedInstancesPublisher(observation.service, Book::class.java, 0, 50)
            .subscribe(subscriber)
        subscriber.subscription.request(1)
        assertEquals(page, subscriber.values.poll(2, TimeUnit.SECONDS))
        subscriber.subscription.cancel()

        cancelled.get(2, TimeUnit.SECONDS)
        assertFalse(subscriber.completed.isDone)
        assertFalse(subscriber.failed.isDone)
    }
}
