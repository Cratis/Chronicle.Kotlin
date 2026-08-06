// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import io.cratis.chronicle.events.EventContext
import io.cratis.chronicle.readModels.IReadModelsService
import io.cratis.chronicle.readModels.ReadModel
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.hasAnnotation

/**
 * Supplies a read model parameter with the current instance for the event's event source.
 *
 * A reactor that needs to know the state something is in would otherwise have to read the event log
 * itself, or hold the read models service and remember to key the lookup on the right identifier.
 * Declaring the read model as a parameter says the same thing in the signature:
 *
 * ```kotlin
 * @Reactor
 * class OverdraftAlerts(private val mail: Mailer) {
 *     suspend fun moneyWithdrawn(event: MoneyWithdrawn, account: AccountBalance?) {
 *         if ((account?.balance ?: 0.0) < 0) mail.overdrawn(account!!.id)
 *     }
 * }
 * ```
 *
 * The instance is fetched per invocation, keyed by the event source id the event arrived under, and
 * is `null` when nothing has been projected for that key yet. Declare the parameter nullable to see
 * that case; a non-nullable one throws rather than pretending an absent read model is an empty one.
 *
 * @param readModels The read models to look instances up through.
 */
class ReadModelArgument(private val readModels: IReadModelsService) : IReactorMethodArgumentResolver {

    override fun canResolve(parameter: KParameter): Boolean =
        (parameter.type.classifier as? KClass<*>)?.hasAnnotation<ReadModel>() == true

    override suspend fun resolve(parameter: KParameter, context: EventContext): Any? {
        @Suppress("UNCHECKED_CAST")
        val readModelClass = parameter.type.classifier as KClass<Any>
        return readModels.getInstanceByKey(readModelClass, context.eventSourceId)
    }
}
