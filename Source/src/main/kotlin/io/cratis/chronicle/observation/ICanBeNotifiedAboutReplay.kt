// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * Marks a reactor as wanting to know when a replay begins and ends.
 *
 * `EventContext.observationState` already tells a handler whether the event it is looking at arrived
 * during a replay, which is enough to make one handler replay-aware. It is not enough to do anything
 * *around* the replay - clearing a cache before it starts, flushing a batch after it finishes, or
 * simply not sending a thousand emails a second time.
 *
 * This is a marker interface; dispatch is by convention, like reactor handlers themselves. Declare
 * either or both of:
 *
 * ```kotlin
 * @Reactor
 * class EmployeeAlerts(private val mail: Mailer) : ICanBeNotifiedAboutReplay {
 *     private var replaying = false
 *
 *     fun replayBegan(context: ReplayContext) { replaying = true }
 *
 *     suspend fun replayEnded(context: ReplayContext) {
 *         replaying = false
 *         mail.summarize(context.partition)
 *     }
 *
 *     fun employeeHired(event: EmployeeHired) {
 *         if (!replaying) mail.welcome(event.name)
 *     }
 * }
 * ```
 *
 * Both may be suspending or not, and both may take no parameter at all if the [ReplayContext] is of
 * no interest. Implementing the interface and declaring neither method is rejected at registration -
 * a marker with nothing behind it would silently do nothing.
 *
 * The kernel flags the first and last event of a replay rather than sending a separate signal, so
 * `replayBegan` runs immediately before the first replayed event is handled and `replayEnded`
 * immediately after the last one. A replay that delivers no events at all therefore produces no
 * notification: there was no first event to flag.
 */
interface ICanBeNotifiedAboutReplay
