// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import Cratis.Chronicle.Contracts.Compliance.ComplianceGrpcKt
import Cratis.Chronicle.Contracts.Projections.ProjectionsGrpcKt
import Cratis.Chronicle.Contracts.ReadModelExplorer.ReadModelExplorerGrpcKt
import Cratis.Chronicle.Contracts.ReadModels.MaterializedReadModelsGrpcKt
import Cratis.Chronicle.Contracts.ReadModels.ReadModelsGrpcKt
import Cratis.Chronicle.Contracts.ReadModels.Readmodels
import com.google.protobuf.Empty
import io.cratis.chronicle.ChronicleOptions
import io.cratis.chronicle.connection.ChronicleConnectionString
import io.cratis.chronicle.connection.ConnectionLifecycle
import io.cratis.chronicle.observation.ReducersService
import io.cratis.chronicle.projections.FromEvent
import io.cratis.chronicle.projections.ProjectionsService
import io.cratis.chronicle.sinks.WellKnownSinkTypes
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

@io.cratis.chronicle.events.EventType
private data class NamingPolicyPersonRegistered(val name: String)

private data class NamingPolicyPerson(val name: String = "")

@ReadModel(id = "explicit-person")
private data class NamingPolicyExplicitPerson(val name: String = "")

@ReadModel(id = "explicit-projected-person")
@FromEvent(NamingPolicyPersonRegistered::class)
private data class NamingPolicyProjectedPerson(val name: String = "")

@io.cratis.chronicle.observation.Reducer(isActive = false)
private class NamingPolicyPersonReducer {
    fun registered(event: NamingPolicyPersonRegistered) = NamingPolicyExplicitPerson(event.name)
}

private val pluralize = ReadModelNamingPolicy { "${it.simpleName}s" }

class ReadModelNamingPolicyTests {

    private val registrations = mutableListOf<Readmodels.RegisterManyRequest>()
    private val stub = mockk<ReadModelsGrpcKt.ReadModelsCoroutineStub>().also {
        coEvery { it.registerMany(capture(registrations), any()) } returns Empty.getDefaultInstance()
    }

    private fun service(policy: ReadModelNamingPolicy? = null): ReadModelsService {
        val materialized = mockk<MaterializedReadModelsGrpcKt.MaterializedReadModelsCoroutineStub>()
        val explorer = mockk<ReadModelExplorerGrpcKt.ReadModelExplorerCoroutineStub>()
        val compliance = mockk<ComplianceGrpcKt.ComplianceCoroutineStub>()
        return if (policy == null) {
            ReadModelsService("my-store", "default", stub, materialized, explorer, compliance)
        } else {
            ReadModelsService("my-store", "default", stub, materialized, explorer, compliance, WellKnownSinkTypes.MONGODB, policy)
        }
    }

    private fun registered(): List<Readmodels.ReadModelDefinition> = registrations.flatMap { it.readModelsList }

    @Test
    fun `without a policy the container is named after the identifier`() = runBlocking {
        service().register(NamingPolicyPerson::class, NamingPolicyExplicitPerson::class)

        val definitions = registered()
        assertEquals(listOf("NamingPolicyPerson", "explicit-person"), definitions.map { it.type.identifier })
        assertEquals(listOf("NamingPolicyPerson", "explicit-person"), definitions.map { it.containerName })
    }

    @Test
    fun `the default policy returns the read model identifier`() {
        assertEquals("NamingPolicyPerson", DefaultReadModelNamingPolicy.getReadModelName(NamingPolicyPerson::class.java))
        assertEquals("explicit-person", DefaultReadModelNamingPolicy.getReadModelName(NamingPolicyExplicitPerson::class.java))
    }

    @Test
    fun `a custom policy changes the container name and leaves the identifier alone`() = runBlocking {
        service(pluralize).register(NamingPolicyPerson::class)

        val definition = registered().single()
        assertEquals("NamingPolicyPerson", definition.type.identifier)
        assertEquals("NamingPolicyPersons", definition.containerName)
    }

    @Test
    fun `a custom policy applies to a read model registered by a reducer`() = runBlocking {
        val service = service(pluralize)

        ReducersService("my-store", "default", ConnectionLifecycle(), mockk(), readModels = service)
            .register(NamingPolicyPersonReducer())
            .cancel()

        val definition = registered().single()
        assertEquals("explicit-person", definition.type.identifier)
        assertEquals("NamingPolicyExplicitPersons", definition.containerName)
        assertEquals(1, definition.observerTypeValue)
        assertEquals("NamingPolicyPersonReducer", definition.observerIdentifier)
    }

    @Test
    fun `a custom policy applies to a read model registered by a projection`() = runBlocking {
        val projections = mockk<ProjectionsGrpcKt.ProjectionsCoroutineStub>()
        coEvery { projections.register(any(), any()) } returns Empty.getDefaultInstance()

        ProjectionsService("my-store", projections, service(pluralize), "default")
            .register(NamingPolicyProjectedPerson::class)

        val definition = registered().single()
        assertEquals("explicit-projected-person", definition.type.identifier)
        assertEquals("NamingPolicyProjectedPersons", definition.containerName)
        assertEquals(2, definition.observerTypeValue)
    }

    @Test
    fun `options carry the default policy until one is set`() {
        val options = ChronicleOptions(ChronicleConnectionString.DEVELOPMENT)

        assertSame(DefaultReadModelNamingPolicy, options.readModelNamingPolicy)
        assertSame(pluralize, options.withReadModelNamingPolicy(pluralize).readModelNamingPolicy)
    }

    @Test
    fun `setting a policy leaves the original options alone`() {
        val options = ChronicleOptions(ChronicleConnectionString.DEVELOPMENT)

        options.withReadModelNamingPolicy(pluralize)

        assertSame(DefaultReadModelNamingPolicy, options.readModelNamingPolicy)
    }

    @Test
    fun `the policy survives the other with methods`() {
        val options = ChronicleOptions(ChronicleConnectionString.DEVELOPMENT).withReadModelNamingPolicy(pluralize)

        assertSame(pluralize, options.withoutAutoRegistration().readModelNamingPolicy)
        assertSame(pluralize, options.withArtifactsFrom("com.example").readModelNamingPolicy)
    }

    @Test
    fun `options with different policies are not equal`() {
        val options = ChronicleOptions(ChronicleConnectionString.DEVELOPMENT)

        assertEquals(options, options.copy())
        assertEquals(options.hashCode(), options.copy().hashCode())
        assertNotEquals(options, options.withReadModelNamingPolicy(pluralize))
    }
}
