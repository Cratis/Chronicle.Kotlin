// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java.for_ReadModelsJavaBridge.when_observing_materialized_instances_as_a_publisher

import io.cratis.chronicle.java.ReadModelsJavaBridge
import io.cratis.chronicle.java.for_ReadModelsJavaBridge.given.AnObservation
import io.cratis.chronicle.java.for_ReadModelsJavaBridge.given.Book
import io.cratis.chronicle.java.for_ReadModelsJavaBridge.given.RecordingSubscriber
import io.mockk.every
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.flow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class AndTheObservationFails {
    @Test
    fun `should signal the failure to the subscriber`() {
        val observation = AnObservation()
        val failure = IllegalStateException("stream failed")
        every { observation.materialized.observeInstances(Book::class, 0, 50) } returns flow { throw failure }
        val subscriber = RecordingSubscriber<List<Book>>()

        ReadModelsJavaBridge.observeMaterializedInstancesPublisher(observation.service, Book::class.java, 0, 50)
            .subscribe(subscriber)
        subscriber.subscription.request(1)

        assertSame(failure, subscriber.failed.get(2, TimeUnit.SECONDS))
        assertFalse(subscriber.completed.isDone)
    }
}
