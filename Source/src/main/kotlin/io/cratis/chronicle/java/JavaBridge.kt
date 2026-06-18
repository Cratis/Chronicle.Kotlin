// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.java

import io.cratis.chronicle.auditing.CausationManager
import io.cratis.chronicle.auditing.CausationType
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.eventSequences.IEventLog
import io.cratis.chronicle.eventSequences.ITransactionalEventSequence
import io.cratis.chronicle.readModels.IReadModelsService
import io.cratis.chronicle.constraints.IConstraintBuilder
import io.cratis.chronicle.constraints.IConstraintsService
import io.cratis.chronicle.constraints.IUniqueConstraintBuilder
import io.cratis.chronicle.projections.IProjectionBuilderFor
import io.cratis.chronicle.events.EventTypesService
import io.cratis.chronicle.transactions.UnitOfWork
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
 */
object UniqueConstraintBuilderJavaBridge {
    @JvmStatic
    fun <TEvent : Any, TValue : Any> on(
        builder: IUniqueConstraintBuilder,
        eventClass: Class<TEvent>,
        property: (TEvent) -> TValue?
    ): IUniqueConstraintBuilder = builder.on(eventClass.kotlin, property)
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
    fun register(service: EventTypesService, vararg eventClasses: Class<*>) {
        runBlocking {
            service.register(*eventClasses.map { it.kotlin }.toTypedArray())
        }
    }
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
 * Java-friendly bridge for CausationManager operations.
 */
object CausationManagerJavaBridge {
    @JvmStatic
    fun add(manager: CausationManager, typeName: String, properties: Map<String, String>) {
        manager.add(CausationType(typeName), properties)
    }
}
