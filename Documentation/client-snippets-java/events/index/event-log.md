```java
import io.cratis.chronicle.EventStore;
import io.cratis.chronicle.events.EventType;
import io.cratis.chronicle.eventSequences.AppendResult;

import io.cratis.chronicle.java.EventLogJavaBridge;

@EventType
record EventsIndexLogEmployeeRegistered(String firstName, String lastName) {}

class EventsIndexEventLog {
    // The event log is the default event sequence, exposed through EventStore.getEventLog().
    AppendResult registerEmployee(EventStore store, String employeeId, String firstName, String lastName) {
        return EventLogJavaBridge.append(store.getEventLog(), employeeId, new EventsIndexLogEmployeeRegistered(firstName, lastName), null);
    }
}
```
