// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import Cratis.Chronicle.Contracts.Events.Events
import Cratis.Chronicle.Contracts.Jobs.JobsOuterClass
import Cratis.Chronicle.Contracts.Observation.EventStoreSubscriptions.ObservationEventstoresubscriptions
import Cratis.Chronicle.Contracts.Observation.Webhooks.ObservationWebhooks
import io.cratis.chronicle.auditing.CausationManager
import io.cratis.chronicle.auditing.CausationType
import io.cratis.chronicle.compliance.IComplianceService
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.eventSequences.ITransactionalEventSequence
import io.cratis.chronicle.eventStoreSubscriptions.IEventStoreSubscriptionsService
import io.cratis.chronicle.eventStoreSubscriptions.IEventStoreSubscriptionBuilder
import io.cratis.chronicle.externalServices.IExternalServicesService
import io.cratis.chronicle.externalServices.IExternalServiceBuilder
import io.cratis.chronicle.identities.IIdentityManagerService
import io.cratis.chronicle.jobs.IJobsService
import io.cratis.chronicle.namespaces.INamespacesService
import io.cratis.chronicle.observation.IReactorsService
import io.cratis.chronicle.observation.IReducersService
import io.cratis.chronicle.projections.IProjectionsService
import io.cratis.chronicle.readModels.IReadModelsService
import io.cratis.chronicle.readModels.ReadModelSnapshot
import io.cratis.chronicle.constraints.IConstraintBuilder
import io.cratis.chronicle.constraints.IConstraintsService
import io.cratis.chronicle.constraints.IUniqueConstraintBuilder
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.events.IEventTypesService
import io.cratis.chronicle.seeding.IEventSeedingBuilder
import io.cratis.chronicle.seeding.IEventSeedingScopeBuilder
import io.cratis.chronicle.seeding.IEventSeedingService
import io.cratis.chronicle.transactions.UnitOfWork
import io.cratis.chronicle.webhooks.IWebhookDefinitionBuilder
import io.cratis.chronicle.webhooks.IWebhooksService
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking

/**
 * Java-friendly bridge for IEventLog operations.
 * Provides blocking versions of suspend functions for Java interop.
 */
object EventLogJavaBridge {
    @JvmStatic
    fun append(eventLog: IEventLog, eventSourceId: String, event: Any, options: AppendOptions?): AppendResult =
        runBlocking { eventLog.append(eventSourceId, event, options) }

    @JvmStatic
    fun appendMany(eventLog: IEventLog, eventSourceId: String, events: List<Any>, options: AppendOptions?): List<AppendResult> =
        runBlocking { eventLog.appendMany(eventSourceId, events, options) }

    @JvmStatic
    fun hasEventsFor(eventLog: IEventLog, eventSourceId: String): Boolean =
        runBlocking { eventLog.hasEventsFor(eventSourceId) }

    @JvmStatic
    fun getSequenceNumber(result: AppendResult): Long = result.sequenceNumber.value
}

/**
 * Java-friendly bridge for ITransactionalEventSequence operations.
 */
object TransactionalEventSequenceJavaBridge {
    @JvmStatic
    fun append(sequence: ITransactionalEventSequence, eventSourceId: String, event: Any, options: AppendOptions?): AppendResult =
        runBlocking { sequence.append(eventSourceId, event, options) }

    @JvmStatic
    fun appendMany(sequence: ITransactionalEventSequence, eventSourceId: String, events: List<Any>, options: AppendOptions?): List<AppendResult> =
        runBlocking { sequence.appendMany(eventSourceId, events, options) }
}

/**
 * Java-friendly bridge for IReadModelsService operations.
 */
object ReadModelsJavaBridge {
    @JvmStatic
    fun register(service: IReadModelsService, vararg readModelClasses: Class<*>) {
        runBlocking {
            service.register(*readModelClasses.map { it.kotlin }.toTypedArray())
        }
    }

    @JvmStatic
    fun <T : Any> getInstanceByKey(service: IReadModelsService, readModelClass: Class<T>, key: String): T? =
        runBlocking { service.getInstanceByKey(readModelClass.kotlin, key) }

    @JvmStatic
    fun <T : Any> getInstances(service: IReadModelsService, readModelClass: Class<T>): List<T> =
        runBlocking { service.getInstances(readModelClass.kotlin) }

    @JvmStatic
    fun <T : Any> getSnapshotsById(service: IReadModelsService, readModelClass: Class<T>, key: String): List<ReadModelSnapshot<T>> =
        runBlocking { service.getSnapshotsById(readModelClass.kotlin, key) }

    @JvmStatic
    fun dehydrateSession(service: IReadModelsService, readModelClass: Class<*>, key: String, sessionId: String) {
        runBlocking { service.dehydrateSession(readModelClass.kotlin, key, sessionId) }
    }

    @JvmStatic
    fun <T : Any> release(service: IReadModelsService, instance: T): T =
        runBlocking { service.release(instance) }

    @JvmStatic
    fun <T : Any> releaseMany(service: IReadModelsService, instances: List<T>): List<T> =
        runBlocking { service.releaseMany(instances) }

