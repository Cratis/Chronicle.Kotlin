```java
import io.cratis.chronicle.events.EventContext;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.observation.BatchContext;
import io.cratis.chronicle.observation.EventAndContext;
import io.cratis.chronicle.observation.EventBatch;
import io.cratis.chronicle.observation.Observes;
import io.cratis.chronicle.observation.Reactor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@EventType
record BatchUserRegistered(String name) {}

@EventType
record BatchUserRenamed(String newName) {}

@EventType
record BatchUserEmailChanged(String email) {}

record BatchUserDocument(String id, String name) {}

record BatchUpsertFailure(String eventSourceId, Exception error) {}

record BatchUpsertThrottled(String eventSourceId, Duration retryAfter) {}

record BatchUpsertResponse(List<BatchUpsertFailure> failures, List<BatchUpsertThrottled> throttled) {}

interface BatchSearchIndex {
    BatchUpsertResponse bulkUpsert(List<BatchUserDocument> documents);
}

interface BatchEmailConfirmations {
    void send(String accountId, String email);
}

@Reactor
class BatchUsers {
    private final BatchSearchIndex index;
    private final BatchEmailConfirmations confirmations;

    BatchUsers(BatchSearchIndex index, BatchEmailConfirmations confirmations) {
        this.index = index;
        this.confirmations = confirmations;
    }

    @Observes(BatchUserRegistered.class)
    @Observes(BatchUserRenamed.class)
    public void index(EventBatch batch, BatchContext context) {
        List<BatchUserDocument> documents = new ArrayList<>();
        for (EventAndContext e : batch) {
            String id = e.getContext().getEventSourceId();
            if (e.getEvent() instanceof BatchUserRegistered registered) {
                documents.add(new BatchUserDocument(id, registered.name()));
            } else if (e.getEvent() instanceof BatchUserRenamed renamed) {
                documents.add(new BatchUserDocument(id, renamed.newName()));
            }
        }

        BatchUpsertResponse response = index.bulkUpsert(documents);
        response.failures().forEach(failure -> batch.fail(failure.eventSourceId(), failure.error()));
        response.throttled().forEach(throttled -> batch.defer(throttled.eventSourceId(), throttled.retryAfter()));
    }

    public void emailChanged(BatchUserEmailChanged event, EventContext context) {
        confirmations.send(context.getEventSourceId(), event.email());
    }
}
```
