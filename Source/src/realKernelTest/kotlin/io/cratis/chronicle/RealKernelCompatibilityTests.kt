// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle

import io.cratis.chronicle.artifacts.KnownClientArtifacts
import io.cratis.chronicle.auditing.Causation
import io.cratis.chronicle.auditing.CausationType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.EventForEventSourceId
import io.cratis.chronicle.eventSequences.EventSequenceNumber
import io.cratis.chronicle.eventSequences.concurrency.ConcurrencyScope
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.identity.Identity
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.projections.IProjectionFor
import io.cratis.chronicle.readModels.ReadModel
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

@EventType(id = "ChronicleKotlinRealKernelCompatibilityEvent")
private data class RealKernelEvent(val marker: String = "")

@ReadModel
private data class RealKernelReadModel(
    val id: String = "",
    val marker: String = ""
)

private class RealKernelProjection : IProjectionFor<RealKernelReadModel> {
    override fun define(builder: IProjectionBuilderFor<RealKernelReadModel>) {
        builder.from(RealKernelEvent::class) { from ->
            from.usingKey("marker")
                .set(RealKernelReadModel::marker).toProperty("marker")
        }
    }
}

class RealKernelCompatibilityTests {
    @Test
    fun `verified kernel preserves explicit context atomic batches and concurrency contracts`() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(8)
        val storeName = "Kt$suffix"
        val firstNamespace = "n1$suffix"
        val secondNamespace = "n2$suffix"
        val client = ChronicleClient(
            ChronicleOptions.fromConnectionString(
                "chronicle://${io.cratis.chronicle.connection.ChronicleConnectionString.DEVELOPMENT_CLIENT}:" +
                    "${io.cratis.chronicle.connection.ChronicleConnectionString.DEVELOPMENT_CLIENT_SECRET}@" +
                    "${kernel.host}:${kernel.getMappedPort(KERNEL_TLS_PORT)}"
            ).copy(
                defaultSinkTypeId = io.cratis.chronicle.sinks.WellKnownSinkTypes.IN_MEMORY,
                autoDiscoverAndRegister = false,
                artifacts = KnownClientArtifacts(
                    RealKernelEvent::class,
                    RealKernelReadModel::class,
                    RealKernelProjection::class
                )
            )
        )

