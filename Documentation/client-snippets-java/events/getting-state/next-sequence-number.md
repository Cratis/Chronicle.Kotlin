```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.EventLogJavaBridge;

class EventsGettingStateNextSequenceNumber {
    long previewNextSequenceNumber(EventStore store) {
        long next = EventLogJavaBridge.getNextSequenceNumber(store.getEventLog());
        System.out.println("The next appended event will be assigned sequence number " + next);
        return next;
    }
}
```
