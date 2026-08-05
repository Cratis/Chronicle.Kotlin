// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation;

import io.cratis.chronicle.events.EventContext;

/**
 * A reducer written in Java, with an explicit event sequence, a passive registration, and both
 * the two- and three-argument handler shapes.
 */
@Reducer(id = "java-reducer", eventSequence = "outbox", isActive = false)
public class JavaReducer {

    public JavaBookState bookAdded(JavaBookAdded event, JavaBookState state) {
        return new JavaBookState(event.title(), true);
    }

    public JavaBookState bookRemoved(JavaBookRemoved event, JavaBookState state, EventContext context) {
        return new JavaBookState(event.title(), false);
    }
}
