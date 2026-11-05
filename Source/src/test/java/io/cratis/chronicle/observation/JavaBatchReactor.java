// Copyright (c) Cratis. All rights reserved.
// Licensed under the MIT license. See LICENSE file in the project root for full license information.

package io.cratis.chronicle.observation;

import io.cratis.chronicle.events.EventContext;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * A reactor written in Java with a batch method observing two event types through the repeated
 * {@code @Observes}, next to a per-event handler for a third.
 */
@Reactor(id = "java-batch-reactor")
public class JavaBatchReactor {
    public final List<String> indexed = new ArrayList<>();
    public final List<String> confirmed = new ArrayList<>();

    @Observes(JavaBookAdded.class)
    @Observes(JavaBookRemoved.class)
    public void index(EventBatch batch, BatchContext context) {
        for (EventAndContext eventAndContext : batch) {
            if (eventAndContext.getEvent() instanceof JavaBookAdded added) {
                if (added.title().equals("throttled")) {
                    batch.defer(eventAndContext.getContext(), Duration.ofSeconds(1));
                } else {
                    indexed.add("added:" + added.title());
                }
            } else if (eventAndContext.getEvent() instanceof JavaBookRemoved removed) {
                indexed.add("removed:" + removed.title());
            }
        }
    }

    public void bookStored(JavaBookStored event, EventContext context) {
        confirmed.add(event.title());
    }
}
