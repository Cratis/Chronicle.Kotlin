// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.readModels

import io.cratis.chronicle.observation.InvalidHandlerSignature
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.javaMethod

/**
 * A single handler method on a read model reactor, together with the change it reacts to.
 *
 * @property changeType The [ReadModelChangeType] the method's name selects it for.
 * @property function The method to invoke.
 * @property readModelClass The read model the method takes as its first parameter.
 * @property isCollection Whether that first parameter is a `List` of read models rather than one instance.
 * @property takesChangeset Whether the method also takes the [ReadModelChangeset] describing the change.
 */
internal data class ReadModelReactorMethod(
    val changeType: ReadModelChangeType,
    val function: KFunction<*>,
    val readModelClass: KClass<*>,
    val isCollection: Boolean,
    val takesChangeset: Boolean
) {
    /** How many parameters the method takes, counting the instance receiver at index 0. */
    val parameterCount: Int get() = function.parameters.size

    /** Whether the read model parameter accepts `null`. Always false for a Java-declared parameter. */
    val takesNullableReadModel: Boolean get() = function.parameters[1].type.isMarkedNullable

    /**
     * Whether the method is declared in Kotlin. Only Kotlin parameters carry nullability, so this is
     * what makes the nullability check on a removal handler meaningful rather than a false alarm.
     */
    val isDeclaredInKotlin: Boolean
        get() = function.javaMethod?.declaringClass?.isAnnotationPresent(Metadata::class.java) == true

    /**
     * Invokes the handler for [changeset], returning whatever it returned so the caller can append
     * any resulting side-effect events.
     *
     * A collection handler is handed the changed instance as a single-element list, or an empty one
     * when the change removed it - never `null`, which a `List` parameter could not accept.
     */
    fun invoke(reactor: Any, changeset: ReadModelChangeset<*>): Any? {
        val readModel = if (isCollection) listOfNotNull(changeset.readModel) else changeset.readModel
        return if (takesChangeset) {
            function.call(reactor, readModel, changeset)
        } else {
            function.call(reactor, readModel)
        }
    }

    /** Throws [InvalidHandlerSignature] with [reason] for this handler. */
    fun reject(reactorClass: KClass<*>, reason: String): Nothing =
        throw InvalidHandlerSignature(reactorClass, function.name, reason)

    companion object {
        /**
         * Reads [function] as a read model reactor handler, or returns `null` when it is not shaped
         * like one - its name is not a change type, or it takes no read model.
         */
        fun from(function: KFunction<*>): ReadModelReactorMethod? {
            val changeType = ReadModelChangeType.entries
                .firstOrNull { it.name.equals(function.name, ignoreCase = true) } ?: return null

            // Index 0 is the instance receiver, so a handler has at least two parameters.
            val readModelParameter = function.parameters.getOrNull(1) ?: return null
            val declared = readModelParameter.type.classifier as? KClass<*> ?: return null

            val isCollection = declared.isSubclassOf(Collection::class)
            val readModelClass = if (isCollection) {
                readModelParameter.type.arguments.firstOrNull()?.type?.classifier as? KClass<*> ?: return null
            } else {
                declared
            }

            return ReadModelReactorMethod(
                changeType = changeType,
                function = function,
                readModelClass = readModelClass,
                isCollection = isCollection,
                takesChangeset = function.parameters.getOrNull(2)?.type?.classifier == ReadModelChangeset::class
            )
        }
    }
}
