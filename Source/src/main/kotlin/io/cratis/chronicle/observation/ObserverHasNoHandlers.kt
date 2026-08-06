// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation

import kotlin.reflect.KClass

/**
 * Thrown when an observer is registered but no handler method could be found on it.
 *
 * An observer with nothing to handle subscribes to no event types at all, so the kernel has nothing
 * to deliver and the observer sits there doing nothing forever. That is almost always a typo in an
 * event type name, a missing `@EventType` on the event, or a handler left private - none of which
 * announce themselves at runtime. Failing at registration names the class while the cause is still
 * in front of you.
 */
class ObserverHasNoHandlers(
    observerClass: KClass<*>,
    expectation: String
) : IllegalArgumentException(
    "'${observerClass.simpleName}' has no handler methods, so it would observe nothing. $expectation"
)
