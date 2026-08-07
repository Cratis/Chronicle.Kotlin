// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberFunctions

/**
 * The replay begin and end methods found on a reactor that asked to be told about replays.
 *
 * @property began The method to call when a replay starts, if the reactor declared one.
 * @property ended The method to call when a replay finishes, if the reactor declared one.
 */
internal class ReplayNotifications(
    private val began: KFunction<*>?,
    private val ended: KFunction<*>?
) {
    /** Whether there is anything to notify at all. */
    val isEmpty: Boolean get() = began == null && ended == null

    /** Tells the reactor a replay of [context]'s partition is starting. */
    suspend fun notifyBegan(reactor: Any, context: ReplayContext) = invoke(began, reactor, context)

    /** Tells the reactor the replay of [context]'s partition has finished. */
    suspend fun notifyEnded(reactor: Any, context: ReplayContext) = invoke(ended, reactor, context)

    /**
     * Calls [method] with the context, or without it when the reactor did not ask for one.
     *
     * `callSuspend` covers both a plain method and a suspending one, so a reactor that has to await
     * something before or after a replay can say so.
     */
    private suspend fun invoke(method: KFunction<*>?, reactor: Any, context: ReplayContext) {
        if (method == null) return
        // Index 0 is the instance receiver, so a method that wants the context arrives as 2.
        if (method.parameters.size == 2) {
            method.callSuspend(reactor, context)
        } else {
            method.callSuspend(reactor)
        }
    }

    companion object {
        private const val BEGAN = "replayBegan"
        private const val ENDED = "replayEnded"

        /** Nothing to notify, which is every reactor that did not ask to be told. */
        val none: ReplayNotifications = ReplayNotifications(null, null)

        /**
         * Reads the notification methods off [reactorClass], or [none] when it never asked for them.
         *
         * A class implementing [ICanBeNotifiedAboutReplay] with neither method is rejected: the
         * marker says it wants to be told something, and nothing would ever tell it.
         */
        fun from(reactorClass: KClass<*>): ReplayNotifications {
            if (!reactorClass.isSubclassOf(ICanBeNotifiedAboutReplay::class)) return none

            val notifications = ReplayNotifications(
                began = reactorClass.notificationNamed(BEGAN),
                ended = reactorClass.notificationNamed(ENDED)
            )

            if (notifications.isEmpty) {
                throw ObserverHasNoHandlers(
                    reactorClass,
                    "It implements ICanBeNotifiedAboutReplay, so declare '$BEGAN' or '$ENDED', " +
                        "each taking a ReplayContext or nothing at all."
                )
            }

            return notifications
        }

        /**
         * The method called [name], provided it is shaped like a notification - it takes the
         * [ReplayContext] or nothing.
         */
        private fun KClass<*>.notificationNamed(name: String): KFunction<*>? {
            val method = memberFunctions.firstOrNull { it.name == name } ?: return null

            // Index 0 is the instance receiver, so the two valid shapes arrive as 1 and 2.
            if (method.parameters.size > 2) {
                throw InvalidHandlerSignature(
                    this,
                    name,
                    "a replay notification takes a ReplayContext, or nothing at all"
                )
            }

            if (method.parameters.size == 2 &&
                method.parameters[1].type.classifier != ReplayContext::class
            ) {
                throw InvalidHandlerSignature(this, name, "its only parameter must be a ReplayContext")
            }

            return method
        }
    }
}
