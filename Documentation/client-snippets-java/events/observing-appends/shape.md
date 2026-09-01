```java
import io.cratis.chronicle.eventSequences.AppendedEventWithResult;
import io.cratis.chronicle.eventSequences.IEventLog;

import java.util.List;
import java.util.function.Consumer;

import kotlinx.coroutines.Job;

// The shape of the Java bridge for observing append operations: EventLogJavaBridge.watchAppendOperations
// subscribes a callback that receives a list of AppendedEventWithResult after every append or
// appendMany call made through the event log, returning the Job backing the subscription.
interface EventSequenceAppendOperationsShape {
    Job watchAppendOperations(IEventLog eventLog, Consumer<List<AppendedEventWithResult>> callback);
}
```
