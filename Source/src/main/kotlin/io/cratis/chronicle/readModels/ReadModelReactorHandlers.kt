// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import io.cratis.chronicle.observation.ObserverHasNoHandlers
import kotlin.reflect.KClass
import kotlin.reflect.full.memberFunctions

/**
 * The handler methods of a read model reactor, indexed by the change each one reacts to.
 *
 * @property methods Every discovered handler method on the reactor.
 */
internal class ReadModelReactorHandlers(private val methods: List<ReadModelReactorMethod>) {
    /**
     * Every read model handled. Each one needs its own watch, so a reactor covering two read models
     * ends up with two subscriptions.
     */
    val readModelClasses: Set<KClass<*>> = methods.map { it.readModelClass }.toSet()

    /**
     * The handlers to run for a [changeType] on [readModelClass], in declaration order.
     *
     * More than one may match - a reactor is free to declare a single-instance and a collection
     * handler for the same change - and none matching simply means the reactor does not care about
     * that change.
     */
    fun resolve(readModelClass: KClass<*>, changeType: ReadModelChangeType): List<ReadModelReactorMethod> =
        methods.filter { it.readModelClass == readModelClass && it.changeType == changeType }

    companion object {
        /**
         * Builds the handler set for [reactorClass] by convention: any method named after a
         * [ReadModelChangeType] that takes a read model as its first parameter.
         */
        fun from(reactorClass: KClass<*>): ReadModelReactorHandlers {
            val methods = reactorClass.memberFunctions.mapNotNull { function ->
                ReadModelReactorMethod.from(function)?.also { requireDispatchable(it, reactorClass) }
            }

            if (methods.isEmpty()) {
                throw ObserverHasNoHandlers(
                    reactorClass,
                    "A read model reactor handler is a public method named after a change - " +
                        ReadModelChangeType.entries.joinToString(", ") { it.name.replaceFirstChar(Char::lowercase) } +
                        " - taking the read model as its first parameter."
                )
            }

            return ReadModelReactorHandlers(methods)
        }

        /**
         * A handler is `(readModel)` or `(readModel, changeset)`, suspending or not. Anything else
         * cannot be invoked, so it is rejected at registration rather than failing on every change
         * that arrives.
         */
        private fun requireDispatchable(method: ReadModelReactorMethod, reactorClass: KClass<*>) {
            // Index 0 is the instance receiver, so the two valid shapes arrive as 2 and 3.
            if (method.parameterCount > 3) {
                method.reject(reactorClass, "a handler takes the read model and optionally a ReadModelChangeset")
            }

            if (method.parameterCount == 3 && !method.takesChangeset) {
                method.reject(reactorClass, "its second parameter must be a ReadModelChangeset")
            }

            requireRemovalAcceptsNoInstance(method, reactorClass)
        }

        /**
         * A removal never carries an instance, so a handler declaring a non-nullable read model could
         * only ever be handed something it rejects. Java parameters carry no nullability at all, so
         * only Kotlin-declared handlers can be held to this.
         */
        private fun requireRemovalAcceptsNoInstance(method: ReadModelReactorMethod, reactorClass: KClass<*>) {
            if (method.changeType != ReadModelChangeType.Removed || method.isCollection) return
            if (!method.isDeclaredInKotlin || method.takesNullableReadModel) return

            method.reject(
                reactorClass,
                "a removed handler is never given an instance - declare its read model parameter nullable"
            )
        }
    }
}
