// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation;

/** A reducer written in Java that folds an event source's events in one batch call. */
@Reducer(id = "java-batch-reducer", maxConcurrency = 1)
public class JavaBatchReducer {

    @Observes(JavaBookAdded.class)
    @Observes(JavaBookRemoved.class)
    public JavaBookState reduce(EventBatch events, JavaBookState current, BatchContext context) {
        JavaBookState state = current;
        for (EventAndContext eventAndContext : events) {
            if (eventAndContext.getEvent() instanceof JavaBookAdded added) {
                state = new JavaBookState(added.title(), true);
            } else if (eventAndContext.getEvent() instanceof JavaBookRemoved removed) {
                state = new JavaBookState(removed.title(), false);
            }
        }
        return state;
    }
}
