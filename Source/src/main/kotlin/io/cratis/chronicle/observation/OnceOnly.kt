// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * Marks a reactor class or an individual handler method as non-replayable.
 *
 * Put it on the class and the whole reactor is excluded from every replay operation - redaction,
 * revision, and observer rewind. Put it on a single method and only that handler is excluded, while
 * the reactor's other handlers still replay.
 *
 * The two placements take effect at different points. A class-level placement registers the reactor
 * as non-replayable, so a replay never starts for it. A method-level placement leaves the reactor
 * replayable and skips the marked handler for every event that arrives as part of a replay.
 *
 * Use this for side effects where running again is worse than never running again - a physical
 * letter, a payment. Use [Replay] instead when the handler should do something *different* during a
 * replay rather than nothing at all.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OnceOnly
