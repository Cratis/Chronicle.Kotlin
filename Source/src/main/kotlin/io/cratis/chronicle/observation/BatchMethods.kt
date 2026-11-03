// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaMethod

/**
 * Discovers the batch methods of an observer and enforces the rules that keep them apart from
 * per-event handlers.
 *
 * A method is a batch method because of its first parameter - an [EventBatch] - not its name, and it
 * says which events it takes with [Observes]. The rules shared by reactors and reducers are checked
 * here; what each observer kind allows beyond them is checked where it registers.
 */
internal object BatchMethods {
    /**
     * Finds the batch methods of [observerClass].
     *
     * @throws ObservesOnlyOnBatchMethods A method that does not take an [EventBatch] declares [Observes].
     * @throws BatchMethodObservesNothing A batch method declares no [Observes].
     * @throws BatchMethodObservesUnknownEventType A batch method observes a class that is not an event type.
     * @throws EventTypeObservedByMoreThanOneMethod Two batch methods observe the same event type.
     */
    fun from(observerClass: KClass<*>): List<BatchMethod> {
        val batchMethods = mutableListOf<BatchMethod>()

        for (function in observerClass.memberFunctions.sortedBy { it.name }) {
            val observes = observesOf(function)
            if (!takesEventBatch(function)) {
                if (observes.isNotEmpty()) throw ObservesOnlyOnBatchMethods(observerClass, function.name)
                continue
            }

            if (observes.isEmpty()) throw BatchMethodObservesNothing(observerClass, function.name)

            val eventTypes = linkedMapOf<String, KClass<*>>()
            for (eventClass in observes.map { it.value }) {
                val id = BatchMethod.eventTypeIdOf(eventClass)
                    ?: throw BatchMethodObservesUnknownEventType(observerClass, function.name, eventClass)
                if (batchMethods.any { it.observes(id) }) throw EventTypeObservedByMoreThanOneMethod(observerClass, id)
                eventTypes[id] = eventClass
            }

            // A batch method on an `internal` or private class is ordinary Kotlin, but its JVM class
            // is not public, so reflection refuses to invoke it without being told.
            function.isAccessible = true
            batchMethods.add(BatchMethod(function, eventTypes, function.findAnnotation<OnceOnly>() != null))
        }

        return batchMethods
    }

    /** Whether [function] takes an [EventBatch] as its first parameter past the receiver. */
    fun takesEventBatch(function: KFunction<*>): Boolean =
        function.parameters.getOrNull(1)?.type?.classifier == EventBatch::class

    /**
     * Throws [BatchContextOnlyForBatchMethods] when one of the per-event [handlers] of [observerClass]
     * takes a [BatchContext].
     */
    fun requireNoBatchContext(observerClass: KClass<*>, handlers: Collection<EventHandlerMethod>) {
        val offending = handlers.firstOrNull { handler ->
            handler.function.parameters.any { it.type.classifier == BatchContext::class }
        }
        if (offending != null) throw BatchContextOnlyForBatchMethods(observerClass, offending.function.name)
    }

    /**
     * The [Observes] annotations on [function]. Kotlin reflection unwraps the container a repeated
     * annotation is stored in; Java reflection is asked as well, for a method declared in Java.
     */
    private fun observesOf(function: KFunction<*>): List<Observes> =
        function.findAnnotations<Observes>().ifEmpty {
            function.javaMethod?.getAnnotationsByType(Observes::class.java)?.toList().orEmpty()
        }
}
