// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation;

import io.cratis.chronicle.events.EventContext;

/**
 * A reactor written in Java, exercising every annotation placement from the Java side:
 * an explicit event sequence, a handler taking an {@link EventContext}, a {@code @Replay}
 * takeover handler, and a method-level {@code @OnceOnly}.
 */
@Reactor(id = "java-reactor", eventSequence = "outbox")
public class JavaReactor {

    public void bookAdded(JavaBookAdded event, EventContext context) {
    }

    @Replay
    public void bookAddedDuringReplay(JavaBookAdded event) {
    }

    @OnceOnly
    public void bookRemoved(JavaBookRemoved event) {
    }
}
