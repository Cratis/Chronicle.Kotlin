// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.testing

import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.events.EventType
import io.cratis.chronicle.json.chronicleGson
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible

/**
 * Specifies what a reducer folds a stream of events into, with no kernel behind it.
 *
 * A reducer is a fold: events in, read model out. That is a pure enough thing to run in-process, and
 * running it there is what makes "given these three events, the state should be this" an ordinary
 * unit test rather than something that needs a kernel, a sink and a Docker daemon.
 *
 * The reducer runs exactly as the client would run it against a real kernel: handlers are discovered
 * the same way, chosen by event type the same way, invoked with the same shapes - `(event)`,
 * `(event, state)`, `(event, state, context)` - and awaited if they suspend. What is left out is the
 * kernel and the sink.
 *
 * ```kotlin
 * val scenario = ReadModelScenario(EmployeeStateReducer())
 *
 * val state = scenario.fold(
 *     "employee-1",
 *     EmployeeHired("Ada", "Lovelace", "Engineer"),
 *     EmployeePromoted("Principal Engineer")
 * )
 *
 * assertEquals("Principal Engineer", state!!.title)
 * ```
 *
 * Chronicle folds each event source independently, so [fold] does too: state does not leak between
 * event sources, which is exactly the mistake a reducer spec should be able to catch.
 *
 * @param reducer The reducer instance to fold with.
 * @param eventStoreName The event store name stamped on event contexts.
 * @param namespace The namespace stamped on event contexts.
 */
class ReadModelScenario<TReadModel : Any>(
    private val reducer: Any,
    eventStoreName: String = "testing",
    namespace: String = "default"
) {
    private val handlers: Map<String, KFunction<*>> = reducer::class.memberFunctions
        .mapNotNull { function ->
            val eventClass = function.parameters.getOrNull(1)?.type?.classifier as? KClass<*>
            val annotation = eventClass?.findAnnotation<EventType>() ?: return@mapNotNull null

            // A reducer declared `internal` or private is ordinary Kotlin - and common in a spec -
            // but its JVM class is not public, so reflection refuses to invoke the handler without
            // being told. The client does the same when it discovers handlers.
            function.isAccessible = true

            annotation.id.ifEmpty { eventClass.simpleName!! } to function
        }
        .toMap()

    private val eventClasses: Map<String, KClass<*>> = reducer::class.memberFunctions
        .mapNotNull { function ->
            val eventClass = function.parameters.getOrNull(1)?.type?.classifier as? KClass<*>
            val annotation = eventClass?.findAnnotation<EventType>() ?: return@mapNotNull null
            annotation.id.ifEmpty { eventClass.simpleName!! } to eventClass
        }
        .toMap()

    /** The events this scenario has been given, in order. */
    val eventLog: InMemoryEventSequence = InMemoryEventSequence(
        eventStoreName = eventStoreName,
        namespace = namespace
    )

    private val states = mutableMapOf<String, Any?>()

    init {
        require(handlers.isNotEmpty()) {
            "'${reducer::class.simpleName}' has no handler methods, so there is nothing to fold. " +
                "A reducer handler is a public method whose first parameter is a class annotated " +
                "with @EventType, returning the read model it produces."
        }
    }

    /** Forgets every event and every folded state. */
    fun reset() {
        eventLog.clear()
        states.clear()
    }

    /**
     * Folds [events] into the state for [eventSourceId] and returns what came out.
     *
     * Calling this again for the same event source continues from where it left off, so a spec can
     * build a history in stages. A different event source starts from nothing, as it would.
     *
     * @param eventSourceId The event source the events belong to.
     * @param events The events to fold, in order.
     * @return The state after folding, or `null` when the reducer produced none.
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun fold(eventSourceId: String, vararg events: Any): TReadModel? {
        for (event in events) {
            eventLog.append(eventSourceId, event)
            val context = eventLog.events.last().context
            val handler = handlers[context.eventType.id.value] ?: continue

            // Round-tripped through the client's serializer first, so a reducer that only works on
            // an instance it was handed directly - and would fail on one deserialized from the
            // kernel - fails here too.
            val eventClass = eventClasses.getValue(context.eventType.id.value)
            val deserialized = chronicleGson.fromJson(chronicleGson.toJson(event), eventClass.java)

            states[eventSourceId] = invoke(handler, deserialized, states[eventSourceId], context)
        }

        return states[eventSourceId] as TReadModel?
    }

    /**
     * The state folded so far for [eventSourceId], or `null` when nothing has been folded for it.
     */
    @Suppress("UNCHECKED_CAST")
    fun stateFor(eventSourceId: String): TReadModel? = states[eventSourceId] as TReadModel?

    /** Every event source that has had anything folded, in order of first appearance. */
    fun eventSourceIds(): List<String> = eventLog.events.map { it.context.eventSourceId }.distinct()

    /**
     * Invokes the handler with whichever of the three shapes it declared.
     *
     * `callSuspend` covers a suspending handler and a plain one alike, which is what the client does.
     */
    private suspend fun invoke(
        handler: KFunction<*>,
        event: Any,
        state: Any?,
        context: EventContext
    ): Any? =
        // Index 0 is the instance receiver, so the three shapes arrive as 2, 3 and 4.
        when (handler.parameters.size) {
            2 -> handler.callSuspend(reducer, event)
            4 -> handler.callSuspend(reducer, event, state, context)
            else -> handler.callSuspend(reducer, event, state)
        }
}
