// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java.for_ReadModelsJavaBridge.when_observing_materialized_instances_as_a_publisher

import io.cratis.chronicle.java.ReadModelsJavaBridge
import io.cratis.chronicle.java.for_ReadModelsJavaBridge.given.AnObservation
import io.cratis.chronicle.java.for_ReadModelsJavaBridge.given.Book
import io.cratis.chronicle.java.for_ReadModelsJavaBridge.given.RecordingSubscriber
import io.mockk.every
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.flowOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class AndTheObservationCompletes {
    @Test
    fun `should complete after the requested pages`() {
        val observation = AnObservation()
        every { observation.materialized.observeInstances(Book::class, 0, 50) } returns flowOf(listOf(Book("Dune")))
        val subscriber = RecordingSubscriber<List<Book>>()

        ReadModelsJavaBridge.observeMaterializedInstancesPublisher(observation.service, Book::class.java, 0, 50)
            .subscribe(subscriber)
        subscriber.subscription.request(1)

        subscriber.completed.get(2, TimeUnit.SECONDS)
        assertEquals(1, subscriber.values.size)
        assertFalse(subscriber.failed.isDone)
    }
}