    @JvmStatic
    fun <T : Any> getMaterializedInstances(service: IReadModelsService, readModelClass: Class<T>, skip: Int, take: Int): List<T> =
        runBlocking { service.materialized.getInstances(readModelClass.kotlin, skip, take) }
}

/**
 * Java-friendly bridge for constraint builder operations.
 */
object ConstraintBuilderJavaBridge {
    @JvmStatic
    fun <TEvent : Any> uniqueFor(builder: IConstraintBuilder, eventClass: Class<TEvent>, message: String): IConstraintBuilder =
        builder.uniqueFor(eventClass.kotlin, message)
}

/**
 * Java-friendly bridge for unique constraint builder operations.
 *
 * Java has no equivalent of a Kotlin property reference (`SomeEvent::email`), so this bridge
 * takes the property name as a plain [String] and resolves it via [IUniqueConstraintBuilder.onWithPropertyName]
 * rather than [IUniqueConstraintBuilder.on].
 */
object UniqueConstraintBuilderJavaBridge {
    @JvmStatic
    fun <TEvent : Any> on(
        builder: IUniqueConstraintBuilder,
        eventClass: Class<TEvent>,
        propertyName: String
    ): IUniqueConstraintBuilder = builder.onWithPropertyName(eventClass.kotlin, propertyName)
}

/**
 * Java-friendly bridge for projection builder operations.
 */
object ProjectionBuilderJavaBridge {
    @JvmStatic
    fun <TReadModel : Any, TEvent : Any> from(
        builder: IProjectionBuilderFor<TReadModel>,
        eventClass: Class<TEvent>
    ): IProjectionBuilderFor<TReadModel> = builder.from(eventClass.kotlin)
}

/**
 * Java-friendly bridge for EventTypesService operations.
 */
object EventTypesServiceJavaBridge {
    @JvmStatic
    fun register(service: IEventTypesService, vararg eventClasses: Class<*>) {
        runBlocking {
            service.register(*eventClasses.map { it.kotlin }.toTypedArray())
        }
    }

    @JvmStatic
    fun registerSingle(service: IEventTypesService, eventClass: Class<*>) {
        runBlocking { service.registerSingle(eventClass.kotlin) }
    }

    @JvmStatic
    fun getAllGenerationsForEventType(service: IEventTypesService, eventTypeId: String): List<Events.EventTypeRegistration> =
        runBlocking { service.getAllGenerationsForEventType(eventTypeId) }
}

/**
 * Java-friendly bridge for IConstraintsService operations.
 */
object ConstraintsServiceJavaBridge {
    @JvmStatic
    fun register(service: IConstraintsService, vararg constraints: Any) {
        runBlocking {
            service.register(*constraints)
        }
    }
}

/**
 * Java-friendly bridge for UnitOfWork operations.
 */
object UnitOfWorkJavaBridge {
    @JvmStatic
    fun commit(unitOfWork: UnitOfWork) {
        runBlocking { unitOfWork.commit() }
    }

    @JvmStatic
    fun rollback(unitOfWork: UnitOfWork) {
        runBlocking { unitOfWork.rollback() }
    }
}

/**
 * Java-friendly bridge for IReactorsService operations.
 */
object ReactorsServiceJavaBridge {
    @JvmStatic
    fun register(service: IReactorsService, reactor: Any): Job =
        runBlocking { service.register(reactor) }
}

/**
 * Java-friendly bridge for IReducersService operations.
 */
object ReducersServiceJavaBridge {
    @JvmStatic
    fun register(service: IReducersService, reducer: Any): Job =
        runBlocking { service.register(reducer) }
}

/**
 * Java-friendly bridge for IProjectionsService operations.
 */
object ProjectionsServiceJavaBridge {
    @JvmStatic
    fun register(service: IProjectionsService, vararg projections: Any) {
        runBlocking { service.register(*projections) }
    }
}

/**
 * Java-friendly bridge for NamespacesService operations.
 */
object NamespacesServiceJavaBridge {
    @JvmStatic
    fun ensure(service: INamespacesService, namespaceName: String) {
        runBlocking { service.ensure(namespaceName) }
    }

    @JvmStatic
    fun getAll(service: INamespacesService): List<String> =
        runBlocking { service.getAll() }
}

/**
 * Java-friendly bridge for IIdentityManagerService operations.
 */
object IdentityManagerServiceJavaBridge {
    @JvmStatic
    fun rename(service: IIdentityManagerService, subject: String, name: String) {
        runBlocking { service.rename(subject, name) }
    }
}

/**
 * Java-friendly bridge for IEventSeedingService operations.
 */
object EventSeedingServiceJavaBridge {
    @JvmStatic
    fun seed(service: IEventSeedingService, vararg seeders: Any) {
        runBlocking { service.seed(*seeders) }
    }
}

/**
 * Java-friendly bridge for IEventSeedingBuilder operations.
 */