        try {
            val firstStore = client.getEventStore(storeName, firstNamespace)
            val secondStore = client.getEventStore(storeName, secondNamespace)
            firstStore.registerAll()
            verifyProjection(firstStore, "projection-$firstNamespace", "projected-$firstNamespace")

            secondStore.registerAll()
            verifyProjection(secondStore, "projection-$secondNamespace", "projected-$secondNamespace")

            verifyExplicitMetadata(firstStore, storeName, firstNamespace, suffix)
            verifyExplicitMetadata(secondStore, storeName, secondNamespace, suffix)
            verifyOrderedAtomicBatch(firstStore, suffix)
            verifyStaleBatchIsAtomicAndRetainsEveryViolation(firstStore, suffix)
            verifyExpectsNoMatchingEvent(firstStore, suffix)
        } finally {
            client.dispose()
        }
    }

    private suspend fun verifyExplicitMetadata(
        store: IEventStore,
        storeName: String,
        namespace: String,
        suffix: String
    ) {
        val correlationId = UUID.randomUUID()
        val causation = Causation(
            Instant.parse("2026-01-02T03:04:05Z"),
            CausationType("RealKernelCompatibility"),
            mapOf("gate" to "16.44.1")
        )
        val identity = Identity("subject-$suffix", "Compatibility User", "compatibility-user")
        val context = OperationContext(correlationId, listOf(causation), identity)
        val sourceId = "metadata-$namespace"
        val tag = "tag-$namespace"

        val append = store.eventLog.append(
            sourceId,
            RealKernelEvent("metadata-$namespace"),
            context,
            AppendOptions(tags = listOf(tag))
        )
        assertTrue(append.isSuccess)

        val read = store.eventLog.getFromSequenceNumber(
            append.sequenceNumber,
            eventSourceId = sourceId,
            eventTypes = listOf(RealKernelEvent::class),
            tags = listOf(tag)
        ).single()

        assertEquals(correlationId, read.context.correlationId)
        assertEquals(listOf(causation), read.context.causation)
        assertEquals(identity, read.context.causedBy)
        assertEquals(namespace, read.context.namespace)
        assertEquals(storeName, read.context.eventStore)
        assertEquals(listOf(tag), read.context.tags)
    }

    private suspend fun verifyProjection(store: IEventStore, sourceId: String, marker: String) {
        val append = store.eventLog.append(sourceId, RealKernelEvent(marker), OperationContext.system())
        assertTrue(append.isSuccess)

        val projected = try {
            withTimeout(30_000) {
                var instance: RealKernelReadModel?
                do {
                    instance = store.readModels.getInstanceByKey(RealKernelReadModel::class, marker)
                    if (instance == null) delay(250)
                } while (instance == null)
                instance
            }
        } catch (throwable: kotlinx.coroutines.TimeoutCancellationException) {
            val failures = store.failedPartitions.getFor("RealKernelProjection")
            error("Projection did not materialize; failed partitions: $failures")
        }

        assertEquals(marker, projected.marker)
    }

    private suspend fun verifyOrderedAtomicBatch(store: IEventStore, suffix: String) {
        val firstSource = "ordered-a-$suffix"
        val secondSource = "ordered-b-$suffix"
        val tailBefore = store.eventLog.getTailSequenceNumber().value
        val events = listOf(
            EventForEventSourceId(firstSource, RealKernelEvent("ordered-1")),
            EventForEventSourceId(secondSource, RealKernelEvent("ordered-2")),
            EventForEventSourceId(firstSource, RealKernelEvent("ordered-3"))
        )

        val results = store.eventLog.appendMany(events, OperationContext.system())

        assertEquals(3, results.size)
        assertTrue(results.all { it.isSuccess })
        val appended = store.eventLog.getFromSequenceNumber(EventSequenceNumber(tailBefore + 1))
            .filter { it.context.eventSourceId == firstSource || it.context.eventSourceId == secondSource }
        assertEquals(3, appended.size)
        listOf("ordered-1", "ordered-2", "ordered-3").zip(appended).forEach { (marker, event) ->
            assertTrue(event.content.contains("\"marker\":\"$marker\""))
        }
    }

    private suspend fun verifyStaleBatchIsAtomicAndRetainsEveryViolation(store: IEventStore, suffix: String) {
        val firstSource = "stale-a-$suffix"
        val secondSource = "stale-b-$suffix"
        val initial = store.eventLog.appendMany(
            listOf(
                EventForEventSourceId(firstSource, RealKernelEvent("stale-initial-a")),
                EventForEventSourceId(secondSource, RealKernelEvent("stale-initial-b"))
            ),
            OperationContext.system()
        )
        assertTrue(initial.all { it.isSuccess })
        val exactButSoonStaleScopes = mapOf(
            firstSource to ConcurrencyScope(initial[0].sequenceNumber, eventSourceId = true),
            secondSource to ConcurrencyScope(initial[1].sequenceNumber, eventSourceId = true)
        )

        val advances = store.eventLog.appendMany(
            listOf(
                EventForEventSourceId(firstSource, RealKernelEvent("stale-advance-a")),
                EventForEventSourceId(secondSource, RealKernelEvent("stale-advance-b"))
            ),
            OperationContext.system()
        )
        assertTrue(advances.all { it.isSuccess })
        val tailBeforeRejectedBatch = store.eventLog.getTailSequenceNumber()

        val rejected = store.eventLog.appendMany(
            listOf(
                EventForEventSourceId(firstSource, RealKernelEvent("must-not-commit-a")),
                EventForEventSourceId(secondSource, RealKernelEvent("must-not-commit-b"))
            ),
            OperationContext.system(),
            exactButSoonStaleScopes
        )

        assertTrue(rejected.all { !it.isSuccess })
        assertTrue(rejected.all { it.concurrencyCheckPerformed })
        rejected.forEach { result ->
            assertEquals(2, result.concurrencyViolations.size)
            assertEquals(
                initial.map { it.sequenceNumber.value }.toSet(),
                result.concurrencyViolations.map { it.expectedSequenceNumber.value }.toSet()
            )
            assertEquals(
                advances.map { it.sequenceNumber.value }.toSet(),
                result.concurrencyViolations.map { it.actualSequenceNumber.value }.toSet()
            )
        }
        assertEquals(tailBeforeRejectedBatch, store.eventLog.getTailSequenceNumber())
        val afterRejected = store.eventLog.getFromSequenceNumber(EventSequenceNumber(tailBeforeRejectedBatch.value + 1))
        assertFalse(afterRejected.any { it.content.contains("must-not-commit") })
    }

    private suspend fun verifyExpectsNoMatchingEvent(store: IEventStore, suffix: String) {
        val source = "expects-none-$suffix"
        val scope = ConcurrencyScope.noMatchingEvent.copy(eventSourceId = true)

        val first = store.eventLog.append(
            source,
            RealKernelEvent("expects-none-first"),
            OperationContext.system(),
            AppendOptions(concurrencyScope = scope)
        )
        assertTrue(first.isSuccess)
        assertTrue(first.concurrencyCheckPerformed)

        val tailBeforeRejected = store.eventLog.getTailSequenceNumber()
        val rejected = store.eventLog.append(
            source,
            RealKernelEvent("expects-none-rejected"),
            OperationContext.system(),
            AppendOptions(concurrencyScope = scope)
        )
        assertFalse(rejected.isSuccess)
        assertTrue(rejected.concurrencyCheckPerformed)
        val violation = rejected.concurrencyViolations.single()
        assertEquals(EventSequenceNumber.unavailable, violation.expectedSequenceNumber)
        assertEquals(first.sequenceNumber, violation.actualSequenceNumber)
        assertEquals(tailBeforeRejected, store.eventLog.getTailSequenceNumber())
    }

    companion object {
        private const val KERNEL_TLS_PORT = 35000

        private lateinit var kernel: GenericContainer<*>

        @JvmStatic
        @BeforeAll
        fun startKernel() {
            val image = checkNotNull(System.getProperty("chronicle.realKernel.image")) {
                "The realKernelTest Gradle task must supply chronicle.realKernel.image"
            }
            try {
                kernel = GenericContainer<Nothing>(DockerImageName.parse(image)).apply {
                    withExposedPorts(KERNEL_TLS_PORT)
                    waitingFor(
                        Wait.forHttp("/health")
                            .forPort(KERNEL_TLS_PORT)
                            .usingTls()
                            .allowInsecure()
                            .forStatusCode(200)
                            .withStartupTimeout(Duration.ofMinutes(2))
                    )
                }
                kernel.start()
            } catch (throwable: Throwable) {
                throw IllegalStateException(
                    "realKernelTest requires an available Docker daemon and a runnable Chronicle image '$image'",
                    throwable
                )
            }
        }

        @JvmStatic
        @AfterAll
        fun stopKernel() {
            if (::kernel.isInitialized) kernel.stop()
        }
    }
}
