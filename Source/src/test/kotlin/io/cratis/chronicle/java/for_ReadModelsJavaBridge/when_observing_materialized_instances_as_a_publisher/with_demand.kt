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
import org.junit.jupiter.api.Test

class WithDemand {
    @Test
    fun `should deliver the requested page`() {
        val observation = AnObservation()
        val page = listOf(Book("Dune"))
        every { observation.materialized.observeInstances(Book::class, 10, 25) } returns flowOf(page)
        val subscriber = RecordingSubscriber<List<Book>>()

        ReadModelsJavaBridge.observeMaterializedInstancesPublisher(observation.service, Book::class.java, 10, 25)
            .subscribe(subscriber)
        subscriber.subscription.request(1)

        assertEquals(page, subscriber.values.poll(2, TimeUnit.SECONDS))
    }
}
