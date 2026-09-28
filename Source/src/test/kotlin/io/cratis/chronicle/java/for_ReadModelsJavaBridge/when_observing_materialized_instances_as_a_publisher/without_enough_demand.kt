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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class WithoutEnoughDemand {
    @Test
    fun `should not deliver a second page until requested`() {
        val observation = AnObservation()
        val first = listOf(Book("Dune"))
        val second = listOf(Book("Foundation"))
        every { observation.materialized.observeInstances(Book::class, 0, 50) } returns flowOf(first, second)
        val subscriber = RecordingSubscriber<List<Book>>()

        ReadModelsJavaBridge.observeMaterializedInstancesPublisher(observation.service, Book::class.java, 0, 50)
            .subscribe(subscriber)
        assertNull(subscriber.values.poll(150, TimeUnit.MILLISECONDS))
        subscriber.subscription.request(1)
        assertEquals(first, subscriber.values.poll(2, TimeUnit.SECONDS))
        assertNull(subscriber.values.poll(150, TimeUnit.MILLISECONDS))
        subscriber.subscription.request(1)

        assertEquals(second, subscriber.values.poll(2, TimeUnit.SECONDS))
    }
}
