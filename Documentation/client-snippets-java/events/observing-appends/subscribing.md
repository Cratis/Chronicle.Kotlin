```java
import io.cratis.chronicle.eventSequences.IEventLog;

import io.cratis.chronicle.java.EventLogJavaBridge;
import kotlinx.coroutines.Job;

class EventsObservingAppendsSubscribing {
    // Subscribes to append operations for the lifetime of the returned Job; cancel it to stop.
    Job subscribe(IEventLog eventLog) {
        return EventLogJavaBridge.watchAppendOperations(eventLog, operations ->
            operations.forEach(operation ->
                System.out.println("Event " + operation.getEvent().getClass().getSimpleName() +
                    " appended: success=" + operation.getResult().isSuccess())));
    }
}
```
