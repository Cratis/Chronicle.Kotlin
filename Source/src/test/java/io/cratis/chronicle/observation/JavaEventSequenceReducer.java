// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation;

/** A Java reducer declaring both an event sequence parameter and the standalone annotation. */
@Reducer(id = "java-event-sequence-reducer", eventSequence = "from-parameter")
@EventSequence("from-standalone")
public class JavaEventSequenceReducer {

    public JavaBookState bookAdded(JavaBookAdded event, JavaBookState state) {
        return new JavaBookState(event.title(), true);
    }
}