object EventSeedingBuilderJavaBridge {
    @JvmStatic
    fun <TEvent : Any> forEventType(
        builder: IEventSeedingBuilder,
        eventClass: Class<TEvent>,
        eventSourceId: String,
        events: List<TEvent>
    ): IEventSeedingBuilder = builder.forEventType(eventClass.kotlin, eventSourceId, events)
}

/**
 * Java-friendly bridge for IEventSeedingScopeBuilder operations.
 */
object EventSeedingScopeBuilderJavaBridge {
    @JvmStatic
    fun <TEvent : Any> forEventType(
        builder: IEventSeedingScopeBuilder,
        eventClass: Class<TEvent>,
        eventSourceId: String,
        events: List<TEvent>
    ): IEventSeedingScopeBuilder = builder.forEventType(eventClass.kotlin, eventSourceId, events)
}

/**
 * Java-friendly bridge for ExternalServicesService operations.
 */
object ExternalServicesServiceJavaBridge {
    @JvmStatic
    fun register(service: IExternalServicesService, name: String, configure: (IExternalServiceBuilder) -> Unit) {
        runBlocking { service.register(name, configure) }
    }
}

/**
 * Java-friendly bridge for JobsService operations.
 */
object JobsServiceJavaBridge {
    @JvmStatic
    fun stop(service: IJobsService, jobId: String) {
        runBlocking { service.stop(jobId) }
    }

    @JvmStatic
    fun resume(service: IJobsService, jobId: String) {
        runBlocking { service.resume(jobId) }
    }

    @JvmStatic
    fun delete(service: IJobsService, jobId: String) {
        runBlocking { service.delete(jobId) }
    }

    @JvmStatic
    fun getJob(service: IJobsService, jobId: String): JobsOuterClass.Job? =
        runBlocking { service.getJob(jobId) }

    @JvmStatic
    fun getJobs(service: IJobsService): List<JobsOuterClass.Job> =
        runBlocking { service.getJobs() }

    @JvmStatic
    fun getJobSteps(service: IJobsService, jobId: String): List<JobsOuterClass.JobStep> =
        runBlocking { service.getJobSteps(jobId) }
}

/**
 * Java-friendly bridge for event store subscription builder operations.
 */
object EventStoreSubscriptionBuilderJavaBridge {
    @JvmStatic
    fun <TEvent : Any> withEventType(builder: IEventStoreSubscriptionBuilder, eventClass: Class<TEvent>): IEventStoreSubscriptionBuilder =
        builder.withEventType(eventClass.kotlin)
}

/**
 * Java-friendly bridge for EventStoreSubscriptionsService operations.
 */
object EventStoreSubscriptionsServiceJavaBridge {
    @JvmStatic
    fun subscribe(
        service: IEventStoreSubscriptionsService,
        id: String,
        sourceEventStore: String,
        configure: (IEventStoreSubscriptionBuilder) -> Unit
    ) {
        runBlocking { service.subscribe(id, sourceEventStore, configure) }
    }

    @JvmStatic
    fun unsubscribe(service: IEventStoreSubscriptionsService, id: String) {
        runBlocking { service.unsubscribe(id) }
    }

    @JvmStatic
    fun getAll(service: IEventStoreSubscriptionsService): List<ObservationEventstoresubscriptions.EventStoreSubscriptionDefinition> =
        runBlocking { service.getAll() }
}

/**
 * Java-friendly bridge for webhook definition builder operations.
 */
object WebhookDefinitionBuilderJavaBridge {
    @JvmStatic
    fun <TEvent : Any> withEventType(builder: IWebhookDefinitionBuilder, eventClass: Class<TEvent>): IWebhookDefinitionBuilder =
        builder.withEventType(eventClass.kotlin)
}

/**
 * Java-friendly bridge for WebhooksService operations.
 */
object WebhooksServiceJavaBridge {
    @JvmStatic
    fun register(service: IWebhooksService, vararg definers: Any) {
        runBlocking { service.register(*definers) }
    }

    @JvmStatic
    fun register(service: IWebhooksService, id: String, targetUrl: String, configure: (IWebhookDefinitionBuilder) -> Unit) {
        runBlocking { service.register(id, targetUrl, configure) }
    }

    @JvmStatic
    fun getAll(service: IWebhooksService): List<ObservationWebhooks.WebhookDefinition> =
        runBlocking { service.getAll() }

    @JvmStatic
    fun remove(service: IWebhooksService, id: String) {
        runBlocking { service.remove(id) }
    }
}

/**
 * Java-friendly bridge for CausationManager operations.
 */
object CausationManagerJavaBridge {
    @JvmStatic
    fun add(manager: CausationManager, typeName: String, properties: Map<String, String>) {
        manager.add(CausationType(typeName), properties)
    }
}

/**
 * Java-friendly bridge for ComplianceService operations.
 */
object ComplianceServiceJavaBridge {
    @JvmStatic
    fun release(service: IComplianceService, subject: String, schema: String, payload: String): String =
        runBlocking { service.release(subject, schema, payload) }

    @JvmStatic
    fun deleteEncryptionKey(service: IComplianceService, identifier: String) {
        runBlocking { service.deleteEncryptionKey(identifier) }
    }
}
