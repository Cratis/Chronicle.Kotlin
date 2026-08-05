```java
import io.cratis.chronicle.EventStore;

import io.cratis.chronicle.java.EventLogJavaBridge;

class EventsGettingStateTail {
    long getAccountTailSequenceNumber(EventStore store, String accountId) {
        long tail = EventLogJavaBridge.getTailSequenceNumber(store.getEventLog(), accountId);
        System.out.println("Tail sequence number for " + accountId + ": " + tail);
        return tail;
    }
}
```
