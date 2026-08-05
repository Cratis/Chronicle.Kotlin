// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * Marks a reactor handler method as the one to run while events are being replayed.
 *
 * A reactor sees the same event twice for different reasons: as it happens, and again when its
 * observer is replayed. Those often call for different work - a notification that should go out once
 * when the event happens has no business going out again during a rebuild. Mark a second handler for
 * the same event type with this and it takes over for the duration of the replay, leaving the
 * unmarked handler to the live path.
 *
 * When an event type has no handler marked with this, the regular handler runs during replay as
 * before. When it has one, only the marked handler runs during replay - the regular handler does not
 * also run.
 *
 * Use [OnceOnly] instead when the side effect should simply not happen again on replay.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Replay
