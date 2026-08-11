// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.spring

import io.cratis.chronicle.IEventStore
import io.cratis.chronicle.eventSequences.AppendOptions
import io.cratis.chronicle.eventSequences.AppendResult
import io.cratis.chronicle.readModels.ReadModelSnapshot
import io.cratis.chronicle.transactions.IUnitOfWork
import kotlinx.coroutines.runBlocking

/**
 * The everyday Chronicle operations, without coroutines.
 *
 * Spring MVC handlers are blocking, and Java has no coroutines at all, so a bean that suspends is
 * awkward in both. Inject this instead and the common things — appending an event, reading a read
 * model, doing several appends atomically — are ordinary method calls:
 *
 * ```java
 * @RestController
 * public class Employees {
 *     private final Chronicle chronicle;
 *
 *     public Employees(Chronicle chronicle) { this.chronicle = chronicle; }
 *
 *     @PostMapping("/employees/{id}/hire")
 *     public void hire(@PathVariable String id, @RequestBody Hire hire) {
 *         chronicle.append(id, new EmployeeHired(hire.firstName(), hire.lastName(), hire.title()));
 *     }
 *
 *     @GetMapping("/employees/{id}")
 *     public EmployeeState get(@PathVariable String id) {
 *         return chronicle.readModel(EmployeeState.class, id);
 *     }
 * }
 * ```
 *
 * Anything beyond the everyday is one hop away through [eventStore], which is the full API — in
 * Kotlin, prefer going straight there and suspending rather than blocking a thread.
 *
 * Every call is routed to the namespace the current piece of work belongs to, exactly as with an
 * injected `IEventStore`.
 *
 * @param eventStore The event store every operation is performed against.
 */
class Chronicle(val eventStore: IEventStore) {
    /**
     * Appends an event.
     *
     * @param eventSourceId The event source the event belongs to.
     * @param event The event to append.
     * @param options Optional append options.
     * @return The outcome of the append, including the sequence number and any constraint violations.
     */
    @JvmOverloads
    fun append(eventSourceId: String, event: Any, options: AppendOptions? = null): AppendResult =
        runBlocking { eventStore.eventLog.append(eventSourceId, event, options) }

    /**
     * Appends several events to the same event source, in order.
     *
     * @param eventSourceId The event source the events belong to.
     * @param events The events to append.
     * @param options Optional append options.
     * @return The outcome of each append.
     */
    @JvmOverloads
    fun appendMany(eventSourceId: String, events: List<Any>, options: AppendOptions? = null): List<AppendResult> =
        runBlocking { eventStore.eventLog.appendMany(eventSourceId, events, options) }

    /**
     * Reads a single read model instance by its key.
     *
     * @param readModelType The read model to read.
     * @param key The key of the instance, normally the event source id.
     * @return The instance, or `null` when no events have produced one yet.
     */
    fun <T : Any> readModel(readModelType: Class<T>, key: String): T? =
        runBlocking { eventStore.readModels.getInstanceByKey(readModelType.kotlin, key) }

    /**
     * Reads every instance of a read model.
     *
     * @param readModelType The read model to read.
     * @return Every instance.
     */
    fun <T : Any> readModels(readModelType: Class<T>): List<T> =
        runBlocking { eventStore.readModels.getInstances(readModelType.kotlin) }

    /**
     * Reads the full history of states a read model instance has been through, rather than just the
     * latest one.
     *
     * @param readModelType The read model to read.
     * @param key The key of the instance.
     * @return Every intermediate state, oldest first.
     */
    fun <T : Any> readModelHistory(readModelType: Class<T>, key: String): List<ReadModelSnapshot<T>> =
        runBlocking { eventStore.readModels.getSnapshotsById(readModelType.kotlin, key) }

    /**
     * Runs [work] inside a unit of work, committing it when the work returns and rolling it back if it
     * throws — so everything appended inside lands together or not at all.
     *
     * A web request already runs inside one of these, so reach for this in scheduled jobs, message
     * handlers, and anywhere else outside a request.
     *
     * @param work The work to perform.
     * @return The unit of work, which reports whether it succeeded and what violated a constraint if not.
     */
    fun <T> inUnitOfWork(work: (IUnitOfWork) -> T): IUnitOfWork {
        val unitOfWork = eventStore.unitOfWorkManager.begin()
        try {
            work(unitOfWork)
            if (!unitOfWork.isCompleted) runBlocking { unitOfWork.commit() }
        } catch (throwable: Throwable) {
            if (!unitOfWork.isCompleted) runBlocking { unitOfWork.rollback() }
            throw throwable
        }
        return unitOfWork
    }

    /**
     * Registers every artifact this application owns with the kernel.
     *
     * Only needed when automatic registration is turned off.
     */
    fun registerAll() {
        runBlocking { eventStore.registerAll() }
    }
}
