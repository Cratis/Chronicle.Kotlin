// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

/**
 * Marks a reactor class as one that should not observe history that predates its first registration.
 *
 * A new reactor normally catches up from the very beginning of the event sequence, as ordinary
 * observation, so one whose handlers cause side effects - sending a notification, calling an
 * external system, starting a job - acts on every historical event the moment it is deployed.
 * [OnceOnly] does not prevent that, because it only excludes replay. This annotation does: the first
 * time Chronicle sees the reactor, it begins at the tail of the event sequence and observes only
 * what is appended from then on.
 *
 * It only affects the first registration. A reactor that Chronicle already holds a position for
 * keeps it, so the annotation is safe to leave in place and never skips events after a restart.
 * Renaming the reactor or moving its package changes its default id, which makes it a new reactor
 * that begins at the tail again - unless an explicit id is given on [Reactor]. An explicit replay
 * still replays from the beginning, because this only chooses where a reactor starts.
 *
 * Both the Chronicle server and this client must support it. A server that predates it ignores the
 * request and the reactor catches up from the beginning.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class StartAtTail
